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
package eu.elixir.ega.ebi.commons.config;

import com.google.common.cache.CacheBuilder;
import eu.elixir.ega.ebi.commons.shared.config.ClientUserIpInterceptor;
import eu.elixir.ega.ebi.commons.shared.dto.MyExternalConfig;
import eu.elixir.ega.ebi.commons.cache2k.My2KCacheFactory;
import eu.elixir.ega.ebi.commons.cache2k.My2KCachePageFactory;
import eu.elixir.ega.ebi.commons.shared.dto.EgaAESFileHeader;
import eu.elixir.ega.ebi.commons.shared.service.ArchiveService;
import eu.elixir.ega.ebi.commons.shared.service.DownloaderLogService;
import eu.elixir.ega.ebi.commons.shared.service.FileInfoService;
import eu.elixir.ega.ebi.commons.shared.service.FileLengthService;
import eu.elixir.ega.ebi.commons.shared.service.KeyService;
import eu.elixir.ega.ebi.commons.shared.service.PermissionsService;
import eu.elixir.ega.ebi.commons.shared.service.ResService;
import eu.elixir.ega.ebi.commons.shared.service.internal.CacheResServiceImpl;
import eu.elixir.ega.ebi.commons.shared.service.internal.CleversaveArchiveServiceImpl;
import eu.elixir.ega.ebi.commons.shared.service.internal.FileInfoServiceImpl;
import eu.elixir.ega.ebi.commons.shared.util.FireCommons;
import eu.elixir.ega.ebi.commons.shared.util.S3Commons;
import uk.ac.ebi.ega.fire.properties.HttpClientProperties;
import uk.ac.ebi.ega.fire.service.FireService;
import uk.ac.ebi.ega.fire.service.IFireService;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.cache2k.Cache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.guava.GuavaCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * @author asenf
 */
@Configuration
@EnableCaching
@EnableEurekaClient
public class MyConfiguration {

    @Value("${ega.ega.external.url}")
    private String externalUrl;
    @Value("${ega.ega.cram.fasta.a}")
    private String cramFastaReferenceA;
    @Value("${ega.ega.cram.fasta.b}")
    private String cramFastaReferenceB;
    
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

    // Ribbon Load Balanced Rest Template for communication with other Microservices

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Profile("add-user-ip-headers")
    @Bean
    ClientUserIpInterceptor clientUserIpInterceptor() {
        return new ClientUserIpInterceptor();
    }

    @Profile("add-user-ip-headers")
    @Bean
    @Primary
    @LoadBalanced
    public RestTemplate interceptedRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setInterceptors(Collections.singletonList(clientUserIpInterceptor()));
        return restTemplate;
    }

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager simpleCacheManager = new SimpleCacheManager();
        GuavaCache tokens = new GuavaCache("tokens", CacheBuilder.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build());
        GuavaCache access = new GuavaCache("access", CacheBuilder.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build());
        GuavaCache reqFile = new GuavaCache("reqFile", CacheBuilder.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS)
                .build());
        GuavaCache index = new GuavaCache("index", CacheBuilder.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS)
                .build());
        GuavaCache fileHead = new GuavaCache("fileHead", CacheBuilder.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS)
                .build());
        GuavaCache headerFile = new GuavaCache("headerFile", CacheBuilder.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS)
                .build());
        GuavaCache fileSize = new GuavaCache("fileSize", CacheBuilder.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS)
                .build());
        GuavaCache fileFile = new GuavaCache("fileFile", CacheBuilder.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS)
                .build());
        GuavaCache fileDatasetFile = new GuavaCache("fileDatasetFile", CacheBuilder.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS)
                .build());
        GuavaCache datasetFile = new GuavaCache("datasetFile", CacheBuilder.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS)
                .build());
        GuavaCache indexFile = new GuavaCache("indexFile", CacheBuilder.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS)
                .build());
        GuavaCache datasetsGa4gh = new GuavaCache("datasetsGa4gh", CacheBuilder.newBuilder()
                .expireAfterWrite(50, TimeUnit.MINUTES)
                .build());
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

        simpleCacheManager.setCaches(Arrays.asList(tokens, access, reqFile, index, fileHead, headerFile, fileSize,
                fileFile, fileDatasetFile, datasetFile, indexFile, datasetsGa4gh, key, archive, path, fireSignedUrl));
        return simpleCacheManager;
    }

    @Bean
    public MyExternalConfig MyArchiveConfig() {
        return new MyExternalConfig(externalUrl, cramFastaReferenceA, cramFastaReferenceB);
    }
    
    @Bean
    @Primary
    public ResService initCacheResService(KeyService keyService, Cache<String, EgaAESFileHeader> myHeaderCache,
            My2KCachePageFactory pageDownloader, IFireService fireService, CloseableHttpClient httpClient,
            FileInfoService fileInfoService, FileLengthService fileLengthService, DownloaderLogService downloaderLogService) {
        return new CacheResServiceImpl(keyService, myHeaderCache, pageDownloader,
                new FireCommons(fireURL, base64EncodedCredentials(), fireService),
                new S3Commons(awsKey, awsSecretKey, awsEndpointUrl, awsRegion), httpClient,
                fileInfoService, fileLengthService, downloaderLogService);
    }

    @Bean
    @Primary
    public ArchiveService initCleversaveArchiveServiceImpl(RestTemplate restTemplate, KeyService keyService,
            IFireService fireService, FileInfoService fileInfoService) {
        return new CleversaveArchiveServiceImpl(restTemplate, keyService,
                new FireCommons(fireURL, base64EncodedCredentials(), fireService), fileInfoService);
    }

    @Bean
    public Cache<String, EgaAESFileHeader> myCache() {
        return (new My2KCacheFactory()).getObject();
    }
    
    @Bean
    public My2KCachePageFactory myPageFactory(final CloseableHttpClient httpClient, 
            Cache<String, EgaAESFileHeader> myCache, LoadBalancerClient loadBalancer, IFireService fireService) throws Exception {
        int pagesize = 1024 * 1024 * 13;    // 13 MB Page Size
        return new My2KCachePageFactory(httpClient, 
                myCache,
                loadBalancer,
                pagesize,
                new FireCommons(fireURL, base64EncodedCredentials(), fireService), 
                new S3Commons(awsKey, awsSecretKey, awsEndpointUrl, awsRegion));
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
    
    @Bean
    public FileInfoService fileInfoService(PermissionsService permissionService, RestTemplate restTemplate) {
        return new FileInfoServiceImpl(permissionService, restTemplate);
    }
    
    private String base64EncodedCredentials() {
        return Base64.getEncoder().encodeToString((fireUsername + ":" + firePassword).getBytes());
    }

}
