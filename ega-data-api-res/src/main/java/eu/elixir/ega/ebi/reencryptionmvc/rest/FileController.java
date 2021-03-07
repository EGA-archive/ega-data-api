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
package eu.elixir.ega.ebi.reencryptionmvc.rest;

import eu.elixir.ega.ebi.reencryptionmvc.dto.ArchiveSource;
import eu.elixir.ega.ebi.reencryptionmvc.exception.NotFoundException;
import eu.elixir.ega.ebi.reencryptionmvc.service.ArchiveService;
import eu.elixir.ega.ebi.reencryptionmvc.service.ResService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author asenf
 */
@RestController
@EnableDiscoveryClient
@RequestMapping("/file")
public class FileController {

    private ResService resService; // Handle Any Direct Re/Encryption Operations
    private ArchiveService archiveService; // Handle Archived File Operations (file identified by Archive ID)

    // Direct Re/Encryption (e.g. Pipeline/Import)
    @GetMapping
    @ResponseBody
    public void getFile(@RequestParam(value = "sourceFormat", required = false, defaultValue = "plain") String sourceFormat,
                        @RequestParam(value = "sourceKey", required = false) String sourceKey,
                        @RequestParam(value = "sourceIV", required = false) String sourceIV,
                        @RequestParam(value = "destinationFormat", required = false, defaultValue = "plain") String destinationFormat,
                        @RequestParam(value = "destinationKey", required = false) String destinationKey,
                        @RequestParam(value = "destinationIV", required = false) String destinationIV,
                        @RequestParam(value = "filePath") String filePath,
                        @RequestParam(value = "startCoordinate", required = false, defaultValue = "0") long startCoordinate,
                        @RequestParam(value = "endCoordinate", required = false, defaultValue = "0") long endCoordinate,
                        @RequestParam(value = "fileSize", required = false, defaultValue = "0") long fileSize,
                        @RequestParam(value = "httpAuth", required = false, defaultValue = "") String httpAuth,
                        @RequestParam(value = "id", required = false, defaultValue = "0") String id,
                        HttpServletRequest request,
                        HttpServletResponse response) {
        resService.transfer(sourceFormat,
                sourceKey,
                sourceIV,
                destinationFormat,
                destinationKey,
                destinationIV,
                filePath,
                startCoordinate,
                endCoordinate,
                fileSize,
                httpAuth,
                id,
                request,
                response);
    }

    // Archive File (List File ID rather than full specification) --------------
    @GetMapping(value = "/archive/{id}")
    @ResponseBody
    public void getArchiveFile(@PathVariable("id") String id,
                               @RequestParam(value = "destinationFormat", required = false, defaultValue = "plain") String destinationFormat,
                               @RequestParam(value = "destinationKey", required = false) String destinationKey,
                               @RequestParam(value = "destinationIV", required = false) String destinationIV,
                               @RequestParam(value = "startCoordinate", required = false, defaultValue = "0") long startCoordinate,
                               @RequestParam(value = "endCoordinate", required = false, defaultValue = "0") long endCoordinate,
                               @RequestHeader(value = "Range", required = false, defaultValue = "") String range,
                               HttpServletRequest request,
                               HttpServletResponse response) {

        // Resolve Archive ID to actual File Path/URL - Needs Organization-Specific Implementation!
        ArchiveSource source = archiveService.getArchiveFile(id, request, response);

        if (range.length() > 0 && range.startsWith("bytes=") && startCoordinate == 0 && endCoordinate == 0) {
            String[] ranges = range.substring("bytes=".length()).split("-");
            startCoordinate = Long.valueOf(ranges[0]);
            endCoordinate = Long.valueOf(ranges[1]) + 1;
        }
        
        // Merge execution with fully specified function
        getFile(source.getEncryptionFormat(),
                source.getEncryptionKey(),
                source.getEncryptionIV(),
                destinationFormat,
                destinationKey,
                destinationIV,
                source.getFileUrl(),
                startCoordinate,
                endCoordinate,
                source.getSize(),
                source.getAuth(),
                id,
                request,
                response);
    }

    // Archive File (List File ID rather than full specification) --------------
    @GetMapping(value = "/archive/{id}/size")
    public long getArchiveFileSize(@PathVariable("id") String id,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {

        // Resolve Archive ID to actual File Path/URL - Needs Organization-Specific Implementation!
        ArchiveSource source = archiveService.getArchiveFile(id, request, response);
        if (source == null) {
            throw new NotFoundException("Archive File not found, id", id);
        }

        // Return File Size
        return source.getSize();
    }


    @Autowired
    public void setResService(ResService resService) {
        this.resService = resService;
    }

    @Autowired
    public void setArchiveService(ArchiveService archiveService) {
        this.archiveService = archiveService;
    }

}
