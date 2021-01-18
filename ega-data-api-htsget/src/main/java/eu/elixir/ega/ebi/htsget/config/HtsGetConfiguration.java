/*
 *
 * Copyright 2020 EMBL - European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package eu.elixir.ega.ebi.htsget.config;

import eu.elixir.ega.ebi.commons.shared.dto.MyExternalConfig;
import eu.elixir.ega.ebi.commons.shared.service.FileDatasetService;
import eu.elixir.ega.ebi.commons.shared.service.FileInfoService;
import eu.elixir.ega.ebi.commons.shared.service.Ga4ghService;
import eu.elixir.ega.ebi.commons.shared.service.JWTService;
import eu.elixir.ega.ebi.commons.shared.service.UserDetailsService;
import eu.elixir.ega.ebi.commons.shared.service.internal.FileDatasetServiceImpl;
import eu.elixir.ega.ebi.commons.shared.service.internal.Ga4ghServiceImpl;
import eu.elixir.ega.ebi.commons.shared.service.internal.JWTServiceImpl;
import eu.elixir.ega.ebi.commons.shared.service.internal.UserDetailsServiceImpl;
import eu.elixir.ega.ebi.htsget.formats.DataProviderFactory;
import eu.elixir.ega.ebi.htsget.service.TicketServiceV2;
import eu.elixir.ega.ebi.htsget.service.internal.ResClient;
import eu.elixir.ega.ebi.htsget.service.internal.TicketServiceV2Impl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;

import static com.nimbusds.jose.jwk.JWKSet.load;

@Configuration
public class HtsGetConfiguration {

    @Bean
    public TicketServiceV2 ticketServiceV2(FileInfoService fileInfoService, MyExternalConfig externalConfig,
                                           ResClient resClient) {
        return new TicketServiceV2Impl(fileInfoService, externalConfig, resClient, new DataProviderFactory());
    }

    @Bean
    public ResClient resClient(LoadBalancerClient loadBalancerClient) {
        return new ResClient(loadBalancerClient);
    }

    @Bean
    public Ga4ghService initGa4ghServiceImpl(final RestTemplate restTemplate,
                                             @Value("${ega.aai.proxy.url}") final String egaAAIProxyURL,
                                             @Value("${ega.aai.proxy.basic-authorization}") final String egaAAIProxyBasicAuth) throws URISyntaxException, MalformedURLException {
        return new Ga4ghServiceImpl(
                restTemplate,
                new URL(egaAAIProxyURL),
                egaAAIProxyBasicAuth
        );
    }

    @Bean
    public JWTService initJWTServiceImpl(@Value("${ega.aai.public.jwk.url}") final String jwkPublicKeyURL,
                                         @Value("${ega.aai.public.jwk.connection-timeout}") final int connectTimeout,
                                         @Value("${ega.aai.public.jwk.read-timeout}") final int readTimeout,
                                         @Value("${ega.aai.public.jwk.size-limit-in-bytes}") final int sizeLimit,
                                         @Value("${ega.aai.public.jwk.key-id:rsa1}") final String keyId) throws IOException, ParseException {
        return new JWTServiceImpl(
                load(
                        new URL(jwkPublicKeyURL),
                        connectTimeout,
                        readTimeout,
                        sizeLimit
                ),
                keyId);
    }

    @Bean
    public FileDatasetService initFileDatasetServiceImpl(final RestTemplate restTemplate) {
        return new FileDatasetServiceImpl(restTemplate);
    }

    @Bean
    public UserDetailsService initUserDetailsServiceImpl(final RestTemplate restTemplate) {
        return new UserDetailsServiceImpl(restTemplate);
    }
}
