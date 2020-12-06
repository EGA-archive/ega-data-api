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
package eu.elixir.ega.ebi.dataedge.config;

import eu.elixir.ega.ebi.dataedge.service.FileMetaService;
import eu.elixir.ega.ebi.dataedge.service.KeyService;
import eu.elixir.ega.ebi.dataedge.service.NuFileService;
import eu.elixir.ega.ebi.dataedge.service.internal.EBINuFileService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.ac.ebi.ega.fire.service.IFireService;

@Configuration
public class EBINuFileServiceConfig {

    @Bean
    public NuFileService nuFileService(KeyService keyService,
                                       FileMetaService fileDatabase,
                                       IFireService fireService) {
        return new EBINuFileService(keyService, fileDatabase, fireService);
    }
}
