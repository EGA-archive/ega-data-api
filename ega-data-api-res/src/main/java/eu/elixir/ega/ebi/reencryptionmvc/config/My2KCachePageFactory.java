/*
 * Copyright 2017 ELIXIR EGA
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

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.springframework.beans.factory.FactoryBean;

import eu.elixir.ega.ebi.reencryptionmvc.dto.CachePage;
import eu.elixir.ega.ebi.reencryptionmvc.service.ArchiveService;
import lombok.extern.slf4j.Slf4j;

/**
 * @author asenf
 */
@Slf4j
public class My2KCachePageFactory implements FactoryBean<Cache<String, CachePage>> { //extends SimpleJdbcDaoSupport

    private final int pageSize;
    private final int pageCount;
    private final ArchiveService archiveService;

        
    public My2KCachePageFactory(ArchiveService archiveService,
                                int pageSize,
                                int pageCount) {
        this.archiveService = archiveService;
        this.pageSize = pageSize;
        this.pageCount = pageCount;
    }

    @Override
    public Cache<String, CachePage> getObject() {
        return new Cache2kBuilder<String, CachePage>() {
        }
                .expireAfterWrite(10, TimeUnit.MINUTES)    // expire/refresh after 10 minutes
                .resilienceDuration(45, TimeUnit.SECONDS) // cope with at most 45 seconds
                // outage before propagating
                // exceptions
                .refreshAhead(false)                      // keep fresh when expiring
                .loader(this::loadPage)                   // auto populating function
                .keepDataAfterExpired(false)
                .loaderExecutor(Executors.newFixedThreadPool(1280))
                .loaderThreadCount(640)
                .entryCapacity(this.pageCount)
                .build();
    }

    @Override
    public Class<?> getObjectType() {
        return Cache.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    /*
     * Cache Page Loader
     * Derive Path and Coordinates from Key
     */
    private CachePage loadPage(String key) {
        return archiveService.loadPageCleversafe(pageSize, key);
    }

}