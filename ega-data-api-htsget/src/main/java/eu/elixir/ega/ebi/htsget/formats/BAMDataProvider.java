package eu.elixir.ega.ebi.htsget.formats;

import eu.elixir.ega.ebi.commons.shared.config.NotFoundException;
import eu.elixir.ega.ebi.htsget.dto.HtsgetResponseV2;
import htsjdk.samtools.*;
import htsjdk.samtools.seekablestream.SeekableStream;
import htsjdk.samtools.util.BlockCompressedStreamConstants;
import org.apache.commons.io.input.CloseShieldInputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class BAMDataProvider extends AbstractDataProvider implements DataProvider {

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
    public URI getHeaderAsDataUri() throws IOException, URISyntaxException {
        try (ByteArrayOutputStream headerOutputStream = new ByteArrayOutputStream()) {

            BAMStreamWriter writer = new BAMStreamWriter(headerOutputStream, null, null, 0, header);
            writer.writeHeader(header);
            writer.finish(false);

            return makeDataUriFromBytes(headerOutputStream.toByteArray());
        }
    }

    @Override
    public void addContentUris(String referenceName, Long start, Long end, URI baseURI, HtsgetResponseV2 urls, SeekableStream dataStream, SeekableStream indexStream) throws IOException, URISyntaxException {
        BAMFileSpan span;

        // look up this sequence in the dictionary
        SAMSequenceDictionary sequenceDictionary = header.getSequenceDictionary();
        int sequenceIndex = sequenceDictionary.getSequenceIndex(referenceName);
        if (sequenceIndex == SAMSequenceRecord.UNAVAILABLE_SEQUENCE_INDEX) {
            throw new NotFoundException("sequence not found", referenceName);
        }

        try (DiskBasedBAMFileIndex index = new DiskBasedBAMFileIndex(indexStream, this.header.getSequenceDictionary())) {

            // Get all chunks that contain any alignments that overlap the requested range
            span = index.getSpanOverlapping(sequenceIndex,
                    start.intValue(),
                    end.intValue());
        }

        // Build the URLs for each chunk
        for (Chunk chunk : span.getChunks()) {
            makeUrlsForBGZFBlocks(baseURI, urls, dataStream, chunk.getChunkStart(), chunk.getChunkEnd());
        }
    }

    @Override
    public URI getFooterAsDataUri() throws URISyntaxException {
        return makeDataUriFromBytes(BlockCompressedStreamConstants.EMPTY_GZIP_BLOCK);
    }

}
