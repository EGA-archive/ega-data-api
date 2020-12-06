/*
 * Copyright 2020 ELIXIR EGA
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
package eu.elixir.ega.ebi.dataedge.service.internal;

import static eu.elixir.ega.ebi.commons.config.Constants.KEYS_SERVICE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import eu.elixir.ega.ebi.dataedge.service.KeyService;
import lombok.extern.slf4j.Slf4j;

@Service
@EnableDiscoveryClient
@Slf4j
public class KeyServiceImpl implements KeyService{
    @Autowired
    private RestTemplate restTemplate;

    @Override
    public String getFileKey(String fileId) {
        ResponseEntity<String> forEntity = restTemplate.getForEntity(KEYS_SERVICE + "/keys/filekeys/{fileId}", String.class, fileId);
        if (forEntity.getStatusCodeValue()!=200){
            log.error("Error to retrieve decryption keys with HTTP return code {} ",forEntity.getStatusCode());
        }
        return forEntity.getBody();
    }
    
    @Override
    public String getEncryptionAlgorithm(String fileId) {
        ResponseEntity<String> forEntity = restTemplate.getForEntity(KEYS_SERVICE + "/keys/encryptionalgorithm/{fileId}", String.class, fileId);
        if (forEntity.getStatusCodeValue()!=200){
            log.error("Error to retrieve encryption algorithm with HTTP return code {} ",forEntity.getStatusCode());
        }
        return forEntity.getBody();
    } 
}