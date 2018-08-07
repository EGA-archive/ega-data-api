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

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.cache2k.Cache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.guava.GuavaCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import com.google.common.cache.CacheBuilder;

import eu.elixir.ega.ebi.reencryptionmvc.dto.CachePage;
import eu.elixir.ega.ebi.reencryptionmvc.dto.EgaAESFileHeader;
import eu.elixir.ega.ebi.reencryptionmvc.dto.MyArchiveConfig;
import eu.elixir.ega.ebi.reencryptionmvc.dto.MyAwsConfig;
import eu.elixir.ega.ebi.reencryptionmvc.dto.MyFireConfig;

/**
 * @author asenf
 */
@Configuration
@EnableCaching
@EnableDiscoveryClient
public class MyConfiguration {

    @Value("${ega.ebi.fire.url}")
    String fireUrl;
    @Value("${ega.ebi.fire.archive}")
    String fireArchive;
    @Value("${ega.ebi.fire.key}")
    String fireKey;

    @Value("${ega.ebi.aws.access.key}")
    String awsKey;
    @Value("${ega.ebi.aws.access.secret}")
    String awsSecretKey;
    @Value("${ega.ebi.aws.endpoint.url}")
    String awsEndpointUrl;
    @Value("${ega.ebi.aws.endpoint.region}")
    String awsRegion;

    @Value("${service.archive.class}")
    String archiveImplBean;

    //@Value("${ega.ebi.cachepage.size}") int pageSize;
    @Value("${eureka.client.serviceUrl.defaultZone}")
    String eurekaUrl;

    @Bean
    @LoadBalanced
    RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public MyFireConfig MyCipherConfig() {
        return new MyFireConfig(fireUrl,
                fireArchive,
                fireKey);
    }

    @Bean
    public MyAwsConfig MyAwsCipherConfig() {
        return new MyAwsConfig(awsKey,
                awsSecretKey, awsEndpointUrl, awsRegion);
    }

    @Bean
    public MyArchiveConfig MyArchiveConfig() {
        return new MyArchiveConfig(archiveImplBean);
    }

    @Bean
    public Cache<String, EgaAESFileHeader> myCache() throws Exception {
        return (new My2KCacheFactory()).getObject();
    }

    //@Bean
    //public Cache<String, byte[]> myPageCache() throws Exception {
    //    return (new My2KCachePageFactory()).getObject();
    //}
    @Bean
    public Cache<String, CachePage> myPageCache() throws Exception {
        int pagesize = 1024 * 1024 * 12;    // 12 MB Page Size
        int pageCount = 1200;               // 1200 * 12 = 14 GB Cache Size
        return (new My2KCachePageFactory(pagesize,
                pageCount,
                awsKey,
                awsSecretKey,
                fireUrl,
                fireArchive,
                fireKey,
                awsEndpointUrl,
                awsRegion,
                eurekaUrl)).getObject();
    }

    //@Bean
    //public CacheManager concurrentCacheManager() {
    //
    //        ConcurrentMapCacheManager manager = new ConcurrentMapCacheManager();
    //        manager.setCacheNames(Arrays.asList("path", "key", "archive"));
    //
    //        return manager;
    //}

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
}
