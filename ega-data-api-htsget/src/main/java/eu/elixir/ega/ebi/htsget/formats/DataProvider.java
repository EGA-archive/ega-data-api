package eu.elixir.ega.ebi.htsget.formats;

import eu.elixir.ega.ebi.htsget.dto.HtsgetResponseV2;
import eu.elixir.ega.ebi.htsget.dto.HtsgetUrlV2;
import htsjdk.samtools.seekablestream.SeekableStream;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public interface DataProvider {
    boolean supportsFileType(String filename);

    URI getHeaderAsDataUri() throws IOException, URISyntaxException;

    List<HtsgetUrlV2> addContentUris(String sequenceIndex, Long start, Long end, URI baseURI, SeekableStream dataStream, SeekableStream indexStream) throws IOException, URISyntaxException;

    URI getFooterAsDataUri() throws IOException, URISyntaxException;
}
