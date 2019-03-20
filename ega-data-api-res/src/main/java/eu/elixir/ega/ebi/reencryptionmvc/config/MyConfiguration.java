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

import com.google.common.cache.CacheBuilder;
import eu.elixir.ega.ebi.reencryptionmvc.dto.*;
import no.uio.ifi.crypt4gh.factory.HeaderFactory;
import org.apache.commons.io.IOUtils;
import org.cache2k.Cache;
import org.identityconnectors.common.security.GuardedString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author asenf
 */
@Configuration
@EnableCaching
@EnableDiscoveryClient
public class MyConfiguration {

    @Value("${ega.ebi.fire.url}")
    private String fireUrl;
    @Value("${ega.ebi.fire.archive}")
    private String fireArchive;
    @Value("${ega.ebi.fire.key}")
    private String fireKey;

    @Value("${ega.ebi.aws.access.key}")
    private String awsKey;
    @Value("${ega.ebi.aws.access.secret}")
    private String awsSecretKey;
    @Value("${ega.ebi.aws.endpoint.url}")
    private String awsEndpointUrl;
    @Value("${ega.ebi.aws.endpoint.region}")
    private String awsRegion;

    @Value("${service.archive.class}")
    private String archiveImplBean;

    @Value("${ega.sharedpass.path}")
    private String sharedKeyPath;

    @Autowired
    private LoadBalancerClient loadBalancer;
    
    @Autowired
    private Cache<String, EgaAESFileHeader> myCache;

    @LoadBalanced
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public MyFireConfig MyCipherConfig() {
        return new MyFireConfig(fireUrl, fireArchive, fireKey);
    }

    @Bean
    public MyAwsConfig MyAwsCipherConfig() {
        return new MyAwsConfig(awsKey, awsSecretKey, awsEndpointUrl, awsRegion);
    }

    @Bean
    public MyArchiveConfig MyArchiveConfig() {
        return new MyArchiveConfig(archiveImplBean);
    }

    @Bean
    public Cache<String, EgaAESFileHeader> myCache() {
        return (new My2KCacheFactory()).getObject();
    }

    @Bean
    public Cache<String, CachePage> myPageCache() throws Exception {
        int pagesize = 1024 * 1024 * 12;    // 12 MB Page Size
        int pageCount = 1200;               // 1200 * 12 = 14 GB Cache Size
        return (new My2KCachePageFactory(myCache,
                loadBalancer,
                pagesize,
                pageCount,
                awsKey,
                awsSecretKey,
                fireUrl,
                fireArchive,
                fireKey,
                awsEndpointUrl,
                awsRegion)).getObject();
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
        simpleCacheManager.setCaches(Arrays.asList(key, archive, path));
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

}
