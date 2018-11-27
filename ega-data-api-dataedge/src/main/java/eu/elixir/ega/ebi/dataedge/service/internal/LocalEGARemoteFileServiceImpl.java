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
package eu.elixir.ega.ebi.dataedge.service.internal;

import com.google.common.io.ByteStreams;
import eu.elixir.ega.ebi.dataedge.config.GeneralStreamingException;
import eu.elixir.ega.ebi.dataedge.dto.*;
import eu.elixir.ega.ebi.dataedge.service.DownloaderLogService;
import eu.elixir.ega.ebi.dataedge.service.FileInfoService;
import eu.elixir.ega.ebi.dataedge.service.FileService;
import eu.elixir.ega.ebi.shared.dto.File;
import htsjdk.samtools.cram.ref.CRAMReferenceSource;
import org.apache.commons.lang.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigInteger;
import java.net.URI;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.*;

import static eu.elixir.ega.ebi.shared.Constants.RES_SERVICE;

// Most likely we don't need any custom implementation for LocalEGA
// Default implementation should work "as is"
// TODO: Remove this file after successful testing
//@Profile("LocalEGA")
//@Service
//@Transactional
//@EnableDiscoveryClient
public class LocalEGARemoteFileServiceImpl implements FileService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private DownloaderLogService downloaderLogService;

    @Autowired
    FileInfoService fileInfoService;

    @Override
    public void getFile(String fileId,
                        String destinationFormat,
                        String destinationKey,
                        String destinationIV,
                        long startCoordinate,
                        long endCoordinate,
                        HttpServletRequest request,
                        HttpServletResponse response) {

        // Ascertain Access Permissions for specified File ID
        File reqFile = fileInfoService.getFileInfo(fileId); // request added for ELIXIR
        if (reqFile == null) {
            try {
                Thread.sleep(2500);
            } catch (InterruptedException ignored) {
            }
            reqFile = fileInfoService.getFileInfo(fileId);
        }
        if (reqFile.getFileSize() > 0 && endCoordinate > reqFile.getFileSize())
            endCoordinate = reqFile.getFileSize();

        // Variables needed for responses at the end of the function
        long timeDelta = 0;
        HttpResult xferResult = null;
        MessageDigest outDigest = null;

        // Log request in Event
        //    EventEntry eev_received = createEventEntry(file_id + ":" + destinationFormat + ":" + startCoordinate + ":" + endCoordinate,
        //            ipAddress, "http_request", user_email);
        //    eev_received.setEventType("request_log");
        //    downloaderLogService.logEvent(eev_received);

        // Build Header - Specify UUID (Allow later stats query regarding this transfer)
        UUID dlIdentifier = UUID.randomUUID();
        String headerValue = dlIdentifier.toString();
        response = setHeaders(response, headerValue);

        // Content Length of response (if available)
//            response.setContentLengthLong(getContentLength(reqFile, destinationFormat, startCoordinate, endCoordinate));

        // If byte range, set response 206
        long fileLength = reqFile.getFileSize();
        if (destinationFormat.equalsIgnoreCase("plain")) fileLength -= 16;
        if (startCoordinate > 0 || endCoordinate > 0) {
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            response.addHeader("Content-Range", "bytes " + startCoordinate +
                    "-" + (endCoordinate - 1) + "/" + fileLength);
            if (endCoordinate - startCoordinate < Integer.MAX_VALUE)
                response.setBufferSize((int) (endCoordinate - startCoordinate));
        }

        try {
            // Get Send Stream - http Response, wrap in Digest Stream
            outDigest = MessageDigest.getInstance("MD5");
            DigestOutputStream outDigestStream = new DigestOutputStream(response.getOutputStream(), outDigest);

            // Get RES data stream, and copy it to output stream
            RequestCallback requestCallback = request_ -> request_.getHeaders()
                    .setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL));

            // ----------------------------------------------------------------- Callback Function for Resttemplate
            // Get Data Stream from RES ReEncryptionService --------------------
            ResponseExtractor<HttpResult> responseExtractor = response_ -> {
                List<String> get = response_.getHeaders().get("X-Session"); // RES session UUID
                long b = 0;
                String inHashtext = "";
                try {
                    // Input stream from RES, wrap in DigestStream
                    MessageDigest inDigest = MessageDigest.getInstance("MD5");
                    DigestInputStream inDigestStream = new DigestInputStream(response_.getBody(), inDigest);

                    // The actual Data Transfer - copy bytes from RES to Http connection to client
                    b = ByteStreams.copy(inDigestStream, outDigestStream); // in, outputStream

                    // Done - Close Streams and obtain MD5 of received Stream
                    inDigestStream.close();
                    outDigestStream.close();
                    inHashtext = getDigestText(inDigest.digest());
                } catch (Throwable t) {
                    System.out.println("RemoteFileServiceImpl Error 1: " + t.toString());
                    t.getMessage();
                    String errorMessage = t.toString();
                    throw new GeneralStreamingException(errorMessage, 7);
                }

                // return number of bytes copied, RES session header, and MD5 of RES input stream
                return new HttpResult(b, get, inHashtext); // This is the result of the RestTemplate
            };
            // -----------------------------------------------------------------

            /*
             * CUSTOMISATION: If you access files by absolute path (nearly everyone)
             * then call getResUri with the file path instead of the file ID
             * [...]getResUri(reqFile.getFileName(),destinationFormat[...]
             */

            // Build Request URI with Ticket Parameters and get requested file from RES (timed for statistics)
            timeDelta = System.currentTimeMillis();
            xferResult = restTemplate.execute(getResUri(reqFile.getFileName(), destinationFormat, destinationKey, destinationIV, startCoordinate, endCoordinate), HttpMethod.GET, requestCallback, responseExtractor);
            timeDelta = System.currentTimeMillis() - timeDelta;

        } catch (Throwable t) { // Log Error!
            System.out.println("LocalEGARemoteFileServiceImpl Error 2: " + t.toString());
            String errorMessage = fileId + ":" + destinationFormat + ":" + startCoordinate + ":" + endCoordinate + ":" + t.toString();
            EventEntry eev = downloaderLogService.createEventEntry(errorMessage,  "file");
            downloaderLogService.logEvent(eev);

            throw new GeneralStreamingException(t.toString(), 4);
        } finally {
            if (xferResult != null) {
                // Compare received MD5 with RES
                String inHashtext = xferResult.getMd5();
                String outHashtext = getDigestText(outDigest.digest());

                // Compare - Sent MD5 equals Received MD5? - Log Download in DB
                boolean success = outHashtext.equals(inHashtext);
                double speed = (xferResult.getBytes() / 1024.0 / 1024.0) / (timeDelta / 1000.0);
                long bytes = xferResult.getBytes();
                System.out.println("Success? " + success + ", Speed: " + speed + " MB/s");
                DownloadEntry dle = downloaderLogService.createDownloadEntry(success, speed, fileId,
                         "file", destinationFormat,
                        startCoordinate, endCoordinate, bytes);
                downloaderLogService.logDownload(dle);
            }
        }
    }

    @Override
    @Cacheable(cacheNames = "fileHead")
    public void getFileHead(String fileId,
                            String destinationFormat,
                            HttpServletRequest request,
                            HttpServletResponse response) {

        throw new NotImplementedException();
    }

    /*
     * GA4GH / Semantic Functionality: Use SAMTools to access a File in Cleversafe
     */

    @Override
    @Cacheable(cacheNames = "headerFile")
    public Object getFileHeader(String fileId,
                                String destinationFormat,
                                String destinationKey,
                                CRAMReferenceSource x) {
        throw new NotImplementedException();
    }

    @Override
    public void getById(String fileId,
                        String accession,
                        String format,
                        String reference,
                        long start,
                        long end,
                        List<String> fields,
                        List<String> tags,
                        List<String> notags,
                        boolean header,
                        String destinationFormat,
                        String destinationKey,
                        HttpServletRequest request,
                        HttpServletResponse response) {
        throw new NotImplementedException();
    }

    @Override
    public void getVCFById(String fileId,
                           String accession,
                           String format,
                           String reference,
                           long start,
                           long end,
                           List<String> fields,
                           List<String> tags,
                           List<String> notags,
                           boolean header,
                           String destinationFormat,
                           String destinationKey,
                           HttpServletRequest request,
                           HttpServletResponse response) {
        throw new NotImplementedException();
    }

    /*
     * Helper Functions
     */
    private String getDigestText(byte[] inDigest) {
        BigInteger bigIntIn = new BigInteger(1, inDigest);
        String hashtext = bigIntIn.toString(16);
        while (hashtext.length() < 32) {
            hashtext = "0" + hashtext;
        }
        return hashtext;
    }

    private HttpServletResponse setHeaders(HttpServletResponse response, String headerValue) {
        // Set headers for the response
        String headerKey = "X-Session";
        response.setHeader(headerKey, headerValue);

        // get MIME type of the file (actually, it's always this for now)
        String mimeType = "application/octet-stream";
        System.out.println("MIME type: " + mimeType);

        // set content attributes for the response
        response.setContentType(mimeType);

        return response;
    }

    private URI getResUri(String fileStableIdPath,
                          String destFormat,
                          String destKey,
                          String destIV,
                          Long startCoord,
                          Long endCoord) {
        destFormat = destFormat.equals("AES") ? "aes128" : destFormat; // default to 128-bit if not specified
        String url = RES_SERVICE + "/file";

        // Build components based on Parameters provided
        UriComponentsBuilder builder;

        if (startCoord == 0 && endCoord == 0 && destFormat.equalsIgnoreCase("plain")) {
            builder = UriComponentsBuilder.fromHttpUrl(url)
                    .queryParam("destinationFormat", destFormat)
                    .queryParam("filePath", fileStableIdPath); // TEST!!
        } else if (startCoord == 0 && endCoord == 0) {
            builder = UriComponentsBuilder.fromHttpUrl(url)
                    .queryParam("destinationFormat", destFormat)
                    .queryParam("destinationKey", destKey)
                    .queryParam("filePath", fileStableIdPath); // TEST!!
        } else if (destFormat.equalsIgnoreCase("plain")) {
            builder = UriComponentsBuilder.fromHttpUrl(url)
                    .queryParam("destinationFormat", destFormat)
                    .queryParam("startCoordinate", startCoord)
                    .queryParam("endCoordinate", endCoord)
                    .queryParam("filePath", fileStableIdPath); // TEST!!
        } else {
            builder = UriComponentsBuilder.fromHttpUrl(url)
                    .queryParam("destinationFormat", destFormat)
                    .queryParam("destinationKey", destKey)
                    .queryParam("destinationIV", destIV)
                    .queryParam("startCoordinate", startCoord)
                    .queryParam("endCoordinate", endCoord)
                    .queryParam("filePath", fileStableIdPath); // TEST!!
        }

        return builder.build().encode().toUri();
    }

    @Override
    @Cacheable(cacheNames = "fileSize")
    public ResponseEntity getHeadById(String fileId,
                                      String accession,
                                      HttpServletRequest request,
                                      HttpServletResponse response) {
        throw new NotImplementedException();
    }

    private long getContentLength(File reqFile, String destinationFormat, long startCoordinate, long endCoordinate) {
        long length = 0;

        // EncryptionFormat
        int prefix = 16;
        if (destinationFormat.equalsIgnoreCase("plain"))
            prefix = 0;

        // Range specified?
        if (startCoordinate > 0 || endCoordinate > 0) {
            length = endCoordinate - startCoordinate;
        } else {
            length = reqFile.getFileSize() - 16;
        }

        return (length + prefix);
    }

}
