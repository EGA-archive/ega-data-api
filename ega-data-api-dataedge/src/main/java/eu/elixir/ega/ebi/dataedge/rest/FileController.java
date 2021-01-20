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

import eu.elixir.ega.ebi.commons.exception.NotFoundException;
import eu.elixir.ega.ebi.commons.shared.service.AuthenticationService;
import eu.elixir.ega.ebi.commons.shared.service.PermissionsService;
import eu.elixir.ega.ebi.dataedge.exception.EgaFileNotFoundException;
import eu.elixir.ega.ebi.dataedge.exception.FileNotAvailableException;
import eu.elixir.ega.ebi.dataedge.exception.RangesNotSatisfiableException;
import eu.elixir.ega.ebi.dataedge.exception.UnretrievableFileException;
import eu.elixir.ega.ebi.dataedge.service.NuFileService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.HEAD;

@RestController
@EnableDiscoveryClient
@Slf4j
@RequestMapping("/files")
public class FileController {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private PermissionsService permissionsService;

    @Autowired
    private NuFileService nuFileService;

    /**
     * Writes a requested file, or part of file from the FileService to the supplied
     * response stream.
     */
    @RequestMapping(value = "/{fileId}", method = GET)
    public ResponseEntity<StreamingResponseBody> getFile(@PathVariable String fileId,
                                                         @RequestHeader(value = "Range", required = false, defaultValue = "") String range,
                                                         @RequestHeader(value = "Session-Id", required = false, defaultValue = "<no session id>") String sessionId)
            throws EgaFileNotFoundException, FileNotAvailableException, UnretrievableFileException, RangesNotSatisfiableException {

        log.info(sessionId + " Get file request started");

        Authentication auth = authenticationService.getAuthentication();
        if (!auth.isAuthenticated())
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {
            permissionsService.getFilePermissionsEntity(fileId);
        } catch (NotFoundException e) {
            throw new EgaFileNotFoundException(fileId, e);
        }

        long startCoordinate;
        long endCoordinate;

        long plainFileSize = nuFileService.getPlainFileSize(fileId);

        if (range.length() > 0 && range.startsWith("bytes=")) {
            String[] ranges = range.substring("bytes=".length()).split("-");
            startCoordinate = Long.parseLong(ranges[0]);
            endCoordinate = Long.parseLong(ranges[1]);
        } else {
            startCoordinate = 0;
            endCoordinate = plainFileSize - 1;
        }

        InputStream stream = nuFileService.getSpecificByteRange(fileId, startCoordinate, endCoordinate);

        long contentLength = endCoordinate - startCoordinate + 1;
        HttpStatus status = (contentLength < plainFileSize) ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK;

        return ResponseEntity.status(status)
                .contentLength(contentLength)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(outputStream -> IOUtils.copy(stream, outputStream));
    }

    /**
     * Returns the http header for a file identified by fileId. This mainly includes
     * the content length, but also a random UUID for statistics.
     *
     * @param fileId ELIXIR id of the requested file
     */
    @RequestMapping(value = "/{fileId}", method = HEAD)
    public ResponseEntity<?> getFileHead(@PathVariable String fileId)
            throws EgaFileNotFoundException, FileNotAvailableException, UnretrievableFileException {

        long plainFileSize = nuFileService.getPlainFileSize(fileId);

        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(plainFileSize)
                .header("X-Content-Length", Long.toString(plainFileSize))
                .header("X-Session", UUID.randomUUID().toString())
                .build();
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

    @ExceptionHandler
    public ResponseEntity<?> handleEgaFileNotFoundException(EgaFileNotFoundException exception) {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler
    public ResponseEntity<?> handleUnretrievableFileException(UnretrievableFileException exception, HttpServletRequest request) {
        String sessionID = request.getHeader("Session-Id");
        if (isNullOrEmpty(sessionID))
            sessionID = "<no session ID>";
        log.error(String.format("%s %s", sessionID, exception.getMessage()), exception);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @ExceptionHandler
    public ResponseEntity<?> handleFileNotAvailableException(FileNotAvailableException exception, HttpServletRequest request) {
        String sessionID = request.getHeader("Session-Id");
        if (isNullOrEmpty(sessionID))
            sessionID = "<no session ID>";
        log.error(String.format("%s %s", sessionID, exception.getMessage()), exception);
        return new ResponseEntity<>(HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS);
    }

    @ExceptionHandler
    public ResponseEntity<?> handleRangesNotSatisfiableException(RangesNotSatisfiableException exception) {
        return new ResponseEntity<>(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
    }

    @ExceptionHandler
    public ResponseEntity<?> handleIOException(IOException exception, HttpServletRequest request) {

        String sessionID = request.getHeader("Session-Id");
        if (isNullOrEmpty(sessionID))
            sessionID = "<no session ID>";
        log.error(String.format("%s %s", sessionID, exception.getMessage()), exception);

        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
