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

import com.google.common.base.Strings;
import eu.elixir.ega.ebi.reencryptionmvc.config.NotFoundException;
import eu.elixir.ega.ebi.reencryptionmvc.dto.ArchiveSource;
import eu.elixir.ega.ebi.reencryptionmvc.dto.CachePage;
import eu.elixir.ega.ebi.reencryptionmvc.dto.EgaFile;
import eu.elixir.ega.ebi.reencryptionmvc.service.ArchiveService;
import eu.elixir.ega.ebi.reencryptionmvc.service.KeyService;

import org.apache.commons.lang.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import static eu.elixir.ega.ebi.reencryptionmvc.config.Constants.FILEDATABASE_SERVICE;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author asenf
 */
@Service
@Profile("enable-filesystem-based-archive")
@Primary
@EnableDiscoveryClient
public class GenericArchiveServiceImpl implements ArchiveService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private KeyService keyService;

    @Override
    @Retryable(maxAttempts = 8, backoff = @Backoff(delay = 2000, multiplier = 2))
    public ArchiveSource getArchiveFile(String id,  HttpServletRequest request, HttpServletResponse response) {
        String sessionId= Strings.isNullOrEmpty(request.getHeader("Session-Id"))? "" : request.getHeader("Session-Id") + " ";
        // Get Filename from EgaFile ID - via DATA service (potentially multiple files)
        ResponseEntity<EgaFile[]> forEntity = restTemplate.getForEntity(FILEDATABASE_SERVICE + "/file/{fileId}", EgaFile[].class, id);
        int statusCodeValue = forEntity.getStatusCodeValue();
        EgaFile[] body = forEntity.getBody();
        if ((body == null || body.length == 0)) {
            throw new NotFoundException(sessionId + "Can't obtain File data for ID", id);
        }
        String fileName = forEntity.getBody()[0].getFileName();

        // Guess Encryption Format from File
        String encryptionFormat = fileName.toLowerCase().endsWith("gpg") ? "symmetricgpg" : "aes256";

        String fileUrlString = body[0].getFileName();
        long size = body[0].getFileSize();

        // Get EgaFile encryption Key
        String encryptionKey = keyService.getFileKey(id);

        // Build result object and return it
        return new ArchiveSource(fileUrlString, size, "", encryptionFormat, encryptionKey, null);
    }

    @Override
    public CachePage loadPageCleversafe(int pageSize, String key) {
        throw new NotImplementedException();        
    }

}
