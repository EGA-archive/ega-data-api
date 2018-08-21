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
import com.google.common.io.CountingOutputStream;
import eu.elixir.ega.ebi.dataedge.config.*;
import eu.elixir.ega.ebi.dataedge.dto.*;
import eu.elixir.ega.ebi.dataedge.service.DownloaderLogService;
import eu.elixir.ega.ebi.dataedge.service.FileService;
import htsjdk.samtools.*;
import htsjdk.samtools.SamReaderFactory.Option;
import htsjdk.samtools.cram.ref.CRAMReferenceSource;
import htsjdk.samtools.cram.ref.ReferenceSource;
import htsjdk.samtools.seekablestream.EgaSeekableCachedResStream;
import htsjdk.samtools.seekablestream.SeekableStream;
import htsjdk.samtools.util.CloseableIterator;
import htsjdk.samtools.util.CloserUtil;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.variantcontext.writer.Options;
import htsjdk.variant.variantcontext.writer.VariantContextWriter;
import htsjdk.variant.variantcontext.writer.VariantContextWriterBuilder;
import htsjdk.variant.vcf.MyVCFFileReader;
import htsjdk.variant.vcf.VCFHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static eu.elixir.ega.ebi.shared.Constants.FILEDATABASE_SERVICE;
import static eu.elixir.ega.ebi.shared.Constants.RES_SERVICE;
import static org.apache.catalina.connector.OutputBuffer.DEFAULT_BUFFER_SIZE;

/**
 * @author asenf
 */
@Profile("!LocalEGA")
@Service
@Transactional
@EnableDiscoveryClient
public class RemoteFileServiceImpl implements FileService {

    @Autowired
    private LoadBalancerClient loadBalancer;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private RetryTemplate retryTemplate;

    // Database Repositories/Services

    @Autowired
    private MyExternalConfig externalConfig;

    @Autowired
    private DownloaderLogService downloaderLogService;

    @Override
    //@HystrixCommand
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

        // File Archive Type - Reject GPG
        if (reqFile.getFileName().toLowerCase().endsWith("gpg")) {
            throw new NotImplementedException("Please contact EGA to download this file.");
        }

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
        response.setContentLengthLong(getContentLength(reqFile, destinationFormat, startCoordinate, endCoordinate));

        // If byte range, set response 206
        long fileLength = reqFile.getFileSize();
        if (destinationFormat.equalsIgnoreCase("plain")) fileLength -= 16;
        if (startCoordinate > 0 || endCoordinate > 0) {
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            response.addHeader("Content-Range", "bytes " + startCoordinate +
                    "-" + (endCoordinate - 1) + "/" + fileLength);
//System.out.println(" ^^^^^^^^^^^ setting buffer: " + (endCoordinate - startCoordinate));
//                if (endCoordinate - startCoordinate < Integer.MAX_VALUE)
//                    response.setBufferSize((int) (endCoordinate - startCoordinate));
            response.setBufferSize(DEFAULT_BUFFER_SIZE);
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
                    // If the stream is encrypted, and coordinates are specified,
                    // there is a possibility that 0-15 extra bytes are sent, because
                    // of the 16-byte AES Block size - read these bytes before moving on
                    InputStream inOrig = response_.getBody();
                    if (destinationFormat.toLowerCase().startsWith("aes") &&
                            destinationIV != null && destinationIV.length() > 0) {
                        long blockStart = (startCoordinate / 16) * 16;
                        int blockDelta = (int) (startCoordinate - blockStart);
                        if (blockDelta > 0)
                            inOrig.read(new byte[blockDelta]);
                    }

                    // Input stream from RES, wrap in DigestStream
                    MessageDigest inDigest = MessageDigest.getInstance("MD5");
                    //DigestInputStream inDigestStream = new DigestInputStream(response_.getBody(), inDigest);
                    DigestInputStream inDigestStream = new DigestInputStream(inOrig, inDigest);

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
            int cnt = 2;
            do {
                xferResult = restTemplate.execute(getResUri(fileId, destinationFormat, destinationKey, destinationIV, startCoordinate, endCoordinate), HttpMethod.GET, requestCallback, responseExtractor);
            } while (xferResult.getBytes() <= 0 && cnt-- > 0);
            timeDelta = System.currentTimeMillis() - timeDelta;

        } catch (Throwable t) { // Log Error!
            System.out.println("RemoteFileServiceImpl Error 2: " + t.toString());
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
    //@HystrixCommand
    @Cacheable(cacheNames = "fileHead")
    public void getFileHead(Authentication auth,
                            String fileId,
                            String destinationFormat,
                            HttpServletRequest request,
                            HttpServletResponse response) {

        // Ascertain Access Permissions for specified File ID
        File reqFile = getReqFile(fileId, auth, request); // request added for ELIXIR

        // Variables needed for responses at the end of the function
        if (reqFile != null) {
            // Build Header - Specify UUID (Allow later stats query regarding this transfer)
            UUID dlIdentifier = UUID.randomUUID();
            String headerValue = dlIdentifier.toString();
            response = setHeaders(response, headerValue);

            // Content Length of response (if available)
            response.setContentLengthLong(getContentLength(reqFile, destinationFormat, 0, 0));
            response.addHeader("X-Content-Length", String.valueOf(getContentLength(reqFile, destinationFormat, 0, 0)));
        }
    }

    /*
     * GA4GH / Semantic Functionality: Use SAMTools to access a File in Cleversafe
     */

    @Override
    //@HystrixCommand
    @Cacheable(cacheNames = "headerFile")
    public Object getFileHeader(Authentication auth,
                                String fileId,
                                String destinationFormat,
                                String destinationKey,
                                CRAMReferenceSource x) {
        Object header = null;

        // Ascertain Access Permissions for specified File ID
        File reqFile = getReqFile(fileId, auth, null);
        if (reqFile != null) {
            URL resURL;
            try {
                resURL = new URL(resURL() + "/file/archive/" + reqFile.getFileId()); // Just specify file ID
                SeekableStream cIn = new EgaSeekableCachedResStream(resURL, null, null, reqFile.getFileSize()); // Deals with coordinates
                SamReader reader = (x == null) ?
                        (SamReaderFactory.make()            // BAM File
                                .validationStringency(ValidationStringency.LENIENT)
                                .enable(Option.CACHE_FILE_BASED_INDEXES)
                                .samRecordFactory(DefaultSAMRecordFactory.getInstance())
                                .open(SamInputResource.of(cIn))) :
                        (SamReaderFactory.make()            // CRAM File
                                .referenceSource(x)
                                .validationStringency(ValidationStringency.LENIENT)
                                .enable(Option.CACHE_FILE_BASED_INDEXES)
                                .samRecordFactory(DefaultSAMRecordFactory.getInstance())
                                .open(SamInputResource.of(cIn)));
                header = reader.getFileHeader();
                reader.close();
            } catch (IOException ex) {
                Logger.getLogger(RemoteFileServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return header;
    }

    //Hack
    private boolean isRDConnect(File reqFile) {
        return reqFile.getDatasetId().equalsIgnoreCase("EGAD00001003952");
    }

    @Override
    //@HystrixCommand
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

        // Adding a content header in the response: binary data
        response.addHeader("Content-Type", MediaType.valueOf("application/octet-stream").toString());

        // CLient IP
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }

        String localFileId = "";
        if (fileId.equalsIgnoreCase("file")) { // Currently only support File IDs
            localFileId = accession;
        }
        CountingOutputStream cOut = null;
        long timeDelta = System.currentTimeMillis();

        // Ascertain Access Permissions for specified File ID
        File reqFile = getReqFile(localFileId, auth, request);
        if (reqFile != null) {

            // SeekableStream on top of RES (using Eureka to obtain RES Base URL)
            SamInputResource inputResource;
            CRAMReferenceSource x = null;
            //SeekableBufferedStream bIn = null, 
            //                       bIndexIn = null;
            try {
                String extension = "";
                if (reqFile.getFileName().contains(".bam")) {
                    extension = ".bam";
                } else if (reqFile.getFileName().contains(".cram")) {
                    extension = ".cram";

                    // hack: differentiate between two file sources
                    //x = new ReferenceSource(new java.io.File(externalConfig.getCramFastaReferenceA()));
                    x = (isRDConnect(reqFile)) ?
                            new ReferenceSource(new java.io.File(externalConfig.getCramFastaReferenceB())) :
                            new ReferenceSource(new java.io.File(externalConfig.getCramFastaReferenceA()));
                }

                // BAM/CRAM File
                URL resURL = new URL(resURL() + "file/archive/" + reqFile.getFileId()); // Just specify file ID
                SeekableStream cIn = (new EgaSeekableCachedResStream(resURL, null, null, reqFile.getFileSize())).setExtension(extension); // Deals with coordinates
                //bIn = new SeekableBufferedStream(cIn);
                // BAI/CRAI File
                FileIndexFile fileIndexFile = getFileIndexFile(reqFile.getFileId());
                File reqIndexFile = getReqFile(fileIndexFile.getIndexFileId(), auth, null);
                URL indexUrl = new URL(resURL() + "file/archive/" + fileIndexFile.getIndexFileId()); // Just specify index ID
                SeekableStream cIndexIn = (new EgaSeekableCachedResStream(indexUrl, null, null, reqIndexFile.getFileSize()));

                inputResource = SamInputResource.of(cIn).index(cIndexIn);
            } catch (Exception ex) {
                throw new InternalErrorException(ex.getMessage(), "9");
            }

            // SamReader with input stream based on RES URL (should work for BAM or CRAM)
            SamReader reader = (x == null) ?
                    (SamReaderFactory.make()            // BAM File
                            .validationStringency(ValidationStringency.LENIENT)
                            .enable(Option.CACHE_FILE_BASED_INDEXES)
                            .samRecordFactory(DefaultSAMRecordFactory.getInstance())
                            .open(inputResource)) :
                    (SamReaderFactory.make()            // CRAM File
                            .referenceSource(x)
                            .validationStringency(ValidationStringency.LENIENT)
                            .enable(Option.CACHE_FILE_BASED_INDEXES)
                            .samRecordFactory(DefaultSAMRecordFactory.getInstance())
                            .open(inputResource));

            SAMFileHeader fileHeader = reader.getFileHeader();
            int iIndex = fileHeader.getSequenceIndex(reference);

            // Handle Request here - query Reader according to parameters
            int iStart = (int) (start);
            int iEnd = (int) (end);
            SAMRecordIterator query;
            if (iIndex > -1) { // ref was specified
                query = reader.queryOverlapping(reference, iStart, iEnd);
            } else if ((reference == null || reference.isEmpty()) && iIndex == -1) {
                throw new GeneralStreamingException("Unknown reference: " + reference, 40);
            } else { // no ref - ignore start/end
                query = reader.iterator();
            }

            // Open return output stream - instatiate a SamFileWriter
            OutputStream out = null;
            SAMFileWriterFactory writerFactory = new SAMFileWriterFactory();
            if (query != null) try {
                cOut = new CountingOutputStream(response.getOutputStream());
                out = cOut;
                //out = response.getOutputStream();
                if (format.equalsIgnoreCase("BAM")) {
                    try (SAMFileWriter writer = writerFactory.makeBAMWriter(fileHeader, true, out)) { // writes out header
                        Stream<SAMRecord> stream = query.stream();
                        Iterator<SAMRecord> iterator = stream.iterator();
                        while (iterator.hasNext()) {
                            SAMRecord next = filterMe(iterator.next(), tags, notags, fields);
                            writer.addAlignment(next);
                        }
                    }
                } else if (format.equalsIgnoreCase("CRAM")) { // Must specify Reference fasta file
                    // Decide on Reference
                    String refFPath = (isRDConnect(reqFile)) ?
                            externalConfig.getCramFastaReferenceB() :
                            externalConfig.getCramFastaReferenceA();

                    try (CRAMFileWriter writer = writerFactory
                            .makeCRAMWriter(fileHeader, out, new java.io.File(refFPath))) {
                        Stream<SAMRecord> stream = query.stream();
                        Iterator<SAMRecord> iterator = stream.iterator();
                        while (iterator.hasNext()) {
                            SAMRecord next = iterator.next();
                            writer.addAlignment(next);
                        }
                    }
                }

            } catch (Throwable t) { // Log Error!
                EventEntry eev = getEventEntry(t.toString(), ipAddress, "GA4GH htsget Download BAM/CRAM", auth.getName());
                downloaderLogService.logEvent(eev);
                System.out.println("ERROR 4 " + t.toString());
                throw new GeneralStreamingException(t.toString(), 6);
            } finally {

                timeDelta = System.currentTimeMillis() - timeDelta;
                double speed = (cOut.getCount() / 1024.0 / 1024.0) / (timeDelta / 1000.0);
                long bytes = cOut.getCount();
                boolean success = cOut.getCount() > 0;
                String user_email = auth.getName(); // For Logging
                System.out.println("Success? " + success + ", Speed: " + speed + " MB/s");
                DownloadEntry dle = getDownloadEntry(success, speed, localFileId,
                        ipAddress, "htsget bam/cram", user_email, destinationFormat,
                        start, end, bytes);
                downloaderLogService.logDownload(dle);

                if (out != null) try {
                    out.close();
                } catch (IOException ex) {
                    ;
                }
            }
        } else { // If no 404 was found, this is a permissions denied error
            throw new PermissionDeniedException(accession);
        }
    }

    @Override
    //@HystrixCommand
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

        // Adding a content header in the response: binary data
        response.addHeader("Content-Type", MediaType.valueOf("application/octet-stream").toString());

        // CLient IP
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }

        String localFileId = "";
        if (fileId.equalsIgnoreCase("file")) { // Currently only support File IDs
            localFileId = accession;
        }

        long timeDelta = System.currentTimeMillis();
        CountingOutputStream cOut = null;

        // Ascertain Access Permissions for specified File ID
        File reqFile = getReqFile(localFileId, auth, request);
        if (reqFile != null) {
            URL resURL, indexURL;
            MyVCFFileReader reader;

            try {
                String[] vcf_ext = {"?destinationFormat=plain&extension=.vcf", "?destinationFormat=plain&extension=.vcf.tbi"};
                if (reqFile.getFileName().toLowerCase().endsWith(".gz") ||
                        reqFile.getFileName().toLowerCase().endsWith(".gz.cip")) {
                    vcf_ext[0] += ".gz";
                    vcf_ext[1] = "?destinationFormat=plain&extension=.vcf.gz.tbi";
                }

                // VCF File
                resURL = new URL(resURL() + "file/archive/" + reqFile.getFileId() + vcf_ext[0]); // Just specify file ID
                FileIndexFile fileIndexFile = getFileIndexFile(reqFile.getFileId());
                indexURL = new URL(resURL() + "file/archive/" + fileIndexFile.getIndexFileId() + vcf_ext[1]); // Just specify index ID

                System.out.println("Opening Reader!! ");
                // VCFFileReader with input stream based on RES URL
                reader = new MyVCFFileReader(resURL.toString(),
                        indexURL.toString(),
                        false,
                        fileDatabaseURL());
                System.out.println("Reader!! ");
            } catch (Exception ex) {
                throw new InternalErrorException(ex.getMessage(), "19");
            } catch (Throwable th) {
                throw new InternalErrorException(th.getMessage(), "19.1");
            }

            VCFHeader fileHeader = reader.getFileHeader();
            System.out.println("Header!! " + fileHeader.toString());

            // Handle Request here - query Reader according to parameters
            int iStart = (int) (start);
            int iEnd = (int) (end);
            CloseableIterator<VariantContext> query;
            if (iEnd > 0 && iEnd >= iStart && iStart > 0 && reference != null && reference.length() > 0) { // ref was specified
                query = reader.query(reference, iStart, iEnd);
            } else { // no ref - ignore start/end
                query = reader.iterator();
            }

            // Open return output stream - instatiate a SamFileWriter
            OutputStream out;
            try {
                cOut = new CountingOutputStream(response.getOutputStream());
                out = cOut;
                //out = response.getOutputStream();

                VariantContextWriterBuilder builder = new VariantContextWriterBuilder().
                        setOutputVCFStream(out).
                        setReferenceDictionary(fileHeader.getSequenceDictionary()).
                        unsetOption(Options.INDEX_ON_THE_FLY);

                final VariantContextWriter writer = builder.build();
                writer.writeHeader(fileHeader);

                while (query.hasNext()) {
                    VariantContext context = filterMe(query.next(), tags, notags, fields);
                    writer.add(context);
                }

                CloserUtil.close(query);
                CloserUtil.close(reader);

                writer.close();
            } catch (IOException ex) {
                throw new InternalErrorException(ex.getMessage(), "20");
            } finally {

                timeDelta = System.currentTimeMillis() - timeDelta;
                double speed = (cOut.getCount() / 1024.0 / 1024.0) / (timeDelta / 1000.0);
                long bytes = cOut.getCount();
                boolean success = cOut.getCount() > 0;
                String user_email = auth.getName(); // For Logging
                System.out.println("Success? " + success + ", Speed: " + speed + " MB/s");
                DownloadEntry dle = getDownloadEntry(success, speed, localFileId,
                        ipAddress, "htsget vcf/bcf", user_email, destinationFormat,
                        start, end, bytes);
                downloaderLogService.logDownload(dle);
            }


        } else { // If no 404 was found, this is a permissions denied error
            throw new PermissionDeniedException(accession);
        }
    }

    /*
     * Helper Functions
     */
    //@HystrixCommand
    private String getDigestText(byte[] inDigest) {
        BigInteger bigIntIn = new BigInteger(1, inDigest);
        String hashtext = bigIntIn.toString(16);
        while (hashtext.length() < 32) {
            hashtext = "0" + hashtext;
        }
        return hashtext;
    }

    //@HystrixCommand
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

    //@HystrixCommand
    private URI getResUri(String fileStableIdPath,
                          String destFormat,
                          String destKey,
                          String destIV,
                          Long startCoord,
                          Long endCoord) {
        destFormat = destFormat.equals("AES") ? "aes128" : destFormat; // default to 128-bit if not specified
        String url = RES_SERVICE + "/file";
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

    //@HystrixCommand
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

    //@HystrixCommand
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

    //@HystrixCommand
    @Cacheable(cacheNames = "reqFile")
    private File getReqFile(String fileId, Authentication auth, HttpServletRequest request) {

        // Obtain all Authorised Datasets (Provided by EGA AAI)
        HashSet<String> permissions = new HashSet<>();
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        if (authorities != null && authorities.size() > 0) {
            Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
            while (iterator.hasNext()) {
                GrantedAuthority next = iterator.next();
                permissions.add(next.getAuthority());
            }
        } else if (request != null) { // ELIXIR User Case: Obtain Permmissions from X-Permissions Header
            //String permissions = request.getHeader("X-Permissions");
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
                //}
                //
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

        ResponseEntity<FileDataset[]> forEntityDataset = restTemplate.getForEntity(FILEDATABASE_SERVICE + "/file/{fileId}/datasets", FileDataset[].class, fileId);
        FileDataset[] bodyDataset = forEntityDataset.getBody();

        File reqFile = null;
        ResponseEntity<File[]> forEntity = restTemplate.getForEntity(FILEDATABASE_SERVICE + "/file/{fileId}", File[].class, fileId);
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
                    ResponseEntity<Long> forSize = restTemplate.getForEntity(RES_SERVICE + "/file/archive/{fileId}/size", Long.class, fileId);
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

    //@HystrixCommand
    private String mapRunToFile(String runId) {

        // Can't access Runs yet... TODO

        return "";
    }

    //@HystrixCommand
    public String resURL() {
        return loadBalancer.choose("RES2").getUri().toString();
    }

    //@HystrixCommand
    public String fileDatabaseURL() {
        return loadBalancer.choose("FILEDATABASE2").getUri().toString();
    }

    //@HystrixCommand
    @Cacheable(cacheNames = "indexFile")
    private FileIndexFile getFileIndexFile(String fileId) {
        FileIndexFile indexFile = null;
        ResponseEntity<FileIndexFile[]> forEntity = restTemplate.getForEntity(FILEDATABASE_SERVICE + "/file/{fileId}/index", FileIndexFile[].class, fileId);
        FileIndexFile[] body = forEntity.getBody();
        if (body != null && body.length >= 1) {
            indexFile = body[0];
        }
        return indexFile;
    }

    @Override
    //@HystrixCommand
    @Cacheable(cacheNames = "fileSize")
    public ResponseEntity getHeadById(Authentication auth,
                                      String fileId,
                                      String accession,
                                      HttpServletRequest request,
                                      HttpServletResponse response) {
        String localFileId = "";
        if (fileId.equalsIgnoreCase("file")) { // Currently only support File IDs
            localFileId = accession;
        }

        // Ascertain Access Permissions for specified File ID
        File reqFile = getReqFile(localFileId, auth, null);
        if (reqFile != null) {
            response.addHeader("Content-Length", String.valueOf(reqFile.getFileSize()));
            return new ResponseEntity(HttpStatus.OK);
        }

        return new ResponseEntity(HttpStatus.UNAUTHORIZED);
    }

    private SAMRecord filterMe(SAMRecord record, List<String> fields, List<String> tags, List<String> notags) {
        // Default - leave record as it is
        if (fields == null && tags == null && notags == null) return record;

        List<SAMRecord.SAMTagAndValue> attributes = record.getAttributes();
        record.clearAttributes();

        // If tags is specified, without listing any, remove all tags.
        if (tags != null && tags.size() == 0) return record;

        // If specific tags are specified
        Iterator<SAMRecord.SAMTagAndValue> iterator = attributes.iterator();
        while (iterator.hasNext()) {
            SAMRecord.SAMTagAndValue nextTag = iterator.next();
            if ((tags != null && tags.contains(nextTag.tag)) ||
                    (notags != null && !notags.contains(nextTag.tag))) {
                record.setAttribute(nextTag.tag, nextTag.value);
            } else if ((tags != null && tags.contains(nextTag.tag)) ||
                    (notags != null && !notags.contains(nextTag.tag))) {
                throw new GeneralStreamingException("Tag value specified in tags and notags: " + nextTag.tag, 80);
            }
        }

        // Is specific fields are specified
        // TODO

        return record;
    }

    private VariantContext filterMe(VariantContext context, List<String> fields, List<String> tags, List<String> notags) {
        // Default - leave record as it is
        if (fields == null && tags == null && notags == null) return context;

        Map<String, Object> attributes = context.getAttributes();

        // TODO  

        return context;
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
