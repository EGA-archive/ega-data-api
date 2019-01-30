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
package eu.elixir.ega.ebi.keyproviderservice.config;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.guava.GuavaCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.RestTemplate;

import com.google.common.cache.CacheBuilder;

/**
 * @author asenf
 */
@Configuration
@EnableCaching
@EnableRetry
@EnableEurekaClient
public class MyConfiguration {

    @Value("${ega.key.path}")
    private String[] cipherKeyPath;
    @Value("${ega.keypass.path}")
    private String[] cipherKeyPassPath;
    @Value("${ega.sharedpass.path}")
    private String sharedKeyPath;
    @Value("${ega.publickey.url}")
    private String publicKeyUrl;
    @Value("${ega.legacy.path}")
    private String egaLegacyPath;

    @Bean
    public MyCipherConfig MyCipherConfig() {
        return new MyCipherConfig(cipherKeyPath,
                cipherKeyPassPath,
                sharedKeyPath,
                publicKeyUrl,
                egaLegacyPath);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager simpleCacheManager = new SimpleCacheManager();
        GuavaCache byFileId = new GuavaCache("byId", CacheBuilder.newBuilder()
                .expireAfterAccess(24, TimeUnit.HOURS)
                .build());

        simpleCacheManager.setCaches(Collections.singletonList(byFileId));
        return simpleCacheManager;
    }
}
