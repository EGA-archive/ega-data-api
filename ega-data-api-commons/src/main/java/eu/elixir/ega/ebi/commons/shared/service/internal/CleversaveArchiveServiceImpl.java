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
package eu.elixir.ega.ebi.commons.shared.service.internal;

import static eu.elixir.ega.ebi.commons.config.Constants.FILEDATABASE_SERVICE;

import eu.elixir.ega.ebi.commons.shared.util.FireObject;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import eu.elixir.ega.ebi.commons.shared.dto.ArchiveSource;
import eu.elixir.ega.ebi.commons.shared.dto.EgaFile;
import eu.elixir.ega.ebi.commons.shared.dto.File;
import eu.elixir.ega.ebi.commons.exception.NoContentException;
import eu.elixir.ega.ebi.commons.exception.NotFoundException;
import eu.elixir.ega.ebi.commons.exception.ServerErrorException;
import eu.elixir.ega.ebi.commons.exception.UnavailableForLegalReasonsException;
import eu.elixir.ega.ebi.commons.shared.service.ArchiveService;
import eu.elixir.ega.ebi.commons.shared.service.FileInfoService;
import eu.elixir.ega.ebi.commons.shared.service.KeyService;
import eu.elixir.ega.ebi.commons.shared.util.FireCommons;

/**
 * @author asenf
 */

@Slf4j
public class CleversaveArchiveServiceImpl implements ArchiveService {

    private RestTemplate restTemplate;

    private KeyService keyService;

    private FireCommons fireCommons;

    private FileInfoService fileInfoService;
    
    public CleversaveArchiveServiceImpl(RestTemplate restTemplate, KeyService keyService, FireCommons fireCommons, FileInfoService fileInfoService) {
        this.restTemplate = restTemplate;
        this.keyService = keyService;
        this.fireCommons = fireCommons;
        this.fileInfoService = fileInfoService;
    }

    @Override
    @Cacheable(cacheNames = "archive", key = "#root.methodName + #id")
    public ArchiveSource getArchiveFile(String id, String sessionId) {   
        checkIfValidFile(id, sessionId);        
        String fileName = getFilePath(id, sessionId);
        if (fileName.startsWith("/fire")) fileName = fileName.substring(16);
        // Guess Encryption Format from File
        String encryptionFormat = fileName.toLowerCase().endsWith("gpg") ? "symmetricgpg" : "aes256";
        // Get Cleversafe URL from Filename via Fire
        FireObject fireObject = fireCommons.getFireSignedUrl(fileName, sessionId);

        // Get EgaFile encryption Key
        log.info("Session Id: {} is starting to get key", sessionId);
        String encryptionKey = keyService.getFileKey(id);
        if (encryptionKey == null || encryptionKey.length() == 0) {
            throw new ServerErrorException(sessionId + "Error in obtaining Archive Key for ", fileName);
        }
        log.info("Session Id: {} - They key is obtained successfully", sessionId);
        // Build result object and return it (auth is 'null' --> it is part of the URL now)
        return new ArchiveSource(fireObject.getFileURL(), fireObject.getFileSize(), null, encryptionFormat, encryptionKey, null);
    }

    private String getFilePath(String id, String sessionId) {
        log.info("Session id: {} querying FileDataBaseService " + FILEDATABASE_SERVICE + "/file/{}", sessionId, id);

        ResponseEntity<EgaFile[]> forEntity = restTemplate.getForEntity(FILEDATABASE_SERVICE + "/file/{fileId}", EgaFile[].class, id);
        if (forEntity.getStatusCode() != HttpStatus.OK) {
            final String error = "Session id: " + sessionId + " FileDataBaseService " + FILEDATABASE_SERVICE +
                    "/file/" + id + " return status code " + forEntity.getStatusCode();

            log.error(error);
            throw new ServerErrorException(error);
        }

        EgaFile[] body = forEntity.getBody();
        String fileName = (body != null && body.length > 0) ? forEntity.getBody()[0].getFileName() : "";
        if ((body == null || body.length == 0)) {
            log.error("Session id: {} error parsing the body (empty)", sessionId);
            throw new NotFoundException(sessionId + "Can't obtain File data for ID", id);

        }
        log.info("Session id: {} querying FileDataBaseService " + FILEDATABASE_SERVICE + "/file/{} successful", sessionId, id);
        return fileName;
    }
    
    

    private void checkIfValidFile(String fileId, String sessionId) {
        File reqFile = fileInfoService.getFileInfo(fileId, sessionId); // request added for ELIXIR
        String encryptionAlgorithm = keyService.getEncryptionAlgorithm(fileId);
        
        if (reqFile == null) {
            try {
                Thread.sleep(2500);
            } catch (InterruptedException ignored) {
            }
            reqFile = fileInfoService.getFileInfo(fileId, sessionId);
        }

        // File Archive Type - Reject GPG
        if (reqFile.getFileName().toLowerCase().endsWith("gpg") || (encryptionAlgorithm != null && !"aes256".equals(encryptionAlgorithm))) {
            String noContentExceptionMessage = "Please contact EGA to download this file ".concat(fileId);
            log.error(sessionId.concat(noContentExceptionMessage));
            throw new NoContentException(noContentExceptionMessage);
        }

        if (!"available".equals(reqFile.getFileStatus())) {
            String unavailableForLegalReasonsExceptionMessage = "Unavailable for legal reasons file ".concat(fileId);
            log.error(sessionId.concat(unavailableForLegalReasonsExceptionMessage));
            throw new UnavailableForLegalReasonsException(unavailableForLegalReasonsExceptionMessage);
        }
    }

}
