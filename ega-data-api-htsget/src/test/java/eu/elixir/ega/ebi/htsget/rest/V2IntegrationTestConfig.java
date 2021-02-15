package eu.elixir.ega.ebi.htsget.rest;

import eu.elixir.ega.ebi.commons.exception.NotFoundException;
import eu.elixir.ega.ebi.commons.shared.dto.File;
import eu.elixir.ega.ebi.commons.shared.dto.FileIndexFile;
import eu.elixir.ega.ebi.commons.shared.dto.MyExternalConfig;
import eu.elixir.ega.ebi.commons.shared.service.FileInfoService;
import eu.elixir.ega.ebi.htsget.config.LocalTestData;
import eu.elixir.ega.ebi.htsget.formats.DataProviderFactory;
import eu.elixir.ega.ebi.htsget.service.TicketService;
import eu.elixir.ega.ebi.htsget.service.internal.ResClient;
import eu.elixir.ega.ebi.htsget.service.internal.TicketServiceImpl;
import htsjdk.samtools.seekablestream.SeekableFileStream;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;

@TestConfiguration
public class V2IntegrationTestConfig {
    @Bean
    public TicketService ticketService(FileInfoService fileInfoService, MyExternalConfig externalConfig, ResClient resClient, DataProviderFactory dataProviderFactory) {
        return new TicketServiceImpl(fileInfoService, externalConfig, resClient, dataProviderFactory);
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
