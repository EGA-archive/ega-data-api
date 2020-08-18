package eu.elixir.ega.ebi.htsget.formats;

import eu.elixir.ega.ebi.htsget.config.LocalTestData;
import eu.elixir.ega.ebi.htsget.dto.HtsgetResponseV2;
import eu.elixir.ega.ebi.htsget.dto.HtsgetUrlV2;
import htsjdk.samtools.*;
import htsjdk.samtools.cram.io.InputStreamUtils;
import htsjdk.samtools.seekablestream.SeekableFileStream;
import htsjdk.samtools.seekablestream.SeekableStream;
import htsjdk.samtools.util.Interval;
import org.apache.http.HttpHeaders;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpRange;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.List;

public class CRAMDataProviderTest {

    public static final String CHROMOSOME_NAME = "chrM";
    public static final Long START_POSITION = 16545L;
    public static final Long END_POSITION = 16545L;
    public static final int EXPECTED_RECORDS_IN_QUERY = 655;

    private static byte[] dataUriToByteArray(URI uri) {
        if (!uri.getScheme().equalsIgnoreCase("data"))
            throw new IllegalArgumentException();

        return Base64.getDecoder().decode(uri.getSchemeSpecificPart().substring("base64,".length()));
    }

    @Test
    public void canGetBytePositionsWithHtsJdkNormally() throws IOException {
        try (SamReader reader = SamReaderFactory.makeDefault().open(new File(LocalTestData.BAM_FILE_PATH))) {
            Assert.assertTrue(reader.hasIndex());

            SAMRecordIterator iterator = reader.query(CHROMOSOME_NAME, START_POSITION.intValue(), END_POSITION.intValue(), false);
            int recordCount = 0;
            while (iterator.hasNext()) {
                ++recordCount;
                SAMRecord record = iterator.next();
                Assert.assertTrue(record.overlaps(new Interval(CHROMOSOME_NAME, START_POSITION.intValue(), END_POSITION.intValue())));
            }
            Assert.assertEquals(EXPECTED_RECORDS_IN_QUERY, recordCount);
        }
    }

    @Test
    public void sliceContainsExpectedNumberOfRecords() throws IOException, URISyntaxException {
        BAMDataProvider bamDataProvider = new BAMDataProvider();

        // Use the data file and index to make a response with URIs for all the pieces
        HtsgetResponseV2 response = new HtsgetResponseV2("BAM");
        try (SeekableStream dataStream = new SeekableFileStream(new File(LocalTestData.BAM_FILE_PATH));
             SeekableStream indexStream = new SeekableFileStream(new File(LocalTestData.BAM_INDEX_FILE_PATH))) {

            bamDataProvider.readHeader(dataStream);
            response.addUrl(new HtsgetUrlV2(bamDataProvider.getHeaderAsDataUri(), "header"));

            bamDataProvider.addContentUris(CHROMOSOME_NAME,
                    START_POSITION, END_POSITION,
                    new URI("file://" + LocalTestData.BAM_FILE_PATH),
                    response,
                    dataStream,
                    indexStream);


            response.addUrl(new HtsgetUrlV2(bamDataProvider.getFooterAsDataUri()));
        }

        Assert.assertEquals(EXPECTED_RECORDS_IN_QUERY, countMatchingRecordsInResponse(response,
                new Interval(CHROMOSOME_NAME, START_POSITION.intValue(), END_POSITION.intValue())));
    }

    private int countMatchingRecordsInResponse(HtsgetResponseV2 response, Interval interval) throws IOException {
        try (ByteArrayInputStream stream = new ByteArrayInputStream(makeBAMFileFromResponse(response))) {
            SamInputResource inputResource = SamInputResource.of(stream);
            try (SamReader reader = SamReaderFactory.makeDefault().open(inputResource)) {
                SAMRecordIterator iterator = reader.iterator();

                int recordCount = 0;

                while (iterator.hasNext()) {
                    SAMRecord record = iterator.next();
                    if (record.overlaps(interval))
                        ++recordCount;
                }

                return recordCount;
            }
        }
    }

    private byte[] makeBAMFileFromResponse(HtsgetResponseV2 response) throws IOException {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream();
             SeekableStream dataStream = new SeekableFileStream(new File(LocalTestData.BAM_FILE_PATH))) {

            for (HtsgetUrlV2 url : response.getUrls()) {
                if (url.getUrl().getScheme().equalsIgnoreCase("data")) {
                    stream.write(dataUriToByteArray(url.getUrl()));
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
