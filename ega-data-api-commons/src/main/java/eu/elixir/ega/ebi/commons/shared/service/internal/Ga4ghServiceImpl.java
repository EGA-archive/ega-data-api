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

import eu.elixir.ega.ebi.commons.shared.dto.JWTTokenDTO;
import eu.elixir.ega.ebi.commons.shared.service.Ga4ghService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import static org.springframework.web.util.UriComponentsBuilder.fromUri;

public class Ga4ghServiceImpl implements Ga4ghService {

    private final RestTemplate restTemplate;
    private final String authorization;
    private final URI egaAAIProxyURI;

    public Ga4ghServiceImpl(final RestTemplate restTemplate,
                            final URL egaAAIProxyURI,
                            final String authorization) throws URISyntaxException {
        this.restTemplate = restTemplate;
        this.egaAAIProxyURI = egaAAIProxyURI.toURI();
        this.authorization = authorization;
    }

    @Cacheable(value = "datasetsGa4gh", key = "#userId")
    @Override
    public List<String> getDatasets(final String userId) {
        final HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", authorization);

        final URI requestURI = fromUri(egaAAIProxyURI)
                .path("/user/{userId}/permissions/ga4gh")
                .buildAndExpand(userId)
                .toUri();

        final HttpEntity<Void> httpEntity = new HttpEntity<>(headers);
        return restTemplate
                .exchange(
                        requestURI,
                        HttpMethod.GET,
                        httpEntity,
                        JWTTokenDTO.class)
                .getBody()
                .getUserClaimsTokens();
    }
}
