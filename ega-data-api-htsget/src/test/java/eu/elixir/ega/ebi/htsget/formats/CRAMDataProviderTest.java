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
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

public class CRAMDataProviderTest extends DataProviderTest {

    public static final String CHROMOSOME_NAME = "chr1";
    public static final Long START_POSITION = 10L;
    public static final Long END_POSITION = 20000L;

    public int countRecordsInOriginalFile(Interval interval) throws IOException {

        try (SamReader reader = SamReaderFactory.makeDefault()
                .referenceSequence(new File(LocalTestData.REFERENCE_FASTA_DOWNLOADED))
                .open(new File(LocalTestData.CRAM_FILE_PATH))) {
            Assert.assertTrue(reader.hasIndex());

            // Apparently this includes unmapped reads?
            SAMRecordIterator iterator = reader.query(interval.getContig(), interval.getStart(), interval.getEnd(), false);
            int recordCount = 0;
            while (iterator.hasNext()) {
                SAMRecord record = iterator.next();

                // the iterator is including some unmapped reads so ignore them
                if (record.getReadUnmappedFlag())
                    continue;

                ++recordCount;

                Assert.assertTrue(record.overlaps(interval));
            }
            return recordCount;
        }
    }

    @Test
    public void sliceContainsExpectedNumberOfRecords() throws IOException, URISyntaxException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Interval interval = new Interval(CHROMOSOME_NAME, START_POSITION.intValue(), END_POSITION.intValue());

        HtsgetResponseV2 response = getHtsgetResponseV2(
                CRAMDataProvider.class,
                "CRAM",
                LocalTestData.CRAM_FILE_PATH,
                LocalTestData.CRAM_INDEX_FILE_PATH,
                interval);

        Assert.assertEquals(countRecordsInOriginalFile(interval),
                countMatchingRecordsInResponse(response, interval));
    }

    private int countMatchingRecordsInResponse(HtsgetResponseV2 response, Interval interval) throws IOException {
        try (ByteArrayInputStream stream = new ByteArrayInputStream(makeDataFileFromResponse(response, LocalTestData.CRAM_FILE_PATH))) {
            SamInputResource inputResource = SamInputResource.of(stream);
            try (SamReader reader = SamReaderFactory.makeDefault().referenceSequence(new File(LocalTestData.REFERENCE_FASTA_DOWNLOADED)).open(inputResource)) {
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
