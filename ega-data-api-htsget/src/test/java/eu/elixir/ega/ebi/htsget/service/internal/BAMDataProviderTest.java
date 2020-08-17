package eu.elixir.ega.ebi.htsget.service.internal;

import eu.elixir.ega.ebi.htsget.config.LocalTestData;
import eu.elixir.ega.ebi.htsget.rest.HtsgetResponse;
import eu.elixir.ega.ebi.htsget.rest.HtsgetUrl;
import htsjdk.samtools.*;
import htsjdk.samtools.seekablestream.SeekableFileStream;
import htsjdk.samtools.seekablestream.SeekableStream;
import htsjdk.samtools.util.BlockCompressedStreamConstants;
import htsjdk.samtools.util.Interval;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;

public class BAMDataProviderTest extends DataProviderTest {

    public static final String CHROMOSOME_NAME = "chrM";
    public static final Long START_POSITION = 16545L;
    public static final Long END_POSITION = 16545L;

    private int countRecordsInOriginalFile(Interval interval) throws IOException {
        int recordCount = 0;
        try (SamReader reader = SamReaderFactory.makeDefault().open(new File(LocalTestData.BAM_FILE_PATH))) {
            Assert.assertTrue(reader.hasIndex());

            int referenceIndex = reader.getFileHeader().getSequenceIndex(interval.getContig());
            QueryInterval query = new QueryInterval(referenceIndex, interval.getStart(), interval.getEnd());
            SAMRecordIterator iterator = reader.queryOverlapping(new QueryInterval[]{query});

            while (iterator.hasNext()) {
                ++recordCount;
                iterator.next();
            }
        }
        return recordCount;
    }

    @Test
    public void sliceContainsExpectedNumberOfRecords() throws IOException, URISyntaxException {

        BAMDataProvider bamDataProvider = new BAMDataProvider();

        // Use the data file and index to make a response with URIs for all the pieces
        HtsgetResponse response = new HtsgetResponse("BAM");
        try (SeekableStream dataStream = new SeekableFileStream(new File(LocalTestData.BAM_FILE_PATH));
             SeekableStream indexStream = new SeekableFileStream(new File(LocalTestData.BAM_INDEX_FILE_PATH))) {

            bamDataProvider.readHeader(dataStream);
            response.addUrl(new HtsgetUrl(bamDataProvider.getHeaderAsDataUri(), "header"));

            bamDataProvider.addContentUris(CHROMOSOME_NAME,
                    START_POSITION, END_POSITION,
                    new URI("file://" + LocalTestData.BAM_FILE_PATH),
                    response,
                    dataStream,
                    indexStream);


            response.addUrl(new HtsgetUrl(bamDataProvider.getFooterAsDataUri()));
        }

        int expectedRecordCount = countRecordsInOriginalFile(new Interval(CHROMOSOME_NAME, START_POSITION.intValue(), END_POSITION.intValue()));

        Assert.assertEquals(expectedRecordCount, countMatchingRecordsInResponse(response,
                new Interval(CHROMOSOME_NAME, START_POSITION.intValue(), END_POSITION.intValue())));
    }

    @Test
    public void footerIsEmptyGzipBlock() throws URISyntaxException {
        BAMDataProvider bamDataProvider = new BAMDataProvider();
        String base64Data = bamDataProvider.getFooterAsDataUri().getSchemeSpecificPart().substring("base64,".length());
        Assert.assertArrayEquals(BlockCompressedStreamConstants.EMPTY_GZIP_BLOCK, Base64.getDecoder().decode(base64Data));
    }

    private int countMatchingRecordsInResponse(HtsgetResponse response, Interval interval) throws IOException {
        try (ByteArrayInputStream stream = new ByteArrayInputStream(makeDataFileFromResponse(response, LocalTestData.BAM_FILE_PATH))) {
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

}
