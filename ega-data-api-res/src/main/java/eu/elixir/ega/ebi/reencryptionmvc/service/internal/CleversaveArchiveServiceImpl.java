/*
 * Copyright 2016 ELIXIR EGA
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

import static eu.elixir.ega.ebi.reencryptionmvc.config.Constants.FILEDATABASE_SERVICE;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import eu.elixir.ega.ebi.reencryptionmvc.util.FireObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.google.common.base.Strings;

import eu.elixir.ega.ebi.reencryptionmvc.dto.ArchiveSource;
import eu.elixir.ega.ebi.reencryptionmvc.dto.EgaFile;
import eu.elixir.ega.ebi.reencryptionmvc.exception.NotFoundException;
import eu.elixir.ega.ebi.reencryptionmvc.exception.ServerErrorException;
import eu.elixir.ega.ebi.reencryptionmvc.service.ArchiveService;
import eu.elixir.ega.ebi.reencryptionmvc.service.KeyService;
import eu.elixir.ega.ebi.reencryptionmvc.util.FireCommons;

/**
 * @author asenf
 */

@Slf4j
public class CleversaveArchiveServiceImpl implements ArchiveService {

    private RestTemplate restTemplate;

    private KeyService keyService;

    private FireCommons fireCommons;

    public CleversaveArchiveServiceImpl(RestTemplate restTemplate, KeyService keyService, FireCommons fireCommons) {
        this.restTemplate = restTemplate;
        this.keyService = keyService;
        this.fireCommons = fireCommons;
    }

    @Override
    @Cacheable(cacheNames = "archive", key = "#root.methodName + #id")
    public ArchiveSource getArchiveFile(String id, HttpServletRequest request, HttpServletResponse response) {
        String sessionId = Strings.isNullOrEmpty(request.getHeader("Session-Id")) ? "" : request.getHeader("Session-Id") + " ";
        String fileName = getFilePath(id, response, sessionId);
        if (fileName.startsWith("/fire")) fileName = fileName.substring(16);
        // Guess Encryption Format from File
        String encryptionFormat = fileName.toLowerCase().endsWith("gpg") ? "symmetricgpg" : "aes256";
        // Get Cleversafe URL from Filename via Fire
        FireObject fireObject = fireCommons.getFireSignedUrl(fileName, sessionId);

        // Get EgaFile encryption Key
        log.info("Session Id: {} is starting to get key", sessionId);
        String encryptionKey = keyService.getFileKey(id);
        if (encryptionKey == null || encryptionKey.length() == 0) {
            response.setStatus(500);
            throw new ServerErrorException(sessionId + "Error in obtaining Archive Key for ", fileName);
        }
        log.info("Session Id: {} - They key is obtained successfully", sessionId);
        // Build result object and return it (auth is 'null' --> it is part of the URL now)
        return new ArchiveSource(fireObject.getFileURL(), fireObject.getFileSize(), null, encryptionFormat, encryptionKey, null);
    }

    private String getFilePath(String id, HttpServletResponse response, String sessionId) {
        log.info("Session id: {} querying FileDataBaseService" + FILEDATABASE_SERVICE + "/file/{}", sessionId, id);

        ResponseEntity<EgaFile[]> forEntity = restTemplate.getForEntity(FILEDATABASE_SERVICE + "/file/{fileId}", EgaFile[].class, id);
        response.setStatus(forEntity.getStatusCodeValue());
        if (forEntity.getStatusCode() != HttpStatus.OK) {
            final String error = "Session id: " + sessionId + " FileDataBaseService " + FILEDATABASE_SERVICE +
                    "/file/" + id + " return status code " + forEntity.getStatusCode();

            log.error(error);
            response.setStatus(500);
            throw new ServerErrorException(error);
        }

        EgaFile[] body = forEntity.getBody();
        String fileName = (body != null && body.length > 0) ? forEntity.getBody()[0].getFileName() : "";
        if ((body == null || body.length == 0)) {
            log.error("Session id: {} error parsing the body (empty)", sessionId);
            response.setStatus(forEntity.getStatusCodeValue());
            throw new NotFoundException(sessionId + "Can't obtain File data for ID", id);

        }
        log.info("Session id: {} querying FileDataBaseService" + FILEDATABASE_SERVICE + "/file/{} successful", sessionId, id);
        return fileName;
    }

}
