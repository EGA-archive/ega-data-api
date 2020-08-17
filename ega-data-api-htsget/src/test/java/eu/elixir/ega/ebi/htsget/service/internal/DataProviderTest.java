package eu.elixir.ega.ebi.htsget.service.internal;

import eu.elixir.ega.ebi.htsget.rest.HtsgetResponse;
import eu.elixir.ega.ebi.htsget.rest.HtsgetUrl;
import htsjdk.samtools.cram.io.InputStreamUtils;
import htsjdk.samtools.seekablestream.SeekableFileStream;
import htsjdk.samtools.seekablestream.SeekableStream;
import org.apache.http.HttpHeaders;
import org.springframework.http.HttpRange;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Base64;
import java.util.List;

public class DataProviderTest {
    private static byte[] dataUriToByteArray(URI uri) {
        if (!uri.getScheme().equalsIgnoreCase("data"))
            throw new IllegalArgumentException();

        return Base64.getDecoder().decode(uri.getSchemeSpecificPart().substring("base64,".length()));
    }

    protected byte[] makeDataFileFromResponse(HtsgetResponse response, String dataFilePath) throws IOException {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream();
             SeekableStream dataStream = new SeekableFileStream(new File(dataFilePath))) {

            for (HtsgetUrl url : response.getUrls()) {
                if (url.getUrl().getScheme().equalsIgnoreCase("data")) {
                    stream.write(DataProviderTest.dataUriToByteArray(url.getUrl()));
                } else {
                    copyByteRanges(stream, dataStream, HttpRange.parseRanges(url.getHeaders().get(HttpHeaders.RANGE)));
                }
            }

            return stream.toByteArray();
        }
    }

    private void copyByteRanges(ByteArrayOutputStream outputStream, SeekableStream dataStream, List<HttpRange> httpRanges) throws IOException {
        long length = dataStream.length();
        for (HttpRange range : httpRanges) {
            dataStream.seek(range.getRangeStart(length));
            byte[] content = InputStreamUtils.readFully(dataStream, ((Long) (range.getRangeEnd(length) - range.getRangeStart(length) + 1)).intValue());
            outputStream.write(content);
        }
    }
}
