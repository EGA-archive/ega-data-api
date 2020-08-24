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
import org.springframework.context.annotation.Import;
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
@Import(V2IntegrationTestConfig.class)
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

}
