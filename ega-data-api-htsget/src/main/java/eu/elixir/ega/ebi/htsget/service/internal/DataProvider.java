package eu.elixir.ega.ebi.htsget.service.internal;

import eu.elixir.ega.ebi.htsget.rest.HtsgetResponse;
import htsjdk.samtools.seekablestream.SeekableStream;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public interface DataProvider {
    boolean supportsFileType(String filename);

    void readHeader(SeekableStream stream) throws IOException;

    URI getHeaderAsDataUri() throws IOException, URISyntaxException;

    void addContentUris(String sequenceIndex, Long start, Long end, URI baseURI, HtsgetResponse urls, SeekableStream dataStream, SeekableStream indexStream) throws IOException, URISyntaxException;

    URI getFooterAsDataUri() throws IOException, URISyntaxException;
}
