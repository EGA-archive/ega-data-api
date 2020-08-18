package eu.elixir.ega.ebi.htsget.rest;

import eu.elixir.ega.ebi.commons.shared.config.NotFoundException;
import eu.elixir.ega.ebi.commons.shared.dto.File;
import eu.elixir.ega.ebi.commons.shared.dto.FileIndexFile;
import eu.elixir.ega.ebi.commons.shared.dto.MyExternalConfig;
import eu.elixir.ega.ebi.commons.shared.service.FileInfoService;
import eu.elixir.ega.ebi.htsget.HtsgetServiceApplication;
import eu.elixir.ega.ebi.htsget.config.LocalTestData;
import eu.elixir.ega.ebi.htsget.formats.DataProviderFactory;
import eu.elixir.ega.ebi.htsget.service.TicketServiceV2;
import eu.elixir.ega.ebi.htsget.service.internal.ResClient;
import eu.elixir.ega.ebi.htsget.service.internal.TicketServiceV2Impl;
import htsjdk.samtools.seekablestream.SeekableFileStream;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;

@RunWith(SpringRunner.class)
@AutoConfigureWebMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = {HtsgetServiceApplication.class})
@TestPropertySource(locations = "classpath:application-test.properties")
public class V2IntegrationTest {

    @Autowired
    public TicketControllerV2 controller;

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    @Ignore
    public void runService() {

        String versionStr = restTemplate.getForObject("/htsget/version", String.class);
        Assert.assertEquals("v1.0.0", versionStr);

        while (true) {

        }
    }

    @TestConfiguration
    public static class Config {
        @Bean
        public TicketServiceV2 ticketService(FileInfoService fileInfoService, MyExternalConfig externalConfig, ResClient resClient, DataProviderFactory dataProviderFactory) {
            return new TicketServiceV2Impl(fileInfoService, externalConfig, resClient, dataProviderFactory);
        }

        @Bean
        public MyExternalConfig externalConfig() {
            MyExternalConfig config = Mockito.mock(MyExternalConfig.class);
            Mockito.when(config.getEgaExternalUrl())
                    .thenReturn("https://ega.ebi.ac.uk:8052/elixir/data/files/");
            return config;
        }

        @Bean
        public FileInfoService fileInfoService() {
            FileInfoService service = Mockito.mock(FileInfoService.class);
            File dataFile = new File();
            dataFile.setFileId(LocalTestData.BAM_FILE_ID);
            dataFile.setFileName("NA12891.bam.cip");
            Mockito.when(service.getFileInfo(LocalTestData.BAM_FILE_ID))
                    .thenReturn(dataFile);
            Mockito.when(service.getFileIndexFile(LocalTestData.BAM_FILE_ID))
                    .thenReturn(new FileIndexFile(LocalTestData.BAM_FILE_ID, LocalTestData.BAM_INDEX_FILE_ID));
            return service;
        }

        @Bean
        public ResClient resClient() throws MalformedURLException, FileNotFoundException {
            ResClient client = Mockito.mock(ResClient.class);
            Mockito.when(client.getStreamForFile(anyString())).thenThrow(NotFoundException.class);
            doReturn(new SeekableFileStream(new java.io.File(LocalTestData.BAM_FILE_PATH))).when(client).getStreamForFile(LocalTestData.BAM_FILE_ID);
            doReturn(new SeekableFileStream(new java.io.File(LocalTestData.BAM_INDEX_FILE_PATH))).when(client).getStreamForFile(LocalTestData.BAM_INDEX_FILE_ID);
            return client;
        }

        @Bean
        public DataProviderFactory dataProviderFactory() {
            return new DataProviderFactory();
        }
    }
}
