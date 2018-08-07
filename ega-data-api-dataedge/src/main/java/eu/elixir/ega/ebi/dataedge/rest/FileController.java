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
import eu.elixir.ega.ebi.dataedge.service.FileService;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.HEAD;
import static org.springframework.web.bind.annotation.RequestMethod.OPTIONS;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author asenf
 */
@RestController
@EnableDiscoveryClient
@RequestMapping("/files")
public class FileController {

    @Autowired
    private FileService fileService;

    @RequestMapping(value = "/{file_id}", method = GET)
    public void getFile(@PathVariable String file_id,
                        @RequestParam(value = "destinationFormat", required = false, defaultValue = "aes128") String destinationFormat,
                        @RequestParam(value = "destinationKey", required = false, defaultValue = "") String destinationKey,
                        @RequestParam(value = "destinationIV", required = false, defaultValue = "RANDOM") String destinationIV,
                        @RequestParam(value = "startCoordinate", required = false, defaultValue = "0") long startCoordinate,
                        @RequestParam(value = "endCoordinate", required = false, defaultValue = "0") long endCoordinate,
                        @RequestHeader(value = "Range", required = false, defaultValue = "") String range,
                        HttpServletRequest request,
                        HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (range.length() > 0 && range.startsWith("bytes=") && startCoordinate == 0 && endCoordinate == 0) {
            String[] ranges = range.substring("bytes=".length()).split("-");
            startCoordinate = Long.valueOf(ranges[0]);
            endCoordinate = Long.valueOf(ranges[1]) + 1; // translate into exclusive end coordinate
        }

        fileService.getFile(auth,
                file_id,
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
            Logger.getLogger(FileController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @RequestMapping(value = "/{file_id}", method = HEAD)
    public void getFileHead(@PathVariable String file_id,
                            @RequestParam(value = "destinationFormat", required = false, defaultValue = "aes128") String destinationFormat,
                            HttpServletRequest request,
                            HttpServletResponse response) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        fileService.getFileHead(auth,
                file_id,
                destinationFormat,
                request,
                response);
    }

    // Experimental - Return a BAM Header
    @RequestMapping(value = "/{file_id}/header", method = GET)
    public Object getFileHeader(@PathVariable String file_id,
                                @RequestParam(value = "destinationFormat", required = false, defaultValue = "aes128") String destinationFormat,
                                @RequestParam(value = "destinationKey", required = false, defaultValue = "") String destinationKey) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        return fileService.getFileHeader(auth,
                file_id,
                destinationFormat,
                destinationKey,
                null);  // This by default makes it BAM

    }

    @RequestMapping(value = "/byid/{type}", method = OPTIONS)
    public void getById_(HttpServletResponse response) {
        response.addHeader("Access-Control-Request-Method", "GET");
    }
    
    // {id} -- 'file', 'sample', 'run', ...
    @RequestMapping(value = "/byid/{type}", method = GET)
    @ResponseBody
    public void getById(@PathVariable String type,
                        @RequestParam(value = "accession", required = true) String accession,
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
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new InvalidAuthenticationException(accession);
        }
        fileService.getById(auth,
                type,
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

    @RequestMapping(value = "/variant/byid/{type}", method = OPTIONS)
    public void getByVariantId_(HttpServletResponse response) {
        response.addHeader("Access-Control-Request-Method", "GET");
    }
    
    @RequestMapping(value = "/variant/byid/{type}", method = GET)
    @ResponseBody
    public void getByVariantId(@PathVariable String type,
                               @RequestParam(value = "accession", required = true) String accession,
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
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new InvalidAuthenticationException(accession);
        }
        fileService.getVCFById(auth,
                type,
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

    @RequestMapping(value = "/byid/{type}", method = HEAD)
    @ResponseBody
    public ResponseEntity getHeadById(@PathVariable String type,
                                      @RequestParam(value = "accession", required = true) String accession,
                                      HttpServletRequest request,
                                      HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new InvalidAuthenticationException(accession);
        }

        return fileService.getHeadById(auth, type, accession, request, response);
    }

    @RequestMapping( value = "/**", method = RequestMethod.OPTIONS ) 
    public ResponseEntity handle() { 
        return new ResponseEntity(HttpStatus.OK); 
    }
}
