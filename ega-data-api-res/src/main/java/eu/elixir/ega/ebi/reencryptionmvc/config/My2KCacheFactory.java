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

import eu.elixir.ega.ebi.reencryptionmvc.dto.EgaAESFileHeader;
import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.springframework.beans.factory.FactoryBean;

import java.util.concurrent.TimeUnit;

/**
 * @author asenf
 */
public class My2KCacheFactory implements FactoryBean<Cache<String, EgaAESFileHeader>> { //extends SimpleJdbcDaoSupport

    @Override
    public Cache<String, EgaAESFileHeader> getObject() {
        return new Cache2kBuilder<String, EgaAESFileHeader>() {
        }
                .expireAfterWrite(5, TimeUnit.MINUTES)    // expire/refresh after 5 minutes
                .resilienceDuration(30, TimeUnit.SECONDS) // cope with at most 30 seconds
                // outage before propagating
                // exceptions
                .refreshAhead(false)                       // keep fresh when expiring
                //.loader(this::expensiveOperation)         // auto populating function
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

}