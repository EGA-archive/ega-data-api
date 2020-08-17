package eu.elixir.ega.ebi.htsget.service.internal;

import eu.elixir.ega.ebi.htsget.rest.HtsgetResponse;
import eu.elixir.ega.ebi.htsget.rest.HtsgetUrl;
import htsjdk.samtools.BAMFileSpan;
import htsjdk.samtools.BAMStreamWriter;
import htsjdk.samtools.Chunk;
import htsjdk.samtools.DiskBasedBAMFileIndex;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.seekablestream.SeekableStream;
import htsjdk.samtools.util.BlockCompressedFilePointerUtil;
import htsjdk.samtools.util.BlockCompressedInputStream;
import htsjdk.samtools.util.BlockCompressedOutputStream;
import htsjdk.samtools.util.BlockCompressedStreamConstants;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.springframework.http.HttpRange;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

public class BAMDataProvider implements DataProvider {

    private SAMFileHeader header;

    @Override
    public boolean supportsFileType(String filename) {
        return filename.toLowerCase().endsWith(".bam") || filename.toLowerCase().endsWith(".bam.cip");
    }

    @Override
    public void readHeader(SeekableStream stream) throws IOException {

        // SamReader will try and close the stream so shield it
        try (CloseShieldInputStream shield = new CloseShieldInputStream(stream);
             SamReader reader = SamReaderFactory.makeDefault().open(SamInputResource.of(shield))) {
                header = reader.getFileHeader();
        }
    }

    @Override
    public SAMFileHeader getHeader() {
        return header;
    }

    @Override
    public URI getHeaderAsDataUri() throws IOException, URISyntaxException {
        try (ByteArrayOutputStream headerOutputStream = new ByteArrayOutputStream()) {

            BAMStreamWriter writer = new BAMStreamWriter(headerOutputStream, null, null, 0, header);
            writer.writeHeader(header);
            writer.finish(false);

            return makeDataUriFromBytes(headerOutputStream.toByteArray());
        }
    }

    @Override
    public void addContentUris(int sequenceIndex, Long start, Long end, URI baseURI, HtsgetResponse urls, SeekableStream dataStream, SeekableStream indexStream) throws IOException, URISyntaxException {
        BAMFileSpan span;

        try (DiskBasedBAMFileIndex index = new DiskBasedBAMFileIndex(indexStream, this.header.getSequenceDictionary())) {

            // Get all chunks that contain any alignments that overlap the requested range
            span = index.getSpanOverlapping(sequenceIndex,
                    start.intValue(),
                    end.intValue());
        }

        // Build the URLs for each chunk
        for (Chunk chunk : span.getChunks()) {

            // For all the blocks we return, the first and last one we have to be careful, because
            // there might be records that are split across the edge of the block.
            long startBlockPosition = BlockCompressedFilePointerUtil.getBlockAddress(chunk.getChunkStart());
            long endBlockPosition = BlockCompressedFilePointerUtil.getBlockAddress(chunk.getChunkEnd());
            int startBlockLength = getBlockSize(dataStream, startBlockPosition);

            Optional<Integer> startBlockStartTrim = Optional.of(BlockCompressedFilePointerUtil.getBlockOffset(chunk.getChunkStart()));
            Optional<Integer> startBlockEndTrim = Optional.empty();
            if (endBlockPosition == startBlockPosition) {
                startBlockEndTrim = Optional.of(BlockCompressedFilePointerUtil.getBlockOffset(chunk.getChunkEnd()));
            }

            urls.addUrl(new HtsgetUrl(getTrimmedBlockAsDataUri(startBlockPosition,
                    startBlockLength,
                    startBlockStartTrim,
                    startBlockEndTrim,
                    dataStream)));

            if (startBlockPosition == endBlockPosition)
                continue;

            // blocks in the middle will never have a problem with records that are at the edge of the block
            // because the other part of the record is in a block that is included too. so in the middle it
            // is OK to return one big range of bytes.
            long midBlockPosition = startBlockPosition + startBlockLength;
            if (midBlockPosition < endBlockPosition) {
                HtsgetUrl chunkURL = new HtsgetUrl(baseURI, "body");
                HttpRange range = HttpRange.createByteRange(midBlockPosition, endBlockPosition - 1);
                chunkURL.setHeader("Range", "bytes=" + range.toString());
                urls.addUrl(chunkURL);
            }

            // Make the end block
            int endBlockLength = getBlockSize(dataStream, endBlockPosition);
            Optional<Integer> endBlockEndTrim = Optional.of(BlockCompressedFilePointerUtil.getBlockOffset(chunk.getChunkEnd()) + 1);

            urls.addUrl(new HtsgetUrl(getTrimmedBlockAsDataUri(endBlockPosition,
                    endBlockLength,
                    Optional.empty(),
                    endBlockEndTrim,
                    dataStream)));
        }
    }

    private URI getTrimmedBlockAsDataUri(long startBlockPosition, int blockSize, Optional<Integer> startTrim, Optional<Integer> endTrim, SeekableStream dataStream) throws IOException, URISyntaxException {

        // Read the compressed block
        byte[] originalBlock = new byte[blockSize];
        dataStream.seek(startBlockPosition);
        dataStream.read(originalBlock);

        // Decompress it
        byte[] uncompressedData;
        try (ByteArrayInputStream input = new ByteArrayInputStream(originalBlock);
            BlockCompressedInputStream inputStream = new BlockCompressedInputStream(input)) {
                uncompressedData = IOUtils.toByteArray(inputStream);
        }

        // Trim it
        if (endTrim.isPresent())
            uncompressedData = Arrays.copyOf(uncompressedData, endTrim.get());
        if (startTrim.isPresent())
            uncompressedData = Arrays.copyOfRange(uncompressedData, startTrim.get(), uncompressedData.length);

        // Compress it again
        try(ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            try (BlockCompressedOutputStream blockStream = new BlockCompressedOutputStream(output, (File)null)) {
                blockStream.write(uncompressedData);
            }

            // Make it into a data uri
            return makeDataUriFromBytes(output.toByteArray());
        }
    }

    @Override
    public URI getFooterAsDataUri() throws IOException, URISyntaxException {
        return makeDataUriFromBytes(BlockCompressedStreamConstants.EMPTY_GZIP_BLOCK);
    }

    private URI makeDataUriFromBytes(byte[] bytes) throws URISyntaxException {
        String base64Data = Base64.getEncoder().encodeToString(bytes);
        return new URI(String.format("data:base64,%s", base64Data));
    }

    private static int getBlockSize(SeekableStream dataStream, long blockPosition) throws IOException {
        dataStream.seek(blockPosition + BlockCompressedStreamConstants.BLOCK_LENGTH_OFFSET);

        byte[] sizeBytes = new byte[2];
        dataStream.read(sizeBytes);

        int highByte = ((int) sizeBytes[1]) & 0xff;
        int lowByte = ((int) sizeBytes[0]) & 0xff;
        return (highByte << 8 | lowByte) + 1;
    }

}
