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
package eu.elixir.ega.ebi.dataedge.config;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author amohan
 */
public class CustomUsernamePasswordAuthenticationToken extends UsernamePasswordAuthenticationToken {

    private final Map<String, List<String>> datasetFileMapping;

    /**
     * The {@code principal} and {@code credentials} should be set with an
     * {@code Object} that provides the respective property via its
     * {@code Object.toString()} method. The simplest such {@code Object} to use is
     * {@code String}.
     *
     *
     * @param principal The principal being authenticated.
     * @param credentials The credentials that prove the principal is correct.
     * @param authorities The authorities granted to the principal.
     * @param datasetFileMapping A map of datasets and included files
     */
    public CustomUsernamePasswordAuthenticationToken(Object principal, Object credentials, Collection<? extends GrantedAuthority> authorities, Map<String,  List<String>> datasetFileMapping) {
        super(principal, credentials, authorities);
        this.datasetFileMapping = datasetFileMapping;
    }

    /**
     * Returns a map of datasets and included files for the current authentication
     * context.
     *
     * @return A map of datasets and included files, or null.
     */
    public Map<String,  List<String>> getDatasetFileMapping() {
        return datasetFileMapping;
    }

}
