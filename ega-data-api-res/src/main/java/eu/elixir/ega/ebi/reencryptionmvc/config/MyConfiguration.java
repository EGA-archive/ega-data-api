/*
 * Copyright 2016 ELIXIR EGA
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
 */
package eu.elixir.ega.ebi.reencryptionmvc.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.cache2k.Cache;
import org.identityconnectors.common.security.GuardedString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.guava.GuavaCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import com.google.common.cache.CacheBuilder;

import eu.elixir.ega.ebi.reencryptionmvc.cache2k.My2KCacheFactory;
import eu.elixir.ega.ebi.reencryptionmvc.cache2k.My2KCachePageFactory;
import eu.elixir.ega.ebi.reencryptionmvc.dto.EgaAESFileHeader;
import eu.elixir.ega.ebi.reencryptionmvc.util.FireCommons;
import eu.elixir.ega.ebi.reencryptionmvc.util.S3Commons;
import htsjdk.samtools.seekablestream.ISeekableStreamFactory;
import htsjdk.samtools.seekablestream.SeekableStreamFactory;
import no.uio.ifi.crypt4gh.factory.HeaderFactory;
import uk.ac.ebi.ega.fire.service.FireService;
import uk.ac.ebi.ega.fire.service.IFireService;
import uk.ac.ebi.ega.fire.properties.HttpClientProperties;

/**
 * @author asenf
 */
@Configuration
@EnableCaching
@EnableDiscoveryClient
public class MyConfiguration {

    @Value("${fire.user:testUser}") 
    private String fireUsername;
    @Value("${fire.password:testPass}") 
    private String firePassword;
    @Value("${fire.url:testUrl}") 
    private String fireURL;

    @Value("${ega.ebi.aws.access.key:#{null}}")
    private String awsKey;
    @Value("${ega.ebi.aws.access.secret:#{null}}")
    private String awsSecretKey;
    @Value("${ega.ebi.aws.endpoint.url:#{null}}")
    private String awsEndpointUrl;
    @Value("${ega.ebi.aws.endpoint.region:#{null}}")
    private String awsRegion;

    @Value("${ega.sharedpass.path}")
    private String sharedKeyPath;

    @Bean
    public ISeekableStreamFactory seekableStreamFactory() {
        return SeekableStreamFactory.getInstance();
    }
    
    @LoadBalanced
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public Cache<String, EgaAESFileHeader> myCache() {
        return (new My2KCacheFactory()).getObject();
    }

    @Bean
    public My2KCachePageFactory myPageFactory(final CloseableHttpClient httpClient, 
            Cache<String, EgaAESFileHeader> myCache, LoadBalancerClient loadBalancer, IFireService fireService) throws Exception {
        int pagesize = 1024 * 1024 * 12;    // 12 MB Page Size
        return new My2KCachePageFactory(httpClient, 
                myCache,
                loadBalancer,
                pagesize,
                new FireCommons(fireURL, base64EncodedCredentials(), fireService), 
                new S3Commons(awsKey, awsSecretKey, awsEndpointUrl, awsRegion));
    }
    
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager simpleCacheManager = new SimpleCacheManager();
        GuavaCache key = new GuavaCache("key", CacheBuilder.newBuilder().build());
        GuavaCache archive = new GuavaCache("archive", CacheBuilder.newBuilder()
                .expireAfterAccess(20, TimeUnit.HOURS)
                .build());
        GuavaCache path = new GuavaCache("path", CacheBuilder.newBuilder()
                .expireAfterAccess(20, TimeUnit.HOURS)
                .build());
        GuavaCache fireSignedUrl = new GuavaCache("fireSignedUrl", CacheBuilder.newBuilder()
                .expireAfterAccess(2, TimeUnit.HOURS)
                .build());
        simpleCacheManager.setCaches(Arrays.asList(key, archive, path, fireSignedUrl));
        return simpleCacheManager;
    }

    @Bean
    public HeaderFactory headerFactory() {
        return HeaderFactory.getInstance();
    }

    @Bean
    public GuardedString sharedKey() throws IOException {
        String passphrase = IOUtils.readLines(new FileInputStream(sharedKeyPath), Charset.defaultCharset()).iterator().next();
        return new GuardedString(passphrase.toCharArray());
    }
    
    @ConfigurationProperties(prefix = "httpclient.connection")
    @Bean
    public HttpClientProperties initHttpClientProperties() {
        return new HttpClientProperties();
    }

    @Bean
    public CloseableHttpClient initHttpClient(final HttpClientProperties httpClientProperties) {

        final ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setBufferSize(httpClientProperties.getBufferSize())
                .build();

        final PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(httpClientProperties.getMaxTotal());
        connectionManager.setDefaultMaxPerRoute(httpClientProperties.getDefaultMaxPerRoute());
        connectionManager.setValidateAfterInactivity(5000);

        final RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(httpClientProperties.getTimeout() * 1000)
                .setConnectionRequestTimeout(httpClientProperties.getTimeout() * 1000)
                .setSocketTimeout(0)
                .build();

        return HttpClients.custom()
                .setDefaultConnectionConfig(connectionConfig)
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .setDefaultHeaders(Collections.singleton(new BasicHeader("Authorization", "Basic ".concat(base64EncodedCredentials()))))
                .build();
    }

    @Bean
    public IFireService initFireService(final CloseableHttpClient httpClient) {
        return new FireService(httpClient, fireURL, 3);
    }

    private String base64EncodedCredentials() {
        return Base64.getEncoder().encodeToString((fireUsername + ":" + firePassword).getBytes());
    }
    
}
