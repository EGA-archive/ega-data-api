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

import eu.elixir.ega.ebi.dataedge.config.InvalidAuthenticationException;
import eu.elixir.ega.ebi.shared.service.AuthenticationService;
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

/**
 * @author asenf
 */
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
     * Writes a requested file, or part of file from the FileService to the supplied
     * response stream.
     *

     */

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
                        @RequestParam(value = "destinationFormat", required = false, defaultValue = "aes128") String destinationFormat,
                        @RequestParam(value = "destinationKey", required = false, defaultValue = "") String destinationKey,
                        @RequestParam(value = "destinationIV", required = false, defaultValue = "RANDOM") String destinationIV,
                        @RequestParam(value = "startCoordinate", required = false, defaultValue = "0") long startCoordinate,
                        @RequestParam(value = "endCoordinate", required = false, defaultValue = "0") long endCoordinate,
                        @RequestHeader(value = "Range", required = false, defaultValue = "") String range,
                        HttpServletRequest request,
                        HttpServletResponse response) {

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
            log.error(ex.getMessage(), ex);
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
     * Experimental - Return a BAM Header.
     *
     * @param fileId            ELIXIR id of the requested file.
     * @param destinationFormat Requested destination format.
     * @param destinationKey    Encryption key that the result file will be
     *                          encrypted with.
     * @return The BAM file header for the file.
     */
    @RequestMapping(value = "/{fileId}/header", method = GET)
    public Object getFileHeader(@PathVariable String fileId,
                                @RequestParam(value = "destinationFormat", required = false, defaultValue = "aes128") String destinationFormat,
                                @RequestParam(value = "destinationKey", required = false, defaultValue = "") String destinationKey) {

        return fileService.getFileHeader(fileId,
                destinationFormat,
                destinationKey,
                null);  // This by default makes it BAM

    }

    /**
     *
     * @param response
     */
    @RequestMapping(value = "/byid/{type}", method = OPTIONS)
    public void getById_(HttpServletResponse response) {
        response.addHeader("Access-Control-Request-Method", "GET");
    }

    // {id} -- 'file', 'sample', 'run', ...
    /**
     * Writes a requested file (or part of file), selected by accession, from the
     * FileService to the supplied response stream.
     *
     * @param type              Should be set to 'file'.
     * @param accession         Local accession ID of the requested file.
     * @param format            Requested file format. Either 'bam' or 'cram' (case
     *                          insensitive).
     * @param reference         FASTA reference name, required for selecting a
     *                          region with start and end.
     * @param start             Start coordinate when requesting a partial file.
     * @param end               End coordinate when requesting a partial file.
     * @param fields            Data fields to include in the output file.
     * @param tags              Data tags to include in the output file.
     * @param notags            Data tags to exclude from the output file.
     * @param header            Unused.
     * @param destinationFormat Requested destination format.
     * @param destinationKey    Unused.
     * @param request           Unused.
     * @param response          Response stream for the returned data.
     */
    @RequestMapping(value = "/byid/{type}", method = GET)
    @ResponseBody
    public void getById(@PathVariable String type,
                        @RequestParam(value = "accession") String accession,
                        @RequestParam(value = "format", required = false, defaultValue = "bam") String format,
                        @RequestParam(value = "chr", required = false, defaultValue = "") String reference,
                        @RequestParam(value = "start", required = false, defaultValue = "0") long start,
                        @RequestParam(value = "end", required = false, defaultValue = "0") long end,
                        @RequestParam(value = "fields", required = false) List<String> fields,
                        @RequestParam(value = "tags", required = false) List<String> tags,
                        @RequestParam(value = "notags", required = false) List<String> notags,
                        @RequestParam(value = "header", required = false, defaultValue = "true") Boolean header,
                        @RequestParam(value = "destinationFormat", required = false, defaultValue = "plain") String destinationFormat,
                        @RequestParam(value = "destinationKey", required = false, defaultValue = "") String destinationKey,
                        HttpServletRequest request,
                        HttpServletResponse response) {
        Authentication auth = authenticationService.getAuthentication();
        if (auth == null) {
            throw new InvalidAuthenticationException(accession);
        }
        fileService.getById(type,
                accession,
                format,
                reference,
                start,
                end,
                fields,
                tags,
                notags,
                header,
                destinationFormat,
                destinationKey,
                request,
                response);
    }

    /**
     *
     * @param response
     */
    @RequestMapping(value = "/variant/byid/{type}", method = OPTIONS)
    public void getByVariantId_(HttpServletResponse response) {
        response.addHeader("Access-Control-Request-Method", "GET");
    }

    /**
     * Writes a requested file (or part of file), selected by accession, from the
     * FileService to the supplied response stream.
     *
     * @param type              Should be set to 'file'.
     * @param accession         Local accession ID of the requested file.
     * @param format            Unused.
     * @param reference         FASTA reference name, required for selecting a
     *                          region with start and end.
     * @param start             Start coordinate when requesting a partial file.
     * @param end               End coordinate when requesting a partial file.
     * @param fields            Data fields to include in the output file.
     * @param tags              Data tags to include in the output file.
     * @param notags            Data tags to exclude from the output file.
     * @param header            Unused.
     * @param destinationFormat Requested destination format.
     * @param destinationKey    Unused.
     * @param request           Unused.
     * @param response          Response stream for the returned data.
     */
    @RequestMapping(value = "/variant/byid/{type}", method = GET)
    @ResponseBody
    public void getByVariantId(@PathVariable String type,
                               @RequestParam(value = "accession") String accession,
                               @RequestParam(value = "format", required = false, defaultValue = "vcf") String format,
                               @RequestParam(value = "chr", required = false, defaultValue = "") String reference,
                               @RequestParam(value = "start", required = false, defaultValue = "0") long start,
                               @RequestParam(value = "end", required = false, defaultValue = "0") long end,
                               @RequestParam(value = "fields", required = false) List<String> fields,
                               @RequestParam(value = "tags", required = false) List<String> tags,
                               @RequestParam(value = "notags", required = false) List<String> notags,
                               @RequestParam(value = "header", required = false, defaultValue = "true") Boolean header,
                               @RequestParam(value = "destinationFormat", required = false, defaultValue = "plain") String destinationFormat,
                               @RequestParam(value = "destinationKey", required = false, defaultValue = "") String destinationKey,
                               HttpServletRequest request,
                               HttpServletResponse response) {
        Authentication auth = authenticationService.getAuthentication();
        if (auth == null) {
            throw new InvalidAuthenticationException(accession);
        }
        fileService.getVCFById(type,
                accession,
                format,
                reference,
                start,
                end,
                fields,
                tags,
                notags,
                header,
                destinationFormat,
                destinationKey,
                request,
                response);
    }

    /**
     * Writes the content length of a selected file to the reponse parameter, and
     * returns OK or UNAUTHORIZED wheather the file exists and can be accessible.
     *
     * @param fileId    should be "file".
     * @param accession accession id of the requested file.
     * @param request   Unused.
     * @param response  reponse object which will be modified with the content
     *                  length of the requested file head.
     * @return httpStatus OK if the file info was accessible, and the reponse was
     *         modified, otherwise UNAUTHORIZED.
     */
    @RequestMapping(value = "/byid/{type}", method = HEAD)
    @ResponseBody
    public ResponseEntity getHeadById(@PathVariable String type,
                                      @RequestParam(value = "accession") String accession,
                                      HttpServletRequest request,
                                      HttpServletResponse response) {
        Authentication auth = authenticationService.getAuthentication();
        if (auth == null) {
            throw new InvalidAuthenticationException(accession);
        }

        return fileService.getHeadById(type, accession, request, response);
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
