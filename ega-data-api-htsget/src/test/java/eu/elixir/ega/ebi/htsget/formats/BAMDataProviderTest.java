package eu.elixir.ega.ebi.htsget.formats;

import eu.elixir.ega.ebi.htsget.config.LocalTestData;
import eu.elixir.ega.ebi.htsget.dto.HtsgetResponseV2;
import htsjdk.samtools.QueryInterval;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.seekablestream.SeekableFileStream;
import htsjdk.samtools.seekablestream.SeekableStream;
import htsjdk.samtools.util.BlockCompressedStreamConstants;
import htsjdk.samtools.util.Interval;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
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
    public void sliceContainsExpectedNumberOfRecords() throws IOException, URISyntaxException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {

        Interval interval = new Interval(CHROMOSOME_NAME, START_POSITION.intValue(), END_POSITION.intValue());

        int expectedRecordCount = countRecordsInOriginalFile(interval);

        HtsgetResponseV2 response = getHtsgetResponseV2(BAMDataProvider.class,
                "BAM",
                LocalTestData.BAM_FILE_PATH,
                LocalTestData.BAM_INDEX_FILE_PATH,
                interval);

        Assert.assertEquals(expectedRecordCount, countMatchingRecordsInResponse(response, interval));
    }

    @Test
    public void sliceWithNoStartOrEndContainsExpectedNumberOfRecords() throws IOException, URISyntaxException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {

        Interval interval = new Interval(CHROMOSOME_NAME, 0, Integer.MAX_VALUE);

        int expectedRecordCount = countRecordsInOriginalFile(interval);

        HtsgetResponseV2 response = getHtsgetResponseV2(BAMDataProvider.class,
                "BAM",
                LocalTestData.BAM_FILE_PATH,
                LocalTestData.BAM_INDEX_FILE_PATH,
                interval);

        Assert.assertEquals(expectedRecordCount, countMatchingRecordsInResponse(response, interval));
    }

    @Test
    public void footerIsEmptyGzipBlock() throws URISyntaxException, IOException {
        try(SeekableStream dataStream = new SeekableFileStream(new File(LocalTestData.BAM_FILE_PATH))) {
            BAMDataProvider bamDataProvider = new BAMDataProvider(dataStream);
            String base64Data = bamDataProvider.getFooterAsDataUri().getSchemeSpecificPart().substring("base64,".length());
            Assert.assertArrayEquals(BlockCompressedStreamConstants.EMPTY_GZIP_BLOCK, Base64.getDecoder().decode(base64Data));
        }
    }

    private int countMatchingRecordsInResponse(HtsgetResponseV2 response, Interval interval) throws IOException {
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
