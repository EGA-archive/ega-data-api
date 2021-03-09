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
package eu.elixir.ega.ebi.commons.shared.service.internal;

import eu.elixir.ega.ebi.commons.shared.dto.KeyPath;
import eu.elixir.ega.ebi.commons.shared.service.KeyService;

import static eu.elixir.ega.ebi.commons.config.Constants.KEYS_SERVICE;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * @author asenf
 */
@Service
@EnableDiscoveryClient
@Slf4j
public class KeyServiceImpl implements KeyService {

    @Autowired
    private RestTemplate restTemplate;

    @Override
    //@Retryable(maxAttempts = 4, backoff = @Backoff(delay = 5000))
    @Cacheable(cacheNames = "key")
    public String getFileKey(String fileId) {
        ResponseEntity<String> forEntity = restTemplate.getForEntity(KEYS_SERVICE + "/keys/filekeys/{fileId}", String.class, fileId);
        if (forEntity.getStatusCodeValue()!=200){
            log.error("Error to retrieve decryption keys with HTTP return code {} ",forEntity.getStatusCode());
        }
        return forEntity.getBody();
    }

    @Override
    //@Retryable(maxAttempts = 4, backoff = @Backoff(delay = 5000))
    public KeyPath getKeyPath(String key) {
        ResponseEntity<KeyPath> forEntity = restTemplate.getForEntity(KEYS_SERVICE + "/keys/retrieve/{keyId}/private/path", KeyPath.class, key);
        if (forEntity.getStatusCodeValue()!=200) {
            log.error("Error to retrieve decryption path key with HTTP return code {} ", forEntity.getStatusCode());
        }
        return forEntity.getBody();
    }

    /**
     * Gets user's public key by email.
     *
     * @param id User's email.
     * @return ASCII-armored public key.
     */
    @Override
    public String getPublicKey(String id) {
        final ResponseEntity<String> forEntity = restTemplate.getForEntity(KEYS_SERVICE + "/keys/retrieve/{keyId}/public", String.class, id);
        if (forEntity.getStatusCodeValue()!=200) {
            log.error("Error to retrieve decryption public key with HTTP return code {} ", forEntity.getStatusCode());
        }
        return forEntity.getBody();
    }

    /**
     * Gets our private key by ID.
     *
     * @param id Key ID.
     * @return ASCII-armored private key.
     */
    @Override
    public String getPrivateKey(String id) {
        final ResponseEntity<String> forEntity = restTemplate.getForEntity(KEYS_SERVICE + "/keys/retrieve/{keyId}/private/key", String.class, id);
        if (forEntity.getStatusCodeValue()!=200) {
            log.error("Error to retrieve decryption private key with HTTP return code {} ", forEntity.getStatusCode());
        }
        return forEntity.getBody();
    }

    @Override
    public String getEncryptionAlgorithm(String fileId) {
        ResponseEntity<String> forEntity = restTemplate.getForEntity(KEYS_SERVICE + "/keys/encryptionalgorithm/{fileId}", String.class, fileId);
        if (forEntity.getStatusCodeValue()!=200){
            log.error("Error to retrieve decryption keys with HTTP return code {} ",forEntity.getStatusCode());
        }
        return forEntity.getBody();
    } 
}
