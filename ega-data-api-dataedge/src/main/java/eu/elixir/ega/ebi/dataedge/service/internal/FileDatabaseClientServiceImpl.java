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
package eu.elixir.ega.ebi.dataedge.service.internal;

import eu.elixir.ega.ebi.commons.config.Constants;
import eu.elixir.ega.ebi.commons.shared.dto.File;
import eu.elixir.ega.ebi.dataedge.exception.EgaFileNotFoundException;
import eu.elixir.ega.ebi.dataedge.service.FileDatabaseClientService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static eu.elixir.ega.ebi.commons.config.Constants.FILEDATABASE_SERVICE;


public class FileDatabaseClientServiceImpl implements FileDatabaseClientService {

    private final RestTemplate restTemplate;

    public FileDatabaseClientServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public File getById(String egaFileId) throws EgaFileNotFoundException {
        ResponseEntity<File[]> responseEntity = restTemplate.getForEntity(FILEDATABASE_SERVICE + "/file/{fileId}", File[].class, egaFileId);
        if (responseEntity.getStatusCode() == HttpStatus.NOT_FOUND)
            throw new EgaFileNotFoundException(egaFileId);

        return responseEntity.getBody()[0];
    }
}
