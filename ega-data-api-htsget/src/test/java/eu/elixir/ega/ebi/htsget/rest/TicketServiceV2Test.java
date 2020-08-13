package eu.elixir.ega.ebi.htsget.rest;

import eu.elixir.ega.ebi.commons.config.InvalidInputException;
import eu.elixir.ega.ebi.commons.shared.config.NotFoundException;
import eu.elixir.ega.ebi.commons.shared.dto.MyExternalConfig;
import eu.elixir.ega.ebi.commons.shared.service.FileInfoService;
import eu.elixir.ega.ebi.htsget.service.TicketServiceV2;
import eu.elixir.ega.ebi.htsget.service.internal.TicketServiceV2Impl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.MalformedURLException;
import java.util.Optional;

@RunWith(SpringRunner.class)
public class TicketServiceV2Test {

    @Autowired
    private TicketServiceV2 service;

    @MockBean
    private FileInfoService fileInfoService;

    @MockBean
    private MyExternalConfig externalConfig;

    @MockBean
    private LoadBalancerClient loadBalancer;

    @TestConfiguration
    public static class Config {
        @Bean
        public TicketServiceV2 ticketService(FileInfoService fileInfoService, MyExternalConfig externalConfig, LoadBalancerClient loadBalancer){
            return new TicketServiceV2Impl(fileInfoService, externalConfig, loadBalancer);
        }
    }

    @Test
    public void readWithIdenticalStartAndEndReturnsValidResponseInRequestedFormat() {

    }

    @Test(expected = InvalidInputException.class)
    public void requestForHeaderWithOtherParametersReturnsInvalidInput() throws MalformedURLException {
        service.getRead("1",
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
    public void requestForNonExistingReferenceReturnsNotFound() throws MalformedURLException {
        service.getRead("2",
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
    public void requestWithStartButNoReferenceNameReturnsInvalidInput() throws MalformedURLException {
        service.getRead("3",
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
    public void requestWithStartAndUnplacedReadsReturnsInvalidInput() throws MalformedURLException {
        service.getRead("3",
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
    public void requestingEntireFileHasSingleURL() throws MalformedURLException {
        Mockito.when(externalConfig.getEgaExternalUrl()).thenReturn("https://test.Htsget.ega.url/");
        HtsgetResponse response = service.getRead("4",
                "BAM",
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
        Assert.assertEquals(1, response.getUrls().length);
        Assert.assertEquals("BAM", response.getFormat());
        Assert.assertEquals("/4", response.getUrls()[0].getUrl().getPath());
    }

    @Test
    public void requestingOnlyHeaderHasSingleURLForHeader() throws MalformedURLException {
        Mockito.when(externalConfig.getEgaExternalUrl()).thenReturn("https://test.Htsget.ega.url/");
        HtsgetResponse response = service.getRead("4",
                "BAM",
                Optional.of("header"),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
        Assert.assertEquals(1, response.getUrls().length);
        Assert.assertEquals("BAM", response.getFormat());
        Assert.assertEquals("/4/header", response.getUrls()[0].getUrl().getPath());
    }

}
