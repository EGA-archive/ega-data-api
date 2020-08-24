package eu.elixir.ega.ebi.htsget.formats;

import eu.elixir.ega.ebi.commons.exception.NotFoundException;
import eu.elixir.ega.ebi.htsget.dto.HtsgetResponseV2;
import eu.elixir.ega.ebi.htsget.dto.HtsgetUrlV2;
import htsjdk.samtools.*;
import htsjdk.samtools.seekablestream.SeekableStream;
import htsjdk.samtools.util.BlockCompressedStreamConstants;
import org.apache.commons.io.input.CloseShieldInputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class BAMDataProvider extends AbstractDataProvider implements DataProvider {

    private SAMFileHeader header;

    public BAMDataProvider(SeekableStream dataStream) throws IOException {
        super(dataStream);
    }

    @Override
    public boolean supportsFileType(String filename) {
        return filename.toLowerCase().endsWith(".bam") || filename.toLowerCase().endsWith(".bam.cip");
    }

    @Override
    protected void readHeader(SeekableStream stream) throws IOException {

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
    public List<HtsgetUrlV2> addContentUris(String referenceName, Long start, Long end, URI baseURI, SeekableStream dataStream, SeekableStream indexStream) throws IOException, URISyntaxException {
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
        List<HtsgetUrlV2> results = new ArrayList<>();

        for (Chunk chunk : span.getChunks()) {
            results.addAll(makeUrlsForBGZFBlocks(baseURI, dataStream, chunk.getChunkStart(), chunk.getChunkEnd()));
        }
        return results;
    }

    @Override
    public URI getFooterAsDataUri() throws URISyntaxException {
        return makeDataUriFromBytes(BlockCompressedStreamConstants.EMPTY_GZIP_BLOCK);
    }

}
