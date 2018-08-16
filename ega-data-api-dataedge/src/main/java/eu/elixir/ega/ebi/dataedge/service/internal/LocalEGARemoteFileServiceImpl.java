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
import eu.elixir.ega.ebi.dataedge.config.NotFoundException;
import eu.elixir.ega.ebi.dataedge.config.PermissionDeniedException;
import eu.elixir.ega.ebi.dataedge.config.VerifyMessageNew;
import eu.elixir.ega.ebi.dataedge.dto.*;
import eu.elixir.ega.ebi.dataedge.service.DownloaderLogService;
import eu.elixir.ega.ebi.dataedge.service.FileService;
import htsjdk.samtools.cram.ref.CRAMReferenceSource;
import org.apache.commons.lang.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

@Profile("LocalEGA")
@Service
@Transactional
@EnableDiscoveryClient
public class LocalEGARemoteFileServiceImpl implements FileService {

    private static final String SERVICE_URL = "http://FILEDATABASE";
    private static final String RES_URL = "http://RES2";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private DownloaderLogService downloaderLogService;

    @Override
    public void getFile(Authentication auth,
                        String fileId,
                        String destinationFormat,
                        String destinationKey,
                        String destinationIV,
                        long startCoordinate,
                        long endCoordinate,
                        HttpServletRequest request,
                        HttpServletResponse response) {

        // Ascertain Access Permissions for specified File ID
        File reqFile = getReqFile(fileId, auth, request); // request added for ELIXIR
        if (reqFile == null) {
            try {
                Thread.sleep(2500);
            } catch (InterruptedException ignored) {
            }
            reqFile = getReqFile(fileId, auth, request);
        }
        if (reqFile.getFileSize() > 0 && endCoordinate > reqFile.getFileSize())
            endCoordinate = reqFile.getFileSize();

        // CLient IP
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }

        String user_email = auth.getName(); // For Logging

        // Variables needed for responses at the end of the function
        long timeDelta = 0;
        HttpResult xferResult = null;
        MessageDigest outDigest = null;

        // Log request in Event
        //    EventEntry eev_received = getEventEntry(file_id + ":" + destinationFormat + ":" + startCoordinate + ":" + endCoordinate,
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
            EventEntry eev = getEventEntry(errorMessage, ipAddress, "file", user_email);
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
                DownloadEntry dle = getDownloadEntry(success, speed, fileId,
                        ipAddress, "file", user_email, destinationFormat,
                        startCoordinate, endCoordinate, bytes);
                downloaderLogService.logDownload(dle);
            }
        }
    }

    @Override
    @Cacheable(cacheNames = "fileHead")
    public void getFileHead(Authentication auth,
                            String fileId,
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
    public Object getFileHeader(Authentication auth,
                                String fileId,
                                String destinationFormat,
                                String destinationKey,
                                CRAMReferenceSource x) {
        throw new NotImplementedException();
    }

    @Override
    public void getById(Authentication auth,
                        String fileId,
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
    public void getVCFById(Authentication auth,
                           String fileId,
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
        String url = RES_URL + "/file";

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

    private DownloadEntry getDownloadEntry(boolean success, double speed, String fileId,
                                           String clientIp,
                                           String server,
                                           String email,
                                           String encryptionType,
                                           long startCoordinate,
                                           long endCoordinate,
                                           long bytes) {
        DownloadEntry dle = new DownloadEntry();
        dle.setDownloadLogId(0L);
        dle.setDownloadSpeed(speed);
        dle.setDownloadStatus(success ? "success" : "failed");
        dle.setFileId(fileId);
        dle.setClientIp(clientIp);
        dle.setEmail(email);
        dle.setApi(server);
        dle.setEncryptionType(encryptionType);
        dle.setStartCoordinate(startCoordinate);
        dle.setEndCoordinate(endCoordinate);
        dle.setBytes(bytes);
        dle.setCreated(new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()));
        dle.setTokenSource("EGA");

        return dle;
    }

    private EventEntry getEventEntry(String t, String clientIp,
                                     String ticket,
                                     String email) {
        EventEntry eev = new EventEntry();
        eev.setEventId("0");
        eev.setClientIp(clientIp);
        eev.setEvent(t);
        eev.setEventType("Error");
        eev.setEmail(email);
        eev.setCreated(new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()));

        return eev;
    }

    @Cacheable(cacheNames = "reqFile")
    private File getReqFile(String fileId, Authentication auth, HttpServletRequest request) {

        // Obtain all Authorised Datasets (Provided by EGA AAI)
        HashSet<String> permissions = new HashSet<>();
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        if (authorities != null && authorities.size() > 0) {
            for (GrantedAuthority next : authorities) {
                permissions.add(next.getAuthority());
            }
        } else if (request != null) { // ELIXIR User Case: Obtain Permmissions from X-Permissions Header
            try {
                List<String> permissions_ = (new VerifyMessageNew(request.getHeader("X-Permissions"))).getPermissions();
                if (permissions_ != null && permissions_.size() > 0) {
                    //StringTokenizer t = new StringTokenizer(permissions, ",");
                    //while (t!=null && t.hasMoreTokens()) {
                    for (String ds : permissions_) {
                        //String ds = t.nextToken();
                        if (ds != null && ds.length() > 0) permissions.add(ds);
                    }
                }
            } catch (Exception ex) {
                //try {
                //    List<String> permissions_ = (new VerifyMessage(request.getHeader("X-Permissions"))).getPermissions();
                //    if (permissions_ != null && permissions_.size() > 0) {
                //        for (String ds : permissions_) {
                //            if (ds != null) {
                //                permissions.add(ds);
                //            }
                //        }
                //    }
                //} catch (Exception ex) {
                String ipAddress = request.getHeader("X-FORWARDED-FOR");
                if (ipAddress == null) {
                    ipAddress = request.getRemoteAddr();
                }
                String user_email = auth.getName(); // For Logging

                System.out.println("getReqFile Error 0: " + ex.toString());
                EventEntry eev = getEventEntry(ex.toString(), ipAddress, "file", user_email);
                downloaderLogService.logEvent(eev);

                throw new GeneralStreamingException(ex.toString(), 0);
            }
        }

        ResponseEntity<FileDataset[]> forEntityDataset = restTemplate.getForEntity(SERVICE_URL + "/file/{fileId}/datasets", FileDataset[].class, fileId);
        FileDataset[] bodyDataset = forEntityDataset.getBody();

        File reqFile = null;
        ResponseEntity<File[]> forEntity = restTemplate.getForEntity(SERVICE_URL + "/file/{fileId}", File[].class, fileId);
        File[] body = forEntity.getBody();
        if (body != null && bodyDataset != null) {
            for (FileDataset f : bodyDataset) {
                String datasetId = f.getDatasetId();
                if (permissions.contains(datasetId) && body.length >= 1) {
                    reqFile = body[0];
                    reqFile.setDatasetId(datasetId);
                    break;
                }
            }

            if (reqFile != null) {
                // If there's no file size in the database, obtain it from RES
                if (reqFile.getFileSize() == 0) {
                    ResponseEntity<Long> forSize = restTemplate.getForEntity(RES_URL + "/file/archive/{fileId}/size", Long.class, fileId);
                    reqFile.setFileSize(forSize.getBody());
                }
            } else { // 403 Unauthorized
                throw new PermissionDeniedException(HttpStatus.UNAUTHORIZED.toString());
            }
        } else { // 404 File Not Found
            throw new NotFoundException(fileId, "4");
        }
        return reqFile;
    }

    @Override
    @Cacheable(cacheNames = "fileSize")
    public ResponseEntity getHeadById(Authentication auth,
                                      String fileId,
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
