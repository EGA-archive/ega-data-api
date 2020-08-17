package eu.elixir.ega.ebi.htsget.service.internal;

import eu.elixir.ega.ebi.htsget.config.LocalTestData;
import eu.elixir.ega.ebi.htsget.rest.HtsgetResponse;
import eu.elixir.ega.ebi.htsget.rest.HtsgetUrl;
import htsjdk.samtools.seekablestream.SeekableFileStream;
import htsjdk.samtools.seekablestream.SeekableStream;
import htsjdk.samtools.util.CloseableIterator;
import htsjdk.samtools.util.Interval;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;
import htsjdk.variant.vcf.VCFHeader;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class VCFDataProviderTest extends DataProviderTest {

    public static final String CHROMOSOME_NAME = "1";
    public static final Long START_POSITION = 1L;
    public static final Long END_POSITION = 1000000L;

    @Test
    public void canReadRecordsFromVCFFile()
    {
        int recordCount = 0;
        try(VCFFileReader reader = new VCFFileReader(new File(LocalTestData.VCF_FILE_PATH), new File(LocalTestData.VCF_INDEX_FILE_PATH))) {
            VCFHeader header = reader.getHeader();

            try (CloseableIterator<VariantContext> iterator = reader.query(CHROMOSOME_NAME, START_POSITION.intValue(), END_POSITION.intValue())) {
                while (iterator.hasNext()) {
                    VariantContext context = iterator.next();
                    ++recordCount;
                }
            }
        }
        Assert.assertEquals(13436, recordCount);
    }

    @Test
    public void vcfBlocksAreValid() throws IOException, URISyntaxException {
        VCFDataProvider provider = new VCFDataProvider();

        // Use the data file and index to make a response with URIs for all the pieces
        HtsgetResponse response = new HtsgetResponse("BAM");
        try (SeekableStream dataStream = new SeekableFileStream(new File(LocalTestData.VCF_FILE_PATH));
             SeekableStream indexStream = new SeekableFileStream(new File(LocalTestData.VCF_INDEX_FILE_PATH))) {

            provider.readHeader(dataStream);
            response.addUrl(new HtsgetUrl(provider.getHeaderAsDataUri(), "header"));

            provider.addContentUris(CHROMOSOME_NAME,
                    START_POSITION, END_POSITION,
                    new URI("file://" + LocalTestData.BAM_FILE_PATH),
                    response,
                    dataStream,
                    indexStream);


            response.addUrl(new HtsgetUrl(provider.getFooterAsDataUri()));
        }

        byte[] responseBytes = this.makeDataFileFromResponse(response, LocalTestData.VCF_FILE_PATH);

        File tempFile = new File("/tmp/my-test.vcf.gz");
        FileUtils.writeByteArrayToFile(tempFile, responseBytes);

        Interval range = new Interval(CHROMOSOME_NAME, START_POSITION.intValue(), END_POSITION.intValue());
        int recordCount = 0;
        try(VCFFileReader reader = new VCFFileReader(tempFile, false)) {
            try (CloseableIterator<VariantContext> iterator = reader.iterator())
            {
                while (iterator.hasNext()) {
                    VariantContext context = iterator.next();
                    if (context.overlaps(range))
                        ++recordCount;
                }
            }
        }
        Assert.assertEquals(13436, recordCount);

    }
}
