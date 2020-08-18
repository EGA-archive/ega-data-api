package eu.elixir.ega.ebi.htsget.formats;

import eu.elixir.ega.ebi.htsget.dto.HtsgetResponseV2;
import eu.elixir.ega.ebi.htsget.dto.HtsgetUrlV2;
import htsjdk.samtools.seekablestream.SeekableStream;
import htsjdk.samtools.util.BlockCompressedFilePointerUtil;
import htsjdk.samtools.util.BlockCompressedInputStream;
import htsjdk.samtools.util.BlockCompressedOutputStream;
import htsjdk.samtools.util.BlockCompressedStreamConstants;
import org.apache.commons.compress.utils.IOUtils;
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

public class AbstractDataProvider {

    private static int getBlockSize(SeekableStream dataStream, long blockPosition) throws IOException {
        dataStream.seek(blockPosition + BlockCompressedStreamConstants.BLOCK_LENGTH_OFFSET);

        byte[] sizeBytes = new byte[2];
        if (dataStream.read(sizeBytes) != sizeBytes.length)
            throw new IOException("could not read block size");

        int highByte = ((int) sizeBytes[1]) & 0xff;
        int lowByte = ((int) sizeBytes[0]) & 0xff;
        return (highByte << 8 | lowByte) + 1;
    }

    protected void makeUrlsForBGZFBlocks(URI baseURI, HtsgetResponseV2 urls, SeekableStream dataStream, long chunkStart, long chunkEnd) throws IOException, URISyntaxException {
        // For all the blocks we return, the first and last one we have to be careful, because
        // there might be records that are split across the edge of the block.
        long startBlockPosition = BlockCompressedFilePointerUtil.getBlockAddress(chunkStart);
        long endBlockPosition = BlockCompressedFilePointerUtil.getBlockAddress(chunkEnd);
        int startBlockLength = AbstractDataProvider.getBlockSize(dataStream, startBlockPosition);

        Optional<Integer> startBlockStartTrim = Optional.of(BlockCompressedFilePointerUtil.getBlockOffset(chunkStart));
        Optional<Integer> startBlockEndTrim = Optional.empty();
        if (endBlockPosition == startBlockPosition) {
            startBlockEndTrim = Optional.of(BlockCompressedFilePointerUtil.getBlockOffset(chunkEnd));
        }

        urls.addUrl(new HtsgetUrlV2(getTrimmedBlockAsDataUri(startBlockPosition,
                startBlockLength,
                startBlockStartTrim,
                startBlockEndTrim,
                dataStream)));

        if (startBlockPosition == endBlockPosition)
            return;

        // blocks in the middle will never have a problem with records that are at the edge of the block
        // because the other part of the record is in a block that is included too. so in the middle it
        // is OK to return one big range of bytes.
        long midBlockPosition = startBlockPosition + startBlockLength;
        if (midBlockPosition < endBlockPosition) {
            HtsgetUrlV2 chunkURL = new HtsgetUrlV2(baseURI, "body");
            HttpRange range = HttpRange.createByteRange(midBlockPosition, endBlockPosition - 1);
            chunkURL.setHeader("Range", "bytes=" + range.toString());
            urls.addUrl(chunkURL);
        }

        // Make the end block
        int endBlockLength = AbstractDataProvider.getBlockSize(dataStream, endBlockPosition);
        Optional<Integer> endBlockEndTrim = Optional.of(BlockCompressedFilePointerUtil.getBlockOffset(chunkEnd) + 1);

        urls.addUrl(new HtsgetUrlV2(getTrimmedBlockAsDataUri(endBlockPosition,
                endBlockLength,
                Optional.empty(),
                endBlockEndTrim,
                dataStream)));
    }

    private URI getTrimmedBlockAsDataUri(long startBlockPosition, int blockSize, Optional<Integer> startTrim, Optional<Integer> endTrim, SeekableStream dataStream) throws IOException, URISyntaxException {

        // Read the compressed block
        byte[] originalBlock = new byte[blockSize];
        dataStream.seek(startBlockPosition);
        if (dataStream.read(originalBlock) != blockSize)
            throw new IOException("Failed to read block");

        if ((((int) originalBlock[0]) & 0xff) != BlockCompressedStreamConstants.GZIP_ID1 || (((int) originalBlock[1]) & 0xff) != BlockCompressedStreamConstants.GZIP_ID2)
            throw new RuntimeException("Invalid block (expected header bytes " + BlockCompressedStreamConstants.GZIP_ID1 + "," + BlockCompressedStreamConstants.GZIP_ID2 + " but got " + originalBlock[0] + "," + originalBlock[1]);

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
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            try (BlockCompressedOutputStream blockStream = new BlockCompressedOutputStream(output, (File) null)) {
                blockStream.write(uncompressedData);
            }

            // Make it into a data uri
            return makeDataUriFromBytes(output.toByteArray());
        }
    }

    protected URI makeDataUriFromBytes(byte[] bytes) throws URISyntaxException {
        String base64Data = Base64.getEncoder().encodeToString(bytes);
        return new URI(String.format("data:base64,%s", base64Data));
    }
}
