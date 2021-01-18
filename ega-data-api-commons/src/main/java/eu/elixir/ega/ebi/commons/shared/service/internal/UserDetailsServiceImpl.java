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

import eu.elixir.ega.ebi.commons.shared.service.UserDetailsService;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Optional;

import static eu.elixir.ega.ebi.commons.config.Constants.DATA_SERVICE;
import static java.util.Optional.ofNullable;
import static org.springframework.web.util.UriComponentsBuilder.fromHttpUrl;

public class UserDetailsServiceImpl implements UserDetailsService {

    private final RestTemplate restTemplate;

    public UserDetailsServiceImpl(final RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public Optional<String> getEGAAccountId(final String userEmail) {
        final URI requestURI = fromHttpUrl(DATA_SERVICE)
                .path("/user/{userEmail}/id")
                .buildAndExpand(userEmail)
                .toUri();
        return ofNullable(doGetEGAAccountId(requestURI));
    }

    @Override
    public Optional<String> getEGAAccountIdForElixirId(String elixirId) {
        final URI requestURI = fromHttpUrl(DATA_SERVICE)
                .path("/user/elixir/{elixirId}/id")
                .buildAndExpand(elixirId)
                .toUri();
        return ofNullable(doGetEGAAccountId(requestURI));
    }

    private String doGetEGAAccountId(final URI requestURI) {
        return restTemplate
                .getForEntity(
                        requestURI,
                        String.class)
                .getBody();
    }
}
