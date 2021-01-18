/*
 *
 * Copyright 2021 EMBL - European Bioinformatics Institute
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
package eu.elixir.ega.ebi.commons.shared.service.internal;

import eu.elixir.ega.ebi.commons.shared.service.FileDatasetService;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;

import static eu.elixir.ega.ebi.commons.config.Constants.DATA_SERVICE;
import static org.springframework.web.util.UriComponentsBuilder.fromHttpUrl;

public class FileDatasetServiceImpl implements FileDatasetService {

    private final RestTemplate restTemplate;

    public FileDatasetServiceImpl(final RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public List<String> getFileIds(final String datasetId) {
        final URI requestURI = fromHttpUrl(DATA_SERVICE)
                .path("/datasets/{datasetId}/file-ids")
                .buildAndExpand(datasetId)
                .toUri();

        return restTemplate.exchange(
                requestURI,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<String>>() {
                }
        ).getBody();
    }
}
