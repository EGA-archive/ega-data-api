package eu.elixir.ega.ebi.htsget.rest;

import eu.elixir.ega.ebi.htsget.HtsgetServiceApplication;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@AutoConfigureWebMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = {HtsgetServiceApplication.class})
@TestPropertySource(locations = "classpath:application-test.properties")
@Import(V2IntegrationTestConfig.class)
public class V2IntegrationTest {

    @Autowired
    public TicketController controller;

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
