package eu.elixir.ega.ebi.htsget.service.internal;

import eu.elixir.ega.ebi.commons.config.InvalidInputException;
import eu.elixir.ega.ebi.commons.config.InvalidRangeException;
import eu.elixir.ega.ebi.commons.config.UnsupportedFormatException;
import eu.elixir.ega.ebi.commons.shared.config.NotFoundException;
import eu.elixir.ega.ebi.commons.shared.dto.File;
import eu.elixir.ega.ebi.commons.shared.dto.FileIndexFile;
import eu.elixir.ega.ebi.commons.shared.dto.MyExternalConfig;
import eu.elixir.ega.ebi.commons.shared.service.FileInfoService;
import eu.elixir.ega.ebi.htsget.config.LocalTestData;
import eu.elixir.ega.ebi.htsget.dto.HtsgetResponse;
import eu.elixir.ega.ebi.htsget.dto.HtsgetResponseV2;
import eu.elixir.ega.ebi.htsget.dto.HtsgetUrlV2;
import eu.elixir.ega.ebi.htsget.formats.DataProvider;
import eu.elixir.ega.ebi.htsget.formats.DataProviderFactory;
import eu.elixir.ega.ebi.htsget.service.TicketServiceV2;
import htsjdk.samtools.seekablestream.SeekableFileStream;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@Ignore
@RunWith(SpringRunner.class)
public class TicketServiceV2Test {

    @Autowired
    private TicketServiceV2 service;

    @MockBean
    private FileInfoService fileInfoService;

    @MockBean
    private MyExternalConfig externalConfig;

    @MockBean
    private ResClient resClient;

    @MockBean
    private DataProviderFactory dataProviderFactory;

    @MockBean
    private DataProvider dataProvider;

    @Before
    public void setUpCommonMocks() throws IOException {
        Mockito.reset(externalConfig);
        when(externalConfig.getEgaExternalUrl())
                .thenReturn("https://test.Htsget.ega.url/");

        Mockito.reset(fileInfoService);
        File dataFile = new File();
        dataFile.setFileId(LocalTestData.BAM_FILE_ID);
        dataFile.setFileName("NA12891.bam.cip");
        when(fileInfoService.getFileInfo(LocalTestData.BAM_FILE_ID))
                .thenReturn(dataFile);
        when(fileInfoService.getFileIndexFile(LocalTestData.BAM_FILE_ID))
                .thenReturn(new FileIndexFile(LocalTestData.BAM_FILE_ID, LocalTestData.BAM_INDEX_FILE_ID));

        Mockito.reset(resClient);
        when(resClient.getStreamForFile(LocalTestData.BAM_FILE_ID))
                .thenReturn(new SeekableFileStream(new java.io.File(LocalTestData.BAM_FILE_PATH)));
        when(resClient.getStreamForFile(LocalTestData.BAM_INDEX_FILE_ID))
                .thenReturn(new SeekableFileStream(new java.io.File(LocalTestData.BAM_INDEX_FILE_PATH)));

        Mockito.reset(dataProviderFactory);
        doThrow(UnsupportedFormatException.class).when(dataProviderFactory).getProviderForFormat(any(), any());
        doReturn(dataProvider).when(dataProviderFactory).getProviderForFormat(eq("BAM"), any());
        doReturn(dataProvider).when(dataProviderFactory).getProviderForFormat(eq("CRAM"), any());
    }

    @Test(expected = InvalidInputException.class)
    public void requestForHeaderWithOtherParametersReturnsInvalidInput() throws IOException, URISyntaxException {
        service.getRead(LocalTestData.BAM_FILE_ID,
                "BAM",
                Optional.of("header"),
                Optional.of("ChromosomeName"),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());

    }

    @Test(expected = NotFoundException.class)
    public void requestForNonExistingReferenceReturnsNotFound() throws IOException, URISyntaxException {
        doReturn(true).when(dataProvider).supportsFileType(any());
        doThrow(NotFoundException.class).when(dataProvider).addContentUris(anyString(), any(), any(), any(), any(), any());
        service.getRead(LocalTestData.BAM_FILE_ID,
                "BAM",
                Optional.empty(),
                Optional.of("DoesNotExist"),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    @Test(expected = InvalidInputException.class)
    public void requestWithStartButNoReferenceNameReturnsInvalidInput() throws IOException, URISyntaxException {
        service.getRead(LocalTestData.BAM_FILE_ID,
                "BAM",
                Optional.empty(),
                Optional.empty(),
                Optional.of(3L),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    @Test(expected = InvalidInputException.class)
    public void requestWithStartAndUnplacedReadsReturnsInvalidInput() throws IOException, URISyntaxException {
        service.getRead(LocalTestData.BAM_FILE_ID,
                "BAM",
                Optional.empty(),
                Optional.of("*"),
                Optional.of(3L),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());

    }

    @Test
    public void requestingEntireFileHasSingleURL() throws IOException, URISyntaxException {
        doReturn(true).when(dataProvider).supportsFileType(any());
        HtsgetResponseV2 response = service.getRead(LocalTestData.BAM_FILE_ID,
                "BAM",
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
        Assert.assertEquals("BAM", response.getFormat());
        Assert.assertEquals(1, response.getUrls().size());

        HtsgetUrlV2 url = response.getUrls().get(0);
        Assert.assertEquals("/" + LocalTestData.BAM_FILE_ID, url.getUrl().getPath());
    }

    @Test
    public void requestingOnlyHeaderHasSingleURLForHeader() throws IOException, URISyntaxException {
        doReturn(true).when(dataProvider).supportsFileType(any());
        doReturn(new URI("data:123")).when(dataProvider).getHeaderAsDataUri();
        HtsgetResponseV2 response = service.getRead(LocalTestData.BAM_FILE_ID,
                "BAM",
                Optional.of("header"),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
        Assert.assertEquals(1, response.getUrls().size());
        Assert.assertEquals("BAM", response.getFormat());

        HtsgetUrlV2 url = response.getUrls().get(0);
        Assert.assertEquals("data", url.getUrl().getScheme());
        Assert.assertEquals("header", url.getUrlClass());
    }

    @Test
    public void byteRangesAreLessThan1GB() throws IOException, URISyntaxException {
        long mockDataLength = 35L * 1024 * 1024 * 1024;
        doReturn(true).when(dataProvider).supportsFileType(any());
        doAnswer(i -> {
            HtsgetUrlV2 bigByteRange = new HtsgetUrlV2(new URI("https://test.data/"));
            bigByteRange.setHeader(HttpHeaders.RANGE, "bytes=" + HttpRange.createByteRange(0, mockDataLength).toString());
            List<HtsgetUrlV2> result = new ArrayList<>();
            result.add(bigByteRange);
            return result;
        }).when(dataProvider).addContentUris(anyString(), any(), any(), any(), any(), any());
        doReturn(new URI("data:123")).when(dataProvider).getHeaderAsDataUri();
        doReturn(new URI("data:321")).when(dataProvider).getFooterAsDataUri();

        HtsgetResponseV2 response = service.getRead(LocalTestData.BAM_FILE_ID,
                "BAM",
                Optional.empty(),
                Optional.of("chrM"),
                Optional.of(1L),
                Optional.of(16545L),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
        long totalLength = 0;
        for (HtsgetUrlV2 url : response.getUrls()) {
            if (url.getUrl().getScheme().equalsIgnoreCase("data"))
                continue;

            for (HttpRange range : HttpRange.parseRanges(url.getHeaders().get(HttpHeaders.RANGE))) {
                long rangeLength = range.getRangeEnd(mockDataLength) - range.getRangeStart(mockDataLength) + 1;
                Assert.assertTrue(rangeLength <= TicketServiceV2Impl.MAX_BYTES_PER_DATA_BLOCK);
                totalLength += rangeLength;
            }
        }
        Assert.assertEquals(mockDataLength, totalLength);
    }

    @Test
    public void whenStartAndEndAreNotThereUseZeroAndMaximum() throws IOException, URISyntaxException {
        doReturn(true).when(dataProvider).supportsFileType(any());
        HtsgetResponseV2 response = service.getRead(LocalTestData.BAM_FILE_ID,
                "BAM",
                Optional.empty(),
                Optional.of("chrM"),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
        Mockito.verify(dataProvider).addContentUris(eq("chrM"), eq(0L), eq(Long.MAX_VALUE), any(), any(), any());
    }

    @Test(expected = InvalidRangeException.class)
    public void whenEndIsLessThanStartThrowsInvalidRangeException() throws IOException, URISyntaxException {
        doReturn(true).when(dataProvider).supportsFileType(any());
        service.getRead(LocalTestData.BAM_FILE_ID,
                "BAM",
                Optional.empty(),
                Optional.of("chrM"),
                Optional.of(100L),
                Optional.of(50L),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    @TestConfiguration
    public static class Config {
        @Bean
        public TicketServiceV2 ticketService(FileInfoService fileInfoService, MyExternalConfig externalConfig, ResClient resClient, DataProviderFactory dataProviderFactory) {
            return new TicketServiceV2Impl(fileInfoService, externalConfig, resClient, dataProviderFactory);
        }
    }

}
