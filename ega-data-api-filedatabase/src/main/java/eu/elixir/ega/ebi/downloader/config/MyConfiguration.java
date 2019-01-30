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
package eu.elixir.ega.ebi.downloader.config;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.guava.GuavaCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.RestTemplate;

import com.google.common.cache.CacheBuilder;

/**
 * @author amohan
 */
@Configuration
@EnableCaching
@EnableRetry
@EnableEurekaClient
public class MyConfiguration {

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager simpleCacheManager = new SimpleCacheManager();
        GuavaCache bySimpleFileId = new GuavaCache("bySimpleFileId",
                CacheBuilder.newBuilder().expireAfterWrite(24, TimeUnit.HOURS).build());
        GuavaCache byFileId = new GuavaCache("byFileId",
                CacheBuilder.newBuilder().expireAfterWrite(24, TimeUnit.HOURS).build());
        GuavaCache fileById = new GuavaCache("fileById",
                CacheBuilder.newBuilder().expireAfterWrite(24, TimeUnit.HOURS).build());
        GuavaCache fileKeyById = new GuavaCache("fileKeyById",
                CacheBuilder.newBuilder().expireAfterWrite(24, TimeUnit.HOURS).build());
        GuavaCache byFileKeyId = new GuavaCache("byFileKeyId",
                CacheBuilder.newBuilder().expireAfterWrite(24, TimeUnit.HOURS).build());
        GuavaCache byFileIdCustom = new GuavaCache("byFileIdCustom",
                CacheBuilder.newBuilder().expireAfterWrite(24, TimeUnit.HOURS).build());
        GuavaCache byFileIndexId = new GuavaCache("byFileIndexId",
                CacheBuilder.newBuilder().expireAfterWrite(24, TimeUnit.HOURS).build());
        GuavaCache byDatasetId = new GuavaCache("byDatasetId",
                CacheBuilder.newBuilder().expireAfterWrite(24, TimeUnit.HOURS).build());
        GuavaCache datasetByFile = new GuavaCache("datasetByFile",
                CacheBuilder.newBuilder().expireAfterWrite(24, TimeUnit.HOURS).build());
        GuavaCache fileIndexFile = new GuavaCache("fileIndexFile",
                CacheBuilder.newBuilder().expireAfterWrite(24, TimeUnit.HOURS).build());
        GuavaCache datasetFiles = new GuavaCache("datasetFiles",
                CacheBuilder.newBuilder().expireAfterWrite(24, TimeUnit.HOURS).build());

        simpleCacheManager.setCaches(Arrays.asList(bySimpleFileId, byFileId, fileById, fileKeyById, byFileKeyId,
                byFileIdCustom, byFileIndexId, byDatasetId, datasetByFile, fileIndexFile, datasetFiles));
        return simpleCacheManager;
    }

}