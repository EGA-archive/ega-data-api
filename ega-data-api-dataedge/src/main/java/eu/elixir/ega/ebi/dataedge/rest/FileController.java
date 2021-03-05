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
package eu.elixir.ega.ebi.dataedge.rest;

import com.google.common.base.Strings;

import eu.elixir.ega.ebi.commons.exception.InvalidAuthenticationException;
import eu.elixir.ega.ebi.commons.shared.service.AuthenticationService;
import lombok.extern.slf4j.Slf4j;
import eu.elixir.ega.ebi.dataedge.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.*;

@RestController
@EnableDiscoveryClient
@Slf4j
@RequestMapping("/files")
public class FileController {

    @Autowired
    private FileService fileService;

    @Autowired
    private AuthenticationService authenticationService;

    /**
     *
     * @param fileId            ELIXIR id of the requested file.
     * @param destinationFormat Requested destination format, either 'plain', 'aes',
     *                          or file extension.
     * @param destinationKey    Encryption key that the result file will be
     *                          encrypted with.
     * @param destinationIV     Initialization Vector for for destination file, used
     *                          when requesting a partial AES encrypted file.
     * @param startCoordinate   Start coordinate when requesting a partial file.
     * @param endCoordinate     End coordinate when requesting a partial file.
     * @param range
     * @param request           Unused.
     * @param response          Response stream for the returned data.
     */
    @RequestMapping(value = "/{fileId}", method = GET)
    public void getFile(@PathVariable String fileId,
                        @RequestParam(value = "destinationFormat", required = false, defaultValue = "plain") String destinationFormat,
                        @RequestParam(value = "destinationKey", required = false, defaultValue = "") String destinationKey,
                        @RequestParam(value = "destinationIV", required = false) String destinationIV,
                        @RequestParam(value = "startCoordinate", required = false, defaultValue = "0") long startCoordinate,
                        @RequestParam(value = "endCoordinate", required = false, defaultValue = "0") long endCoordinate,
                        @RequestHeader(value = "Range", required = false, defaultValue = "") String range,
                        HttpServletRequest request,
                        HttpServletResponse response) {
		String sessionId= Strings.isNullOrEmpty(request.getHeader("Session-Id"))? "" : request.getHeader("Session-Id") + " ";
		log.info(sessionId + "Get file request started");
		
        if (range.length() > 0 && range.startsWith("bytes=") && startCoordinate == 0 && endCoordinate == 0) {
            String[] ranges = range.substring("bytes=".length()).split("-");
            startCoordinate = Long.valueOf(ranges[0]);
            endCoordinate = Long.valueOf(ranges[1]) + 1; // translate into exclusive end coordinate
        }

        fileService.getFile(fileId,
                destinationFormat,
                destinationKey,
                destinationIV,
                startCoordinate,
                endCoordinate,
                request,
                response);

        try {
            response.flushBuffer();
        } catch (IOException ex) {
            log.error(sessionId + ex.getMessage(), ex);
        }
    }

    /**
     * Returns the http header for a file identified by fileId. This mainly includes
     * the content length, but also a random UUID for statistics.
     *
     * @param fileId            ELIXIR id of the requested file
     * @param destinationFormat Requested destination format.
     * @param request           Unused.
     * @param response          Response stream for the returned data.
     */
    @RequestMapping(value = "/{fileId}", method = HEAD)
    public void getFileHead(@PathVariable String fileId,
                            @RequestParam(value = "destinationFormat", required = false, defaultValue = "aes128") String destinationFormat,
                            HttpServletRequest request,
                            HttpServletResponse response) {

        fileService.getFileHead(fileId,
                destinationFormat,
                request,
                response);
    }

    /**
     * Sets return status OK for all OPTIONS requests.
     *
     * @return {@code HttpStatus.OK}
     */
    @RequestMapping(value = "/**", method = RequestMethod.OPTIONS)
    public ResponseEntity handle() {
        return new ResponseEntity(HttpStatus.OK);
    }

}
