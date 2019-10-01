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
package eu.elixir.ega.ebi.dataedge.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.guava.GuavaCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import com.google.common.cache.CacheBuilder;

import eu.elixir.ega.ebi.shared.config.ClientUserIpInterceptor;
import eu.elixir.ega.ebi.shared.dto.MyExternalConfig;

/**
 * @author asenf
 */
@Configuration
@EnableCaching
@EnableEurekaClient
@EnableAsync
public class MyConfiguration {

    @Value("${ega.ega.external.url}")
    private String externalUrl;
    @Value("${ega.ega.cram.fasta.a}")
    private String cramFastaReferenceA;
    @Value("${ega.ega.cram.fasta.b}")
    private String cramFastaReferenceB;

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

        simpleCacheManager.setCaches(Arrays.asList(tokens, access, reqFile, index,
                fileHead, headerFile, fileSize, fileFile, fileDatasetFile));
        return simpleCacheManager;
    }

    @Bean
    public MyExternalConfig MyArchiveConfig() {
        return new MyExternalConfig(externalUrl, cramFastaReferenceA, cramFastaReferenceB);
    }

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
}