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
package eu.elixir.ega.ebi.downloader;

import com.google.common.cache.CacheBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.guava.GuavaCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
@EnableCaching
//@EnableCircuitBreaker
//@EnableHystrix
@EnableDiscoveryClient
public class DownloaderServiceApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(DownloaderServiceApplication.class, args);
    }

    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(DownloaderServiceApplication.class);
    }

    @Bean
    @LoadBalanced
    RestTemplate restTemplate() {
        return new RestTemplate();
    }

    //@Bean
    //public CacheManager cacheManager() {
    //
    //	ConcurrentMapCache simplefilecache = new ConcurrentMapCache("bySimpleFileId");
    //	ConcurrentMapCache filecache = new ConcurrentMapCache("byFileId");
    //	ConcurrentMapCache fileindexcache = new ConcurrentMapCache("byFileIndexId");
    //	ConcurrentMapCache datasetcache = new ConcurrentMapCache("byDatasetId");
    //
    //	SimpleCacheManager manager = new SimpleCacheManager();
    //	manager.setCaches(Arrays.asList(simplefilecache, filecache, fileindexcache, datasetcache));
    //
    //	return manager;
    //}

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager simpleCacheManager = new SimpleCacheManager();
        GuavaCache bySimpleFileId = new GuavaCache("bySimpleFileId", CacheBuilder.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS)
                .build());
        GuavaCache byFileId = new GuavaCache("byFileId", CacheBuilder.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS)
                .build());
        GuavaCache fileById = new GuavaCache("fileById", CacheBuilder.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS)
                .build());
        GuavaCache byFileIdCustom = new GuavaCache("byFileIdCustom", CacheBuilder.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS)
                .build());
        GuavaCache byFileIndexId = new GuavaCache("byFileIndexId", CacheBuilder.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS)
                .build());
        GuavaCache byDatasetId = new GuavaCache("byDatasetId", CacheBuilder.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS)
                .build());
        GuavaCache datasetByFile = new GuavaCache("datasetByFile", CacheBuilder.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS)
                .build());
        GuavaCache fileIndexFile = new GuavaCache("fileIndexFile", CacheBuilder.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS)
                .build());
        GuavaCache datasetFiles = new GuavaCache("datasetFiles", CacheBuilder.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS)
                .build());

        simpleCacheManager.setCaches(Arrays.asList(bySimpleFileId, byFileId,
                fileById, byFileIdCustom, byFileIndexId, byDatasetId, datasetByFile, fileIndexFile,
                datasetFiles));
        return simpleCacheManager;
    }

/*        
        @Bean
        public CacheManager cacheManager() {
            SimpleCacheManager simpleCacheManager = new SimpleCacheManager();

            GuavaCache fileById = new GuavaCache("fileById", CacheBuilder.newBuilder()
                    .expireAfterAccess(4, TimeUnit.HOURS)
                    .build());
            GuavaCache datasetByFile = new GuavaCache("datasetByFile", CacheBuilder.newBuilder()
                    .expireAfterAccess(4, TimeUnit.HOURS)
                    .build());
            GuavaCache datasetFiles = new GuavaCache("datasetFiles", CacheBuilder.newBuilder()
                    .expireAfterAccess(4, TimeUnit.HOURS)
                    .build());
            GuavaCache fileIndexFile = new GuavaCache("fileIndexFile", CacheBuilder.newBuilder()
                    .expireAfterAccess(4, TimeUnit.HOURS)
                    .build());

            simpleCacheManager.setCaches(Arrays.asList(fileById, datasetByFile, datasetFiles, fileIndexFile));
            return simpleCacheManager;
        }
*/
}
