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
package eu.elixir.ega.ebi.reencryptionmvc.service.internal;

import eu.elixir.ega.ebi.reencryptionmvc.dto.KeyPath;
import eu.elixir.ega.ebi.reencryptionmvc.service.KeyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import static eu.elixir.ega.ebi.shared.Constants.KEYS_SERVICE;

/**
 * @author asenf
 */
@Service
@EnableDiscoveryClient
public class KeyServiceImpl implements KeyService {

    @Autowired
    private RestTemplate restTemplate;

    @Override
    //@Retryable(maxAttempts = 4, backoff = @Backoff(delay = 5000))
    @Cacheable(cacheNames = "key")
    @HystrixCommand
    public String getFileKey(String fileId) {
        ResponseEntity<String> forEntity = restTemplate.getForEntity(KEYS_SERVICE + "/keys/filekeys/{fileId}", String.class, fileId);
        return forEntity.getBody();
    }

    @Override
    //@Retryable(maxAttempts = 4, backoff = @Backoff(delay = 5000))
    @HystrixCommand
    public KeyPath getKeyPath(String key) {
        ResponseEntity<KeyPath> forEntity = restTemplate.getForEntity(KEYS_SERVICE + "/keys/retrieve/{keyId}/private/path", KeyPath.class, key);
        return forEntity.getBody();
    }

    /**
     * Gets user's public key by email.
     *
     * @param id User's email.
     * @return ASCII-armored public key.
     */
    @Override
    @HystrixCommand
    public String getPublicKey(String id) {
        return restTemplate.getForEntity(KEYS_SERVICE + "/keys/retrieve/{keyId}/public/{keyType}", String.class, id, "email").getBody();
    }

    /**
     * Gets our private key by ID.
     *
     * @param id Key ID.
     * @return ASCII-armored private key.
     */
    @Override
    @HystrixCommand
    public String getPrivateKey(String id) {
        return restTemplate.getForEntity(KEYS_SERVICE + "/keys/retrieve/{keyId}/private/key", String.class, id).getBody();
    }

}
