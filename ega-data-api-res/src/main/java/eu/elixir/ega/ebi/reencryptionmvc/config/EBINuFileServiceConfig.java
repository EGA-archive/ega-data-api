/*
 *
 * Copyright 2020 EMBL - European Bioinformatics Institute
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
 *
 */
package eu.elixir.ega.ebi.reencryptionmvc.config;

import eu.elixir.ega.ebi.reencryptionmvc.service.FileDatabaseClientService;
import eu.elixir.ega.ebi.reencryptionmvc.service.KeyService;
import eu.elixir.ega.ebi.reencryptionmvc.service.NuFileService;
import eu.elixir.ega.ebi.reencryptionmvc.service.internal.EBINuFileService;
import eu.elixir.ega.ebi.reencryptionmvc.service.internal.FileDatabaseClientServiceImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.ega.fire.service.IFireService;

@Configuration
public class EBINuFileServiceConfig {

    @Bean
    public NuFileService nuFileService(KeyService keyService,
                                       FileDatabaseClientService fileDatabase,
                                       IFireService fireService) {
        return new EBINuFileService(keyService, fileDatabase, fireService);
    }

    @Bean
    @LoadBalanced
    public RestTemplate fileDatabaseRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    public FileDatabaseClientService fileDatabaseClient(@Qualifier("fileDatabaseRestTemplate") RestTemplate restTemplate) {
        return new FileDatabaseClientServiceImpl(restTemplate);
    }

}
