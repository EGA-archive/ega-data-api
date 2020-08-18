package eu.elixir.ega.ebi.htsget.formats;

import eu.elixir.ega.ebi.htsget.config.LocalTestData;
import eu.elixir.ega.ebi.htsget.dto.HtsgetResponseV2;
import eu.elixir.ega.ebi.htsget.dto.HtsgetUrlV2;
import htsjdk.samtools.seekablestream.SeekableFileStream;
import htsjdk.samtools.seekablestream.SeekableStream;
import htsjdk.samtools.util.CloseableIterator;
import htsjdk.samtools.util.Interval;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class BCFDataProviderTest extends DataProviderTest {

    public static final String CHROMOSOME_NAME = "1";
    public static final Long START_POSITION = 1L;
    public static final Long END_POSITION = 1000000L;

    protected int countRecordsInOriginalFile(Interval range) {
        int recordCount = 0;
        try (VCFFileReader reader = new VCFFileReader(new File(LocalTestData.BCF_FILE_PATH), new File(LocalTestData.BCF_INDEX_FILE_PATH))) {
            try (CloseableIterator<VariantContext> iterator = reader.query(range.getContig(), range.getStart(), range.getEnd())) {
                while (iterator.hasNext()) {
                    iterator.next();
                    ++recordCount;
                }
            }
        }
        return recordCount;
    }

    @Test
    public void bcfBlocksAreValid() throws IOException, URISyntaxException {
        BCFDataProvider provider = new BCFDataProvider();

        // Use the data file and index to make a response with URIs for all the pieces
        HtsgetResponseV2 response = new HtsgetResponseV2("BCF");
        try (SeekableStream dataStream = new SeekableFileStream(new File(LocalTestData.BCF_FILE_PATH));
             SeekableStream indexStream = new SeekableFileStream(new File(LocalTestData.BCF_INDEX_FILE_PATH))) {

            provider.readHeader(dataStream);
            response.addUrl(new HtsgetUrlV2(provider.getHeaderAsDataUri(), "header"));

            provider.addContentUris(CHROMOSOME_NAME,
                    START_POSITION, END_POSITION,
                    new URI("file://" + LocalTestData.BCF_FILE_PATH),
                    response,
                    dataStream,
                    indexStream);


            response.addUrl(new HtsgetUrlV2(provider.getFooterAsDataUri()));
        }

        byte[] responseBytes = this.makeDataFileFromResponse(response, LocalTestData.BCF_FILE_PATH);

        File tempFile = new File("/tmp/my-test.bcf");
        FileUtils.writeByteArrayToFile(tempFile, responseBytes);

        Interval range = new Interval(CHROMOSOME_NAME, START_POSITION.intValue(), END_POSITION.intValue());
        int recordCount = 0;
        try (VCFFileReader reader = new VCFFileReader(tempFile, false)) {
            try (CloseableIterator<VariantContext> iterator = reader.iterator()) {
                while (iterator.hasNext()) {
                    VariantContext context = iterator.next();
                    if (context.overlaps(range))
                        ++recordCount;
                }
            }
        }
        Assert.assertEquals(countRecordsInOriginalFile(range), recordCount);

    }
}
