package eu.elixir.ega.ebi.htsget.formats;

import eu.elixir.ega.ebi.htsget.dto.HtsgetResponseV2;
import eu.elixir.ega.ebi.htsget.dto.HtsgetUrlV2;
import htsjdk.samtools.cram.io.InputStreamUtils;
import htsjdk.samtools.seekablestream.SeekableFileStream;
import htsjdk.samtools.seekablestream.SeekableStream;
import htsjdk.samtools.util.Interval;
import org.apache.http.HttpHeaders;
import org.springframework.http.HttpRange;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.List;

public abstract class DataProviderTest {
    private static byte[] dataUriToByteArray(URI uri) {
        if (!uri.getScheme().equalsIgnoreCase("data"))
            throw new IllegalArgumentException();

        return Base64.getDecoder().decode(uri.getSchemeSpecificPart().substring("base64,".length()));
    }

    protected byte[] makeDataFileFromResponse(HtsgetResponseV2 response, String dataFilePath) throws IOException {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream();
             SeekableStream dataStream = new SeekableFileStream(new File(dataFilePath))) {

            for (HtsgetUrlV2 url : response.getUrls()) {
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

    protected HtsgetResponseV2 getHtsgetResponseV2(Class<? extends AbstractDataProvider> dataProviderClass, String format, String dataFilePath, String indexFilePath, Interval interval) throws IOException, URISyntaxException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        HtsgetResponseV2 response = new HtsgetResponseV2(format);

        try (SeekableStream dataStream = new SeekableFileStream(new File(dataFilePath));
             SeekableStream indexStream = new SeekableFileStream(new File(indexFilePath))) {

            Constructor constructor = dataProviderClass.getConstructor(SeekableStream.class);
            DataProvider dataProvider = (DataProvider) constructor.newInstance(dataStream);

            response.addUrl(new HtsgetUrlV2(dataProvider.getHeaderAsDataUri(), "header"));

            response.getUrls().addAll(dataProvider.addContentUris(interval.getContig(),
                    (long)interval.getStart(), (long)interval.getEnd(),
                    new URI("file://" + dataFilePath),
                    dataStream,
                    indexStream));

            response.addUrl(new HtsgetUrlV2(dataProvider.getFooterAsDataUri()));
        }
        return response;
    }
}
