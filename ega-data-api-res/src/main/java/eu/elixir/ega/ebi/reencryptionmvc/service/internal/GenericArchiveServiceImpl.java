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

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import eu.elixir.ega.ebi.reencryptionmvc.config.NotFoundException;
import eu.elixir.ega.ebi.reencryptionmvc.dto.ArchiveSource;
import eu.elixir.ega.ebi.reencryptionmvc.dto.EgaFile;
import eu.elixir.ega.ebi.reencryptionmvc.service.ArchiveService;
import eu.elixir.ega.ebi.reencryptionmvc.service.KeyService;

/**
 * @author asenf
 */
@Service
@Profile("enable-filesystem-based-archive")
@Primary
@EnableDiscoveryClient
public class GenericArchiveServiceImpl implements ArchiveService {

    //private final String SERVICE_URL = "http://DOWNLOADER";
    private final String SERVICE_URL = "http://FILEDATABASE";

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    private KeyService keyService;

    @Override
    @Retryable(maxAttempts = 8, backoff = @Backoff(delay = 2000, multiplier = 2))
    public ArchiveSource getArchiveFile(String id, HttpServletResponse response) {

        // Get Filename from EgaFile ID - via DATA service (potentially multiple files)
        ResponseEntity<EgaFile[]> forEntity = restTemplate.getForEntity(SERVICE_URL + "/file/{file_id}", EgaFile[].class, id);
        int statusCodeValue = forEntity.getStatusCodeValue();
        EgaFile[] body = forEntity.getBody();
        if ((body == null || body.length == 0)) {
            throw new NotFoundException("Can't obtain File data for ID", id);
        }
        String fileName = (body != null && body.length > 0) ? forEntity.getBody()[0].getFileName() : "";

        // Guess Encryption Format from File
        String encryptionFormat = fileName.toLowerCase().endsWith("gpg") ? "symmetricgpg" : "aes256";
        String keyKey = encryptionFormat.toLowerCase().equals("symmetricgpg") ? "GPG" : "AES";

        String fileUrlString = body[0].getFileName();
        long size = body[0].getFileSize();

        // Get EgaFile encryption Key
        String encryptionKey = keyService.getFileKey(keyKey);

        // Build result object and return it
        return new ArchiveSource(fileUrlString, size, "", encryptionFormat, encryptionKey);
    }

}
