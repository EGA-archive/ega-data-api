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
package eu.elixir.ega.ebi.dataedge.config;

import com.nimbusds.jose.jwk.JWKSet;
import eu.elixir.ega.ebi.commons.config.CachingMultipleRemoteTokenService;
import eu.elixir.ega.ebi.commons.config.CachingRemoteTokenService;
import eu.elixir.ega.ebi.commons.config.MyAccessTokenConverter;
import eu.elixir.ega.ebi.commons.config.MyUserAuthenticationConverter;
import eu.elixir.ega.ebi.commons.shared.service.FileDatasetService;
import eu.elixir.ega.ebi.commons.shared.service.Ga4ghService;
import eu.elixir.ega.ebi.commons.shared.service.JWTService;
import eu.elixir.ega.ebi.commons.shared.service.UserDetailsService;
import eu.elixir.ega.ebi.commons.shared.service.internal.FileDatasetServiceImpl;
import eu.elixir.ega.ebi.commons.shared.service.internal.Ga4ghServiceImpl;
import eu.elixir.ega.ebi.commons.shared.service.internal.JWTServiceImpl;
import eu.elixir.ega.ebi.commons.shared.service.internal.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.oauth2.provider.token.AccessTokenConverter;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;

import static com.nimbusds.jose.jwk.JWKSet.load;

@Configuration
@EnableAsync
public class DataEdgeConfig {

    @Bean
    public TaskExecutor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("Async-");
        executor.initialize();
        return executor;
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

    @Profile("!integration-test")
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

    @Profile("integration-test")
    @Bean
    public JWTService initJWTServiceImplIntegrationTest() {
        return new JWTServiceImpl(new JWKSet(), "rsa1");
    }

    @Bean
    public FileDatasetService initFileDatasetServiceImpl(final RestTemplate restTemplate) {
        return new FileDatasetServiceImpl(restTemplate);
    }

    @Bean
    public UserDetailsService initUserDetailsServiceImpl(final RestTemplate restTemplate) {
        return new UserDetailsServiceImpl(restTemplate);
    }

    /**
     * This is a bit of a Hack! MitreID doesn't return 'user_name' but 'user_id',
     * The customized User Authentication Converter simply changes the field name
     * for extraction.
     *
     * @return The converted access token
     */
    @Bean
    public AccessTokenConverter accessTokenConverter(final JWTService jwtService,
                                                     final Ga4ghService ga4ghService,
                                                     final FileDatasetService fileDatasetService,
                                                     final UserDetailsService userDetailsService) {
        return new MyAccessTokenConverter(
                jwtService,
                ga4ghService,
                fileDatasetService,
                new MyUserAuthenticationConverter(),
                userDetailsService
        );
    }

    /**
     * Creates a combined token service including both EGA AAI and Elixir AAI
     * authentication.
     *
     * @param request Unused
     * @param checkTokenUrl EGA AAI token endpoint url
     * @param clientId Client id for the EGA AAI
     * @param clientSecret Client secret for the EGA AAI
     * @param zuulCheckTokenUrl Elixir token endpoint url
     * @param zuulClientId Elixir AAI ID
     * @param zuulClientSecret Elixir AAI client
     *
     * @return A combined authentication token service
     */
    @Profile("enable-aai")
    @Primary
    @Bean
    public RemoteTokenServices remoteTokenServices(HttpServletRequest request,
                                                   final @Value("${auth.server.url}") String checkTokenUrl,
                                                   final @Value("${auth.server.clientId}") String clientId,
                                                   final @Value("${auth.server.clientsecret}") String clientSecret,
                                                   final @Value("${auth.zuul.server.url}") String zuulCheckTokenUrl,
                                                   final @Value("${auth.zuul.server.clientId}") String zuulClientId,
                                                   final @Value("${auth.zuul.server.clientsecret}") String zuulClientSecret,
                                                   final AccessTokenConverter accessTokenConverter) {

        final CachingMultipleRemoteTokenService remoteTokenServices = new CachingMultipleRemoteTokenService();

        // EGA AAI
        CachingRemoteTokenService b = new CachingRemoteTokenService();
        b.setCheckTokenEndpointUrl(checkTokenUrl);
        b.setClientId(clientId);
        b.setClientSecret(clientSecret);
        b.setAccessTokenConverter(accessTokenConverter);
        remoteTokenServices.addRemoteTokenService(b);

        // ELIXIR AAI
        CachingRemoteTokenService a = new CachingRemoteTokenService();
        a.setCheckTokenEndpointUrl(zuulCheckTokenUrl);
        a.setClientId(zuulClientId);
        a.setClientSecret(zuulClientSecret);
        a.setAccessTokenConverter(accessTokenConverter);
        remoteTokenServices.addRemoteTokenService(a);

        return remoteTokenServices;
    }

    /**
     * Sets CORS headers to allow all methods and all hosts.
     *
     * @return CORSfilter
     */
    @Bean
    @Order(0)
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
