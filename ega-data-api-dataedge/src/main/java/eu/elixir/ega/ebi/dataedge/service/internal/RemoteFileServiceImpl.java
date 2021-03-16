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

import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;
import eu.elixir.ega.ebi.commons.exception.GeneralStreamingException;
import eu.elixir.ega.ebi.commons.exception.NoContentException;
import eu.elixir.ega.ebi.commons.exception.UnavailableForLegalReasonsException;
import eu.elixir.ega.ebi.commons.shared.dto.DownloadEntry;
import eu.elixir.ega.ebi.commons.shared.dto.EventEntry;
import eu.elixir.ega.ebi.commons.shared.dto.File;
import eu.elixir.ega.ebi.commons.shared.dto.MyExternalConfig;
import eu.elixir.ega.ebi.commons.shared.service.ArchiveService;
import eu.elixir.ega.ebi.commons.shared.service.DownloaderLogService;
import eu.elixir.ega.ebi.commons.shared.service.FileInfoService;
import eu.elixir.ega.ebi.commons.shared.service.FileLengthService;
import eu.elixir.ega.ebi.commons.shared.service.KeyService;
import eu.elixir.ega.ebi.commons.shared.service.ResService;
import eu.elixir.ega.ebi.dataedge.dto.HttpResult;
import eu.elixir.ega.ebi.dataedge.service.FileService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URI;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.apache.catalina.connector.OutputBuffer.DEFAULT_BUFFER_SIZE;

@Service
@EnableDiscoveryClient
@Slf4j
public class RemoteFileServiceImpl implements FileService {

    @Autowired
    private OkHttpClient client;

    @Autowired
    private LoadBalancerClient loadBalancer;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${res.connection.chunksize.header}")
    private int resHeaderChunkSize;

    @Value("${res.connection.chunksize.data}")
    private int resDataChunkSize;

    @Value("${res.connection.chunksize.index}")
    private int resIndexChunkSize;

    // Database Repositories/Services

    @Autowired
    private MyExternalConfig externalConfig;

    @Autowired
    private DownloaderLogService downloaderLogService;

    @Autowired
    private FileInfoService fileInfoService;

    @Autowired
    private FileLengthService fileLengthService;
    
    @Autowired
    private KeyService keyService;
    
    @Autowired
    private ResService resService; 
    
    @Autowired
    private ArchiveService archiveService;
    

    /**
     * Writes a requested file, or part of file from the FileService to the
     * supplied response stream.
     *
     * @param fileId ELIXIR id of the requested file.
     * @param destinationFormat Requested destination format, either 'plain',
     *     'aes', or file extension.
     * @param destinationKey Encryption key that the result file will be
     *     encrypted with.
     * @param destinationIV Initialization Vector for for destination file, used
     *     when requesting a partial AES encrypted file.
     * @param startCoordinate Start coordinate when requesting a partial file.
     * @param endCoordinate End coordinate when requesting a partial file.
     * @param request Unused.
     * @param response Response stream for the returned data.
     */
    @Override
    public void getFile(String fileId,
                        String destinationFormat,
                        String destinationKey,
                        String destinationIV,
                        long startCoordinate,
                        long endCoordinate,
                        HttpServletRequest request,
                        HttpServletResponse response) {

		String sessionId= Strings.isNullOrEmpty(request.getHeader("Session-Id"))? "" : request.getHeader("Session-Id") + " ";
        // Ascertain Access Permissions for specified File ID
        File reqFile = fileInfoService.getFileInfo(fileId, sessionId); // request added for ELIXIR
        String encryptionAlgorithm = keyService.getEncryptionAlgorithm(fileId);

        if (reqFile == null) {
            try {
                Thread.sleep(2500);
            } catch (InterruptedException ignored) {
            }
            reqFile = fileInfoService.getFileInfo(fileId, sessionId);
        }
        if (reqFile.getFileSize() > 0 && endCoordinate > reqFile.getFileSize())
            endCoordinate = reqFile.getFileSize();

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

        // Variables needed for responses at the end of the function
        long timeDelta = 0;
        HttpResult xferResult = null;
        MessageDigest outDigest = null;

        // Build Header - Specify UUID (Allow later stats query regarding this transfer)
        UUID dlIdentifier = UUID.randomUUID();
        String headerValue = dlIdentifier.toString();
        response = setHeaders(response, headerValue);

        long fileLength = fileLengthService.getContentLength(reqFile, destinationFormat, startCoordinate, endCoordinate);

        // Content Length of response (if available)
        response.setContentLengthLong(fileLength);

        // If byte range, set response 206
        if (startCoordinate > 0 || endCoordinate > 0) {
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            response.addHeader("Content-Range", "bytes " + startCoordinate +
                    "-" + (endCoordinate - 1) + "/" + fileLength);
            response.setBufferSize(DEFAULT_BUFFER_SIZE);
        }

        try {
            // Get Send Stream - http Response, wrap in Digest Stream
            outDigest = MessageDigest.getInstance("MD5");
            try (DigestOutputStream outDigestStream = new DigestOutputStream(response.getOutputStream(), outDigest)) {

                // Get RES data stream, and copy it to output stream
                RequestCallback requestCallback = request_ -> {
                    request_.getHeaders().set("Session-Id", sessionId.trim());
                    request_.getHeaders().setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL));
                };
                // -----------------------------------------------------------------
                // Callback Function for Resttemplate
                // Get Data Stream from RES ReEncryptionService
                // -----------------------------------------------------------------
                ResponseExtractor<HttpResult> responseExtractor = responseExtractor(destinationFormat, destinationIV, startCoordinate, outDigestStream, sessionId);

                // -----------------------------------------------------------------

                

                // Build Request URI with Ticket Parameters and get requested file from RES (timed for statistics)
                timeDelta = System.currentTimeMillis();
                int cnt = 2;
                do {
                    xferResult = restTemplate.execute(getResUri(fileId, destinationFormat, destinationKey, 
                            destinationIV, startCoordinate, endCoordinate), HttpMethod.GET, requestCallback, 
                            responseExtractor);
                } while (xferResult.getBytes() <= 0 && cnt-- > 0);
                timeDelta = System.currentTimeMillis() - timeDelta;

            }
            
        } catch (Throwable t) { // Log Error!
            log.error(sessionId + " Throwable execute error " , t);
            String errorMessage = fileId + ":" + destinationFormat + ":" + startCoordinate + ":" + endCoordinate + ":"
                    + t.toString();
            if (errorMessage != null && errorMessage.length() > 256) {
                errorMessage = errorMessage.substring(0, 256);
            }
            EventEntry eev = downloaderLogService.createEventEntry(errorMessage, "file");
            downloaderLogService.logEvent(eev);

            throw new GeneralStreamingException(sessionId +" "+ t.toString(), 4);
        } finally {
            if (xferResult != null) {

                // Compare received MD5 with RES
                String inHashtext = xferResult.getMd5();
                String outHashtext = getDigestText(outDigest.digest());

                // Compare - Sent MD5 equals Received MD5? - Log Download in DB
                boolean success = outHashtext.equals(inHashtext);
                double speed = (xferResult.getBytes() / 1024.0 / 1024.0) / (timeDelta / 1000.0);
                long bytes = xferResult.getBytes();
                log.info(sessionId + "Success? " + success + ", Speed: " + speed + " MB/s");
                DownloadEntry dle = downloaderLogService.createDownloadEntry(success, speed, fileId,
                         "file", destinationFormat,
                        startCoordinate, endCoordinate, bytes);
                downloaderLogService.logDownload(dle);
            }
        }
    }

    private ResponseExtractor<HttpResult> responseExtractor(final String destinationFormat, final String destinationIV,
            final long startCoordinate, final OutputStream outDigestStream, final String sessionId) {
        return response_ -> {
            List<String> get = response_.getHeaders().get("X-Session"); // RES session UUID
            long b = 0;
            String inHashtext = "";
            try(InputStream inOrig = response_.getBody()) {
                // If the stream is encrypted, and coordinates are specified,
                // there is a possibility that 0-15 extra bytes are sent, because
                // of the 16-byte AES Block size - read these bytes before moving on
                
                if (destinationFormat.toLowerCase().startsWith("aes") && destinationIV != null
                        && destinationIV.length() > 0) {
                    long blockStart = (startCoordinate / 16) * 16;
                    int blockDelta = (int) (startCoordinate - blockStart);
                    if (blockDelta > 0)
                        inOrig.read(new byte[blockDelta]);
                }
                // Input stream from RES, wrap in DigestStream
                MessageDigest inDigest = MessageDigest.getInstance("MD5");
                try (DigestInputStream inDigestStream = new DigestInputStream(inOrig, inDigest)) {
                    // The actual Data Transfer - copy bytes from RES to Http connection to client
                    b = ByteStreams.copy(inDigestStream, outDigestStream); // in, outputStream
                    inHashtext = getDigestText(inDigest.digest());
                }
            } catch (Throwable t) {
                log.error(sessionId + " Throwable responseExtractor error ", t);
                throw new GeneralStreamingException(sessionId + " " + t.getMessage(), 7);
            }
            // return number of bytes copied, RES session header, and MD5 of RES input stream
            return new HttpResult(b, get, inHashtext); // This is the result of the RestTemplate
        };
    }

    /**
     * Returns the http header for a file identified by fileId. This mainly
     * includes the content length, but also a random UUID for statistics.
     *
     * @param fileId ELIXIR id of the requested file
     * @param destinationFormat Requested destination format.
     * @param request Unused.
     * @param response Response stream for the returned data.
     */


    //Hack
    private boolean isRDConnect(File reqFile) {
        return reqFile.getDatasetId().equalsIgnoreCase("EGAD00001003952");
    }

    /*
     * Helper Functions
     */

     /**
      * Returns the integer digest as a zero-padded length 32 string.
      *
      * @param inDigest input digest to convert.
      * @return integer digest of the input.
      */
    private String getDigestText(byte[] inDigest) {
        BigInteger bigIntIn = new BigInteger(1, inDigest);
        String hashtext = bigIntIn.toString(16);
        while (hashtext.length() < 32) {
            hashtext = "0" + hashtext;
        }
        return hashtext;
    }

    /**
     * Sets the given headerValue as an 'X-session' value in the response.
     *
     * @param response response object to set the header value in.
     * @param headerValue header value to set as an 'X-session' value.
     * @return The response parameter with the header value set.
     */
    private HttpServletResponse setHeaders(HttpServletResponse response, String headerValue) {
        // Set headers for the response
        String headerKey = "X-Session";
        response.setHeader(headerKey, headerValue);

        // get MIME type of the file (actually, it's always this for now)
        String mimeType = "application/octet-stream";
        log.info("MIME type: " + mimeType);

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
        String url =  "/file";
        if (fileStableIdPath.startsWith("EGAF")) { // If an ID is specified - resolve this in RES
            url += "/archive/" + fileStableIdPath;
        }

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
                    .queryParam("destinationIV", destIV)
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

    /**
     * Asks the load balancer for a file database URL.
     *
     * @return file database URL.
     */
    public String fileDatabaseURL() {
        return loadBalancer.choose("FILEDATABASE").getUri().toString();
    }

    /**
     * Writes the content length of a selected file to the reponse parameter,
     * and returns OK or UNAUTHORIZED wheather the file exists and can be
     * accessible.
     *
     * @param fileId should be "file".
     * @param accession accession id of the requested file.
     * @param request Unused.
     * @param response reponse object which will be modified with the content
     *     length of the requested file head.
     * @return httpStatus OK if the file info was accessible, and the reponse
     *     was modified, otherwise UNAUTHORIZED.
     */
    @Override
    @Cacheable(cacheNames = "fileSize", key="T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication() + #p0 + #p1 + #p2 +#p3")
    public ResponseEntity getHeadById(String fileId,
                                      String accession,
                                      HttpServletRequest request,
                                      HttpServletResponse response) {
        String localFileId = "";
        if (fileId.equalsIgnoreCase("file")) { // Currently only support File IDs
            localFileId = accession;
        }

        String sessionId= Strings.isNullOrEmpty(request.getHeader("Session-Id"))? "" : request.getHeader("Session-Id") + " ";

        // Ascertain Access Permissions for specified File ID
        File reqFile = fileInfoService.getFileInfo(localFileId, sessionId);
        if (reqFile != null) {
            response.addHeader("Content-Length", String.valueOf(reqFile.getFileSize()));
            return new ResponseEntity(HttpStatus.OK);
        }

        return new ResponseEntity(HttpStatus.UNAUTHORIZED);
    }

}
