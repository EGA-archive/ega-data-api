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
import com.google.common.io.CountingOutputStream;
import eu.elixir.ega.ebi.dataedge.config.*;
import eu.elixir.ega.ebi.dataedge.dto.*;
import eu.elixir.ega.ebi.shared.config.GeneralStreamingException;
import eu.elixir.ega.ebi.shared.config.PermissionDeniedException;
import eu.elixir.ega.ebi.shared.dto.DownloadEntry;
import eu.elixir.ega.ebi.shared.dto.EventEntry;
import eu.elixir.ega.ebi.shared.service.DownloaderLogService;
import eu.elixir.ega.ebi.shared.service.FileInfoService;
import eu.elixir.ega.ebi.dataedge.service.FileLengthService;
import eu.elixir.ega.ebi.dataedge.service.FileService;
import eu.elixir.ega.ebi.shared.dto.File;
import eu.elixir.ega.ebi.shared.dto.FileIndexFile;
import eu.elixir.ega.ebi.shared.dto.MyExternalConfig;
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
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
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

import java.util.stream.Stream;

import static eu.elixir.ega.ebi.dataedge.config.Constants.FILEDATABASE_SERVICE;
import static eu.elixir.ega.ebi.dataedge.config.Constants.RES_SERVICE;
import static org.apache.catalina.connector.OutputBuffer.DEFAULT_BUFFER_SIZE;

/**
 * @author asenf
 */
@Service
@EnableDiscoveryClient
@Slf4j
public class RemoteFileServiceImpl implements FileService {

    @Autowired
    private LoadBalancerClient loadBalancer;

    @Autowired
    private RestTemplate restTemplate;

    // Database Repositories/Services

    @Autowired
    private MyExternalConfig externalConfig;

    @Autowired
    private DownloaderLogService downloaderLogService;

    @Autowired
    private FileInfoService fileInfoService;

    @Autowired
    private FileLengthService fileLengthService;

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

        // File Archive Type - Reject GPG
        if (reqFile.getFileName().toLowerCase().endsWith("gpg")) {
            throw new NotImplementedException(sessionId + "Please contact EGA to download this file.");
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
                ResponseExtractor<HttpResult> responseExtractor = response_ -> {
                    List<String> get = response_.getHeaders().get("X-Session"); // RES session UUID
                    long b = 0;
                    String inHashtext = "";
                    try (InputStream inOrig = response_.getBody()) {
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

                            // Done - Close Streams and obtain MD5 of received Stream
                            inHashtext = getDigestText(inDigest.digest());
                        }
                    } catch (Throwable t) {
                        String errorMessage = t.toString();
                        log.error(sessionId + "RemoteFileServiceImpl Error 1: " + errorMessage);
                        throw new GeneralStreamingException(sessionId + errorMessage, 7);
                    } 

                    // return number of bytes copied, RES session header, and MD5 of RES input stream
                    return new HttpResult(b, get, inHashtext); // This is the result of the RestTemplate
                };
                // -----------------------------------------------------------------

                /*
                 * CUSTOMISATION: If you access files by absolute path (nearly everyone) then
                 * call getResUri with the file path instead of the file ID
                 * [...]getResUri(reqFile.getFileName(),destinationFormat[...]
                 */

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
            log.info(sessionId + "Get file received error");
            log.error(sessionId + "RemoteFileServiceImpl Error 2: " + t.toString());
            String errorMessage = fileId + ":" + destinationFormat + ":" + startCoordinate + ":" + endCoordinate + ":" + t.toString();
            if(errorMessage!=null && errorMessage.length() > 256) {
                errorMessage = errorMessage.substring(0,256);
            }
            EventEntry eev = downloaderLogService.createEventEntry(errorMessage,  "file");
            downloaderLogService.logEvent(eev);

            throw new GeneralStreamingException(sessionId + t.toString(), 4);
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

    /**
     * Returns the http header for a file identified by fileId. This mainly
     * includes the content length, but also a random UUID for statistics.
     *
     * @param fileId ELIXIR id of the requested file
     * @param destinationFormat Requested destination format.
     * @param request Unused.
     * @param response Response stream for the returned data.
     */
    @Override
    @Cacheable(cacheNames = "fileHead", key="T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication() + #p0 + #p1 + #p2 + #p3")
    public void getFileHead(String fileId,
                            String destinationFormat,
                            HttpServletRequest request,
                            HttpServletResponse response) {

        // Ascertain Access Permissions for specified File ID
        File reqFile = fileInfoService.getFileInfo(fileId); // request added for ELIXIR

        // Variables needed for responses at the end of the function
        if (reqFile != null) {
            // Build Header - Specify UUID (Allow later stats query regarding this transfer)
            UUID dlIdentifier = UUID.randomUUID();
            String headerValue = dlIdentifier.toString();
            response = setHeaders(response, headerValue);

            // Content Length of response (if available)
            response.setContentLengthLong(fileLengthService.getContentLength(reqFile, destinationFormat, 0, 0));
            response.addHeader("X-Content-Length", String.valueOf(fileLengthService.getContentLength(reqFile, destinationFormat, 0, 0)));
        }
    }

    /*
     * GA4GH / Semantic Functionality: Use SAMTools to access a File in Cleversafe
     */
    /**
     * Returns the SAM file header for a file identified by fileId.
     *
     * @param fileId ELIXIR id of the requested file.
     * @param destinationFormat Requested destination format.
     * @param destinationKey Encryption key that the result file will be
     *     encrypted with.
     * @param x optional CRAM reference source to be used with the
     *     SamReaderFactory.
     * @return The SAM file header for the file.
     */
    @Override
    @Cacheable(cacheNames = "headerFile", key="T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication() + #p0 + #p1 + #p2 + #p3")
    public Object getFileHeader(String fileId,
                                String destinationFormat,
                                String destinationKey,
                                CRAMReferenceSource x) {
        Object header = null;

        // Ascertain Access Permissions for specified File ID
        File reqFile = fileInfoService.getFileInfo(fileId);
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
                log.error(ex.getMessage(), ex);
            }
        }

        return header;
    }

    //Hack
    private boolean isRDConnect(File reqFile) {
        return reqFile.getDatasetId().equalsIgnoreCase("EGAD00001003952");
    }

    /**
     * Writes a requested file (or part of file), selected by accession, from
     * the FileService to the supplied response stream.
     *
     * @param fileId Should be set to 'file'.
     * @param accession Local accession ID of the requested file.
     * @param format Requested file format. Either 'bam' or 'cram' (case
     *     insensitive).
     * @param reference FASTA reference name, required for selecting a region
     *     with start and end.
     * @param start Start coordinate when requesting a partial file.
     * @param end End coordinate when requesting a partial file.
     * @param fields Data fields to include in the output file.
     * @param tags Data tags to include in the output file.
     * @param notags Data tags to exclude from the output file.
     * @param header Unused.
     * @param destinationFormat Requested destination format.
     * @param destinationKey Unused.
     * @param request Unused.
     * @param response Response stream for the returned data.
     */
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
		
		String sessionId= Strings.isNullOrEmpty(request.getHeader("Session-Id"))? "" : request.getHeader("Session-Id") + " ";		
        // Adding a content header in the response: binary data
        response.addHeader("Content-Type", MediaType.valueOf("application/octet-stream").toString());

        String localFileId = "";
        if (fileId.equalsIgnoreCase("file")) { // Currently only support File IDs
            localFileId = accession;
        }
        CountingOutputStream cOut = null;
        long timeDelta = System.currentTimeMillis();

        // Ascertain Access Permissions for specified File ID
        File reqFile = fileInfoService.getFileInfo(localFileId);
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
                URL resURL = new URL(resURL() + "/file/archive/" + reqFile.getFileId()); // Just specify file ID
                SeekableStream cIn = (new EgaSeekableCachedResStream(resURL, null, null, reqFile.getFileSize())).setExtension(extension); // Deals with coordinates
                //bIn = new SeekableBufferedStream(cIn);
                // BAI/CRAI File
                FileIndexFile fileIndexFile = getFileIndexFile(reqFile.getFileId());
                if(fileIndexFile == null || StringUtils.isEmpty(fileIndexFile.getIndexFileId())) {
                    throw new IndexNotFoundException("IndexFileId not found for file", fileId);
                }

                File reqIndexFile = fileInfoService.getFileInfo(fileIndexFile.getIndexFileId());
                URL indexUrl = new URL(resURL() + "/file/archive/" + fileIndexFile.getIndexFileId()); // Just specify index ID
                SeekableStream cIndexIn = (new EgaSeekableCachedResStream(indexUrl, null, null, reqIndexFile.getFileSize()));

                inputResource = SamInputResource.of(cIn).index(cIndexIn);
            } catch (Exception ex) {
                throw new InternalErrorException(sessionId + ex.getMessage(), "9");
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
                throw new GeneralStreamingException(sessionId + "Unknown reference: " + reference, 40);
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
                String errorMessage = t.toString();
                if(errorMessage!=null && errorMessage.length() > 256) {
                    errorMessage = errorMessage.substring(0,256);
                }
                EventEntry eev = downloaderLogService.createEventEntry(errorMessage, "GA4GH htsget Download BAM/CRAM");
                downloaderLogService.logEvent(eev);
                log.error(sessionId + "ERROR 4 " + t.toString());
                throw new GeneralStreamingException(sessionId + t.toString(), 6);
            } finally {

                timeDelta = System.currentTimeMillis() - timeDelta;
                double speed = (cOut.getCount() / 1024.0 / 1024.0) / (timeDelta / 1000.0);
                long bytes = cOut.getCount();
                boolean success = cOut.getCount() > 0;
                log.info(sessionId + "Success? " + success + ", Speed: " + speed + " MB/s");
                DownloadEntry dle = downloaderLogService.createDownloadEntry(success, speed, localFileId,
                         "htsget bam/cram", destinationFormat,
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

    /**
     * Writes a requested file (or part of file), selected by accession, from
     * the FileService to the supplied response stream.
     *
     * @param fileId Should be set to 'file'.
     * @param accession Local accession ID of the requested file.
     * @param format Unused.
     * @param reference FASTA reference name, required for selecting a region
     *     with start and end.
     * @param start Start coordinate when requesting a partial file.
     * @param end End coordinate when requesting a partial file.
     * @param fields Data fields to include in the output file.
     * @param tags Data tags to include in the output file.
     * @param notags Data tags to exclude from the output file.
     * @param header Unused.
     * @param destinationFormat Requested destination format.
     * @param destinationKey Unused.
     * @param request Unused.
     * @param response Response stream for the returned data.
     */
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
                           
		String sessionId= Strings.isNullOrEmpty(request.getHeader("Session-Id"))? "" : request.getHeader("Session-Id") + " ";
        // Adding a content header in the response: binary data
        response.addHeader("Content-Type", MediaType.valueOf("application/octet-stream").toString());

        String localFileId = "";
        if (fileId.equalsIgnoreCase("file")) { // Currently only support File IDs
            localFileId = accession;
        }

        long timeDelta = System.currentTimeMillis();
        CountingOutputStream cOut = null;

        // Ascertain Access Permissions for specified File ID
        File reqFile = fileInfoService.getFileInfo(localFileId);
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
                resURL = new URL(resURL() + "/file/archive/" + reqFile.getFileId() + vcf_ext[0]); // Just specify file ID
                FileIndexFile fileIndexFile = getFileIndexFile(reqFile.getFileId());
                if(fileIndexFile == null || StringUtils.isEmpty(fileIndexFile.getIndexFileId())) {
                    throw new IndexNotFoundException("IndexFileId not found for file", fileId);
                }

                indexURL = new URL(resURL() + "/file/archive/" + fileIndexFile.getIndexFileId() + vcf_ext[1]); // Just specify index ID

                log.info(sessionId + "Opening Reader!! ");
                // VCFFileReader with input stream based on RES URL
                reader = new MyVCFFileReader(resURL.toString(),
                        indexURL.toString(),
                        false,
                        fileDatabaseURL());
                log.info(sessionId + "Reader!! ");
            } catch (Exception ex) {
                throw new InternalErrorException(sessionId + ex.getMessage(), "19");
            } catch (Throwable th) {
                throw new InternalErrorException(sessionId + th.getMessage(), "19.1");
            }

            VCFHeader fileHeader = reader.getFileHeader();
            log.info(sessionId + "Header!! " + fileHeader.toString());

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
                throw new InternalErrorException(sessionId + ex.getMessage(), "20");
            } finally {

                timeDelta = System.currentTimeMillis() - timeDelta;
                double speed = (cOut.getCount() / 1024.0 / 1024.0) / (timeDelta / 1000.0);
                long bytes = cOut.getCount();
                boolean success = cOut.getCount() > 0;
                log.info(sessionId + "Success? " + success + ", Speed: " + speed + " MB/s");
                DownloadEntry dle = downloaderLogService.createDownloadEntry(success, speed, localFileId,
                        "htsget vcf/bcf", destinationFormat,
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

    /**
     * Create a formatted URI to request a resource from the RES micro-service.
     *
     * @param fileStableIdPath Path to the file.
     * @param destFormat Requested format (encryption type, 'plain', or
     *     'publicgpg').
     * @param destKey Encryption key that the result file will be encrypted
     *     with.
     * @param destIV Destination Initialization Vector. Needed to request part
     *     of an AES encrypted file, as the IV is otherwise part of the header.
     * @param startCoord Start coordinate of the requested file area, or 0.
     * @param endCoord End coordinate of the requested file area, or 0.
     * @return Formatted URI for the resource.
     */
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

    private String mapRunToFile(String runId) {

        // Can't access Runs yet... TODO

        return "";
    }

    /**
     * Asks the load balancer for a RES (Re-Encryption Service) URL.
     *
     * @return RES service URL.
     */
    public String resURL() {
        return loadBalancer.choose("RES2").getUri().toString();
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
     * Returns the index file for a given fileId.
     *
     * @param fileId ELIXIR id of the requested file.
     * @return The content of the index file.
     */
    @Cacheable(cacheNames = "indexFile", key="T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication() + #p0")
    private FileIndexFile getFileIndexFile(String fileId) {
        FileIndexFile indexFile = null;
        ResponseEntity<FileIndexFile[]> forEntity = restTemplate.getForEntity(FILEDATABASE_SERVICE + "/file/{fileId}/index", FileIndexFile[].class, fileId);
        FileIndexFile[] body = forEntity.getBody();
        if (body != null && body.length >= 1) {
            indexFile = body[0];
        }
        return indexFile;
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

        // Ascertain Access Permissions for specified File ID
        File reqFile = fileInfoService.getFileInfo(localFileId);
        if (reqFile != null) {
            response.addHeader("Content-Length", String.valueOf(reqFile.getFileSize()));
            return new ResponseEntity(HttpStatus.OK);
        }

        return new ResponseEntity(HttpStatus.UNAUTHORIZED);
    }

    /**
     * Filters a SAM record based on fields, tags, and excluded tags, if no
     * fields, tags or notags are provided, the original context is returned.
     *
     * @param record SAM format record to be filtered.
     * @param fields Fields to include after filtering.
     * @param tags Tags to include after filtering.
     * @param notags Tags to exclude after filtering.
     * @return The modified record.
     */
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

    /**
     * Filters a variant context based on fields, tags, and excluded tags, if no
     * fields, tags or notags are provided, the original context is returned.
     *
     * @param context The context to be filtered.
     * @param fields Fields to include after filtering.
     * @param tags Tags to include after filtering.
     * @param notags Tags to exclude after filtering.
     * @return The modified context.
     */
    private VariantContext filterMe(VariantContext context, List<String> fields, List<String> tags, List<String> notags) {
        // Default - leave record as it is
        if (fields == null && tags == null && notags == null) return context;

        Map<String, Object> attributes = context.getAttributes();

        // TODO

        return context;
    }

}
