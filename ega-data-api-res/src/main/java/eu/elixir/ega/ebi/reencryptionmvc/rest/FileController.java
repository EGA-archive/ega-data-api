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

import eu.elixir.ega.ebi.reencryptionmvc.config.NotFoundException;
import eu.elixir.ega.ebi.reencryptionmvc.dto.ArchiveSource;
import eu.elixir.ega.ebi.reencryptionmvc.service.ArchiveService;
import eu.elixir.ega.ebi.reencryptionmvc.service.ResService;
import htsjdk.samtools.seekablestream.RandomInputStream;
import htsjdk.samtools.seekablestream.ebi.BufferedBackgroundInputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author asenf
 */
@RestController
@EnableDiscoveryClient
@RequestMapping("/file")
public class FileController {

    private ResService resService; // Handle Any Direct Re/Encryption Operations
    private ArchiveService archiveService; // Handle Archived File Operations (file identified by Archive ID)

    /**
     * Size of a byte buffer to read/write file (for Random Stream)
     */
    private static final int BUFFER_SIZE = 4096;

    // Direct Re/Encryption (e.g. Pipeline/Import)
    @GetMapping
    @ResponseBody
    public void getFile(@RequestParam(value = "sourceFormat", required = false, defaultValue = "plain") String sourceFormat,
                        @RequestParam(value = "sourceKey", required = false) String sourceKey,
                        @RequestParam(value = "destinationFormat", required = false, defaultValue = "plain") String destinationFormat,
                        @RequestParam(value = "destinationKey", required = false) String destinationKey,
                        @RequestParam(value = "destinationIV", required = false) String destinationIV,
                        @RequestParam(value = "filePath") String filePath,
                        @RequestParam(value = "startCoordinate", required = false, defaultValue = "0") long startCoordinate,
                        @RequestParam(value = "endCoordinate", required = false, defaultValue = "0") long endCoordinate,
                        @RequestParam(value = "fileSize", required = false, defaultValue = "0") long fileSize,
                        @RequestParam(value = "httpAuth", required = false, defaultValue = "") String httpAuth,
                        String id,
                        HttpServletRequest request,
                        HttpServletResponse response) {
        resService.transfer(sourceFormat,
                sourceKey,
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
                               @RequestParam(value = "destinationFormat") String destinationFormat,
                               @RequestParam(value = "destinationKey", required = false) String destinationKey,
                               @RequestParam(value = "destinationIV", required = false) String destinationIV,
                               @RequestParam(value = "startCoordinate", required = false, defaultValue = "0") long startCoordinate,
                               @RequestParam(value = "endCoordinate", required = false, defaultValue = "0") long endCoordinate,
                               HttpServletRequest request,
                               HttpServletResponse response) {

        // Resolve Archive ID to actual File Path/URL - Needs Organization-Specific Implementation!
        ArchiveSource source = archiveService.getArchiveFile(id, response);

        // Merge execution with fully specified function
        getFile(source.getEncryptionFormat(),
                source.getEncryptionKey(),
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
        ArchiveSource source = archiveService.getArchiveFile(id, response);
        if (source == null) {
            throw new NotFoundException("Archive File not found, id", id);
        }

        // Return File Size
        return source.getSize();
    }

    // *************************************************************************
    // *************************************************************************
    // *************************************************************************
    // ************************************************************************* Test Code - To be Removed!

    // Generate a random input stream and return it as a Flux of byte[]
    @GetMapping(value = "/test1/{size}")
    public void getArchiveFile(@PathVariable("size") String size) {
        RandomInputStream in = new RandomInputStream(Integer.parseInt(size));
        BufferedBackgroundInputStream bbIn = new BufferedBackgroundInputStream(in);

        Iterator<byte[]> sourceIterator = bbIn.iterator();

        Iterable<byte[]> iterable = () -> sourceIterator;
        Stream<byte[]> targetStream = StreamSupport.stream(iterable.spliterator(), false);

        //return Flux.fromStream(targetStream);
    }


    // Return a stream of random bytes
    @GetMapping(value = "/test2/{size}")
    public String doDownload(@PathVariable("size") String size,
                             @RequestParam(value = "random", required = false) boolean random,
                             HttpServletRequest request,
                             HttpServletResponse response) throws IOException, NoSuchAlgorithmException {

        // get absolute path of the application
        ServletContext context = request.getServletContext();
        String appPath = context.getRealPath("");
        System.out.println("appPath = " + appPath);

        //String size = "1024";
        InputStream inputStream = new RandomInputStream(Integer.parseInt(size));
        //InputStream inputStream = new RandomInputStream(Integer.parseInt(size));
        MessageDigest md = MessageDigest.getInstance("MD5");
        DigestInputStream dIn = new DigestInputStream(inputStream, md);

        // get MIME type of the file
        String mimeType = "application/octet-stream";
        System.out.println("MIME type: " + mimeType);

        // set content attributes for the response
        response.setContentType(mimeType);
        //response.setContentLength(Integer.parseInt(size));

        // Build Header - Specify UUID
        UUID dlIdentifier = UUID.randomUUID();

        // Set headers for the response
        String headerKey = "X-Session";
        String headerValue = dlIdentifier.toString();
        response.setHeader(headerKey, headerValue);

        // get output stream of the response
        OutputStream outStream = response.getOutputStream();

        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead = -1;

        // write bytes read from the input stream into the output stream
        while ((bytesRead = dIn.read(buffer)) != -1) {
            outStream.write(buffer, 0, bytesRead);
        }

        dIn.close();
        inputStream.close();
        outStream.close();

        byte[] digest_ = md.digest();
        BigInteger bigInt_ = new BigInteger(1, digest_);
        String hashtext_ = bigInt_.toString(16);
        while (hashtext_.length() < 32) {
            hashtext_ = "0" + hashtext_;
        }

        return hashtext_;

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
