package eu.elixir.ega.ebi.htsget.formats;

import eu.elixir.ega.ebi.htsget.config.LocalTestData;
import eu.elixir.ega.ebi.htsget.dto.HtsgetResponseV2;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.util.Interval;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

public class CRAMDataProviderTest extends DataProviderTest {

    public static final String CHROMOSOME_NAME = "chrM";
    public static final Long START_POSITION = 16545L;
    public static final Long END_POSITION = 16545L;
    public static final int EXPECTED_RECORDS_IN_QUERY = 655;

    @Test
    public void canGetBytePositionsWithHtsJdkNormally() throws IOException {
        try (SamReader reader = SamReaderFactory.makeDefault().open(new File(LocalTestData.CRAM_FILE_PATH))) {
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
        Interval interval = new Interval(CHROMOSOME_NAME, START_POSITION.intValue(), END_POSITION.intValue());

        HtsgetResponseV2 response = getHtsgetResponseV2(
                new BAMDataProvider(),
                "CRAM",
                LocalTestData.CRAM_FILE_PATH,
                LocalTestData.CRAM_INDEX_FILE_PATH,
                interval);

        Assert.assertEquals(EXPECTED_RECORDS_IN_QUERY, countMatchingRecordsInResponse(response,
                new Interval(CHROMOSOME_NAME, START_POSITION.intValue(), END_POSITION.intValue())));
    }

    private int countMatchingRecordsInResponse(HtsgetResponseV2 response, Interval interval) throws IOException {
        try (ByteArrayInputStream stream = new ByteArrayInputStream(makeDataFileFromResponse(response, LocalTestData.CRAM_FILE_PATH))) {
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
