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

/*
 * Implementation for S3 Interface at the EGA-EBI
 *
 * Load archive data as Cache Pages, keep unencrypted data in cache memory
 * Serve data requests from cache only
 */
package eu.elixir.ega.ebi.commons.shared.service.internal;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Iterator;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.KeyFingerPrintCalculator;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.cache2k.Cache;
import org.springframework.cache.annotation.Cacheable;

import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;

import eu.elixir.ega.ebi.commons.cache2k.My2KCachePageFactory;
import eu.elixir.ega.ebi.commons.exception.GeneralStreamingException;
import eu.elixir.ega.ebi.commons.exception.ServerErrorException;
import eu.elixir.ega.ebi.commons.shared.dto.DownloadEntry;
import eu.elixir.ega.ebi.commons.shared.dto.EgaAESFileHeader;
import eu.elixir.ega.ebi.commons.shared.dto.EventEntry;
import eu.elixir.ega.ebi.commons.shared.dto.File;
import eu.elixir.ega.ebi.commons.shared.dto.KeyPath;
import eu.elixir.ega.ebi.commons.shared.service.DownloaderLogService;
import eu.elixir.ega.ebi.commons.shared.service.FileInfoService;
import eu.elixir.ega.ebi.commons.shared.service.FileLengthService;
import eu.elixir.ega.ebi.commons.shared.service.KeyService;
import eu.elixir.ega.ebi.commons.shared.service.ResService;
import eu.elixir.ega.ebi.commons.shared.util.DecryptionUtils;
import eu.elixir.ega.ebi.commons.shared.util.FireCommons;
import eu.elixir.ega.ebi.commons.shared.util.Glue;
import eu.elixir.ega.ebi.commons.shared.util.S3Commons;
import lombok.extern.slf4j.Slf4j;


/**
 * @author asenf
 */
@Slf4j
public class CacheResServiceImpl implements ResService {

    /**
     * Size of a byte buffer to read/write file (for Random Stream)
     */
    //private static final int BUFFER_SIZE = 16;
    private static final long BUFFER_SIZE = 1024 * 1024 * 12;
    /**
     * Bouncy Castle code for Public Key encrypted Files
     */
    private static final KeyFingerPrintCalculator fingerPrintCalculater = new BcKeyFingerprintCalculator();
    /**
     * Background processing
     */
    private KeyService keyService;
    private Cache<String, EgaAESFileHeader> myHeaderCache;
    private My2KCachePageFactory pageDowloader;
    private FireCommons fireCommons;
    private S3Commons s3Commons;
    private CloseableHttpClient httpClient;
    private FileInfoService fileInfoService;
    private FileLengthService fileLengthService;
    private DownloaderLogService downloaderLogService;
    
    public CacheResServiceImpl(KeyService keyService, Cache<String, EgaAESFileHeader> myHeaderCache,
                               My2KCachePageFactory pageDowloader, FireCommons fireCommons, S3Commons s3Commons,
                               CloseableHttpClient httpClient, FileInfoService fileInfoService, FileLengthService fileLengthService,
                               DownloaderLogService downloaderLogService) {
        this.keyService = keyService;
        this.myHeaderCache = myHeaderCache;
        this.pageDowloader = pageDowloader;
        this.fireCommons = fireCommons;
        this.s3Commons = s3Commons;
        this.httpClient = httpClient;
        this.fileInfoService = fileInfoService;
        this.fileLengthService = fileLengthService;
        this.downloaderLogService = downloaderLogService;
    }

    /*
     * Perform Data Transfer Requested by File Controller
     */

    private static PGPPublicKeyRing getKeyring(InputStream keyBlockStream) throws IOException {
        // PGPUtil.getDecoderStream() will detect ASCII-armor automatically and decode it,
        // the PGPObject factory then knows how to read all the data in the encoded stream
        PGPObjectFactory factory = new PGPObjectFactory(PGPUtil.getDecoderStream(keyBlockStream), fingerPrintCalculater);

        // these files should really just have one object in them,
        // and that object should be a PGPPublicKeyRing.
        Object o = factory.nextObject();
        if (o instanceof PGPPublicKeyRing) {
            return (PGPPublicKeyRing) o;
        }
        throw new IllegalArgumentException("Input text does not contain a PGP Public Key");
    }

    // -------------------------------------------------------------------------
    private static PGPPublicKey getEncryptionKey(PGPPublicKeyRing keyRing) {
        if (keyRing == null)
            return null;

        // iterate over the keys on the ring, look for one
        // which is suitable for encryption.
        Iterator keys = keyRing.getPublicKeys();
        PGPPublicKey key = null;
        while (keys.hasNext()) {
            key = (PGPPublicKey) keys.next();
            if (key.isEncryptionKey()) {
                return key;
            }
        }
        return null;
    }

    @Override
    public long transfer(String sourceFormat,
                         String sourceKey,
                         String sourceIV,
                         String destinationFormat,
                         String destinationKey,
                         String destinationIV,  // Base64 encoded
                         String fileLocation,
                         long startCoordinate,
                         long endCoordinate,
                         long fileSize,
                         String httpAuth,   // null / "" with new storage back end
                         String id,
                         HttpServletRequest request,
                         HttpServletResponse response) {

        String sessionId = Strings.isNullOrEmpty(request.getHeader("Session-Id")) ? "" : request.getHeader("Session-Id") + " ";
        long timeDelta = System.currentTimeMillis();

        // Check if File Header is in Cache - otherwise Load it
        if (!myHeaderCache.containsKey(id))
            loadHeaderCleversafe(id, fileLocation, httpAuth, fileSize, request, response, sourceKey);

        // Streams and Digests for this data transfer
        OutputStream outStream = null;
        MessageDigest encryptedDigest = null;
        DigestOutputStream encryptedDigestOut = null;
        OutputStream eOut = null;
        InputStream in = null;

        // get MIME type of the file (actually, it's always this for now)
        String mimeType = "application/octet-stream";

        // set content attributes for the response
        response.setContentType(mimeType);

        long bytesToTransfer = fileSize - startCoordinate - (endCoordinate > 0 ? (fileSize - endCoordinate) : 0);
        long bytesTransferred = 0;
        
        int errorLocation = 0;
        try {
            // Get Send Stream - http Response, wrap in Digest Stream
            outStream = response.getOutputStream();
            encryptedDigest = MessageDigest.getInstance("MD5");
            encryptedDigestOut = new DigestOutputStream(outStream, encryptedDigest);

            // Generate Encrypting OutputStream
            eOut = getTarget(encryptedDigestOut,
                    destinationFormat,
                    destinationKey,
                    destinationIV,
                    startCoordinate);
            errorLocation = 1;
            if (eOut == null) {
                throw new GeneralStreamingException(sessionId + "Output Stream (ReEncryption Stage) Null", 2);
            }

            // Transfer Loop - Get data from Cache, write it - until done!
            if (sourceFormat.equalsIgnoreCase("aes128") || sourceFormat.equalsIgnoreCase("aes256"))
                fileSize = fileSize - 16; // Adjust CIP File Size (subtracting 16 bytes)
            
            if (endCoordinate > fileSize)
                endCoordinate = fileSize;
            // Adjust start coordinate requested - to match 16 byte block structure
            if (destinationFormat.toLowerCase().startsWith("aes") &&
                    destinationIV != null && destinationIV.length() > 0) {
                long blockStart = (startCoordinate / 16) * 16;
                int blockDelta = (int) (startCoordinate - blockStart);
                startCoordinate -= blockDelta;
            }
            bytesToTransfer = fileSize - startCoordinate - (endCoordinate > 0 ? (fileSize - endCoordinate) : 0);
            errorLocation = 2;

            int startPage = (int) (startCoordinate / BUFFER_SIZE);
            int pageOffset = (int) (startCoordinate - ((long) startPage * (long) BUFFER_SIZE));

            while (bytesTransferred < bytesToTransfer) {
                errorLocation = 3;

                byte[] page = pageDowloader.downloadPage(id, startPage);
                errorLocation = 4;
                if (page == null)
                    throw new GeneralStreamingException(sessionId + " Error getting page id '" + id + "' page '"
                            + startPage + "'");

                ByteArrayInputStream bais = new ByteArrayInputStream(page);
                bais.skip(pageOffset); // first cache page

                // At this point the plain data is in a cache page

                long delta = bytesToTransfer - bytesTransferred;
                if (delta < page.length) {
                    in = ByteStreams.limit(bais, delta);
                } else {
                    in = bais;
                }
                pageOffset = 0;

                // Copy the specified contents - decrypting through input, encrypting through output
                long bytes = ByteStreams.copy(in, eOut);
                errorLocation = 6;
                bytesTransferred += bytes;
                startPage += 1;
            }
            
            timeDelta = System.currentTimeMillis() - timeDelta;
            return bytesTransferred;
        } catch (Exception t) {
            log.error(sessionId.concat(t.getMessage()) , t);
            String errorMessage = id + ":" + destinationFormat + ":" + startCoordinate + ":" + endCoordinate + ":" + t.toString();
            if (errorMessage != null && errorMessage.length() > 256) {
                errorMessage = errorMessage.substring(0, 256);
            }
            
            EventEntry eev = downloaderLogService.createEventEntry(errorMessage, "file");
            downloaderLogService.logEvent(eev);
            throw new GeneralStreamingException(sessionId +" "+ t.toString(), 4);
        } finally {
            
            if (bytesToTransfer == bytesTransferred) {

                // Compare - Sent MD5 equals Received MD5? - Log Download in DB
                double speed = (bytesTransferred / 1024.0 / 1024.0) / (timeDelta / 1000.0);
                
                log.info(sessionId + "Success? true, Speed: " + speed + " MB/s");
                DownloadEntry dle = downloaderLogService.createDownloadEntry(true, speed, id,
                         "file", destinationFormat,
                        startCoordinate, endCoordinate, bytesTransferred);
                downloaderLogService.logDownload(dle);
            }
            
            try {
                if (in != null)
                    in.close();
                if (encryptedDigestOut != null)
                    encryptedDigestOut.close();
                if (eOut != null)
                    eOut.close();
            } catch (Exception ex) {
                log.error(sessionId + " Error Location: " + errorLocation + "\n" + ex.toString(), ex);
                throw new GeneralStreamingException(sessionId.concat(" ").concat(ex.toString()), 5);
            }
        }
    }
    
    @Override
    @Cacheable(cacheNames = "fileHead", key="T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication() + #p0 + #p1 + #p2 + #p3")
    public void getFileHead(String fileId,
                            String destinationFormat,
                            HttpServletRequest request,
                            HttpServletResponse response) {
        String sessionId= Strings.isNullOrEmpty(request.getHeader("Session-Id"))? "" : request.getHeader("Session-Id") + " ";

        // Ascertain Access Permissions for specified File ID
        File reqFile = fileInfoService.getFileInfo(fileId, sessionId); // request added for ELIXIR

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


    // Return ReEncrypted Output Stream for Target
    // This function also takes a specified IV as parameter, to produce a target
    // "random access" encrypting output stream, properly initialised
    private OutputStream getTarget(OutputStream outStream,
                                   String destinationFormat,
                                   String destinationKey,
                                   String destinationIV,
                                   long startCoordinate) throws NoSuchAlgorithmException,
            NoSuchPaddingException,
            InvalidKeyException,
            InvalidAlgorithmParameterException,
            IOException {
        OutputStream out = null; // Return Stream - an Encrypted File
        boolean IVSpecified = (destinationIV != null && destinationIV.length() > 0);

        if (destinationFormat.equalsIgnoreCase("plain")) {
            out = outStream; // No Encryption Necessary

        } else if (destinationFormat.equalsIgnoreCase("aes128") ||
                destinationFormat.equalsIgnoreCase("aes256")) {
            // Specify Encryption stength with specified key
            int bits = 128;
            if (destinationFormat.equalsIgnoreCase("aes256"))
                bits = 256;
            SecretKey secret = new Glue().getKey(destinationKey.toCharArray(), bits);
            // Determine random IV - either random, or specified. Account for starting offset
            byte[] random_iv = new byte[16];
            if (IVSpecified) {
                //byte[] dIV = Base64.decode(destinationIV);
                byte[] dIV = java.util.Base64.getDecoder().decode(destinationIV);
                System.arraycopy(dIV, 0, random_iv, 0, 16);
                DecryptionUtils.byteIncrementFast(random_iv, startCoordinate);
            } else {
                SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
                random.nextBytes(random_iv);
            }
            AlgorithmParameterSpec paramSpec = new IvParameterSpec(random_iv);
            // If the random IV was generated in here, write it to the output stream
            if (!IVSpecified)
                outStream.write(random_iv);
            Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding"); // load a cipher AES / Segmented Integer Counter
            cipher.init(Cipher.ENCRYPT_MODE, secret, paramSpec);
            out = new CipherOutputStream(outStream, cipher);

        } 

        return out;
    }

    // *************************************************************************
    // ** Get Public Key fo Encryption
    public PGPPublicKey getPublicGPGKey(String destinationFormat) throws IOException {
        PGPPublicKey pgKey = null;
        Security.addProvider(new BouncyCastleProvider());

        // Paths (file containing the key - no paswords for public GPG Keys)
        KeyPath vals = keyService.getKeyPath(destinationFormat);
        if (vals == null) {
            throw new GeneralStreamingException("Can't Read Destination Key: " + destinationFormat, 10);
        }
        String path = vals.getKeyPath();
        InputStream in = new FileInputStream(path);

        // Two types of public GPG key files - pick the correct one! (through trial-and-error)
        boolean error = false;
        try {
            pgKey = readPublicKey(in); // key ring file (e.g. EBI key) -- TODO remove!
        } catch (IOException | PGPException ex) {
            in.reset();
            error = true;
        }
        if (pgKey == null || error) {
            try {
                pgKey = getEncryptionKey(getKeyring(in)); // exported key file (should be standard)
            } catch (IOException ignored) {
            }
        }
        in.close();

        return pgKey;
    }

    // Getting a public GPG key from a keyring
    private PGPPublicKey readPublicKey(InputStream in)
            throws IOException, PGPException {
        in = PGPUtil.getDecoderStream(in);

        PGPPublicKeyRingCollection pgpPub = new PGPPublicKeyRingCollection(in, fingerPrintCalculater);

        //
        // we just loop through the collection till we find a key suitable for encryption, in the real
        // world you would probably want to be a bit smarter about this.
        //
        PGPPublicKey key = null;

        //
        // iterate through the key rings.
        //
        Iterator rIt = pgpPub.getKeyRings();

        while (key == null && rIt.hasNext()) {
            PGPPublicKeyRing kRing = (PGPPublicKeyRing) rIt.next();
            Iterator kIt = kRing.getPublicKeys();

            while (key == null && kIt.hasNext()) {
                PGPPublicKey k = (PGPPublicKey) kIt.next();

                if (k.isEncryptionKey()) {
                    key = k;
                }
            }
        }

        if (key == null) {
            throw new IllegalArgumentException("Can't find encryption key in key ring.");
        }

        return key;
    }

    private void loadHeaderCleversafe(String id, String url, String httpAuth, long fileSize,
                                      HttpServletRequest request_, HttpServletResponse response_, String sourceKey) {
        String sessionId = Strings.isNullOrEmpty(request_.getHeader("Session-Id")) ? ""
                : request_.getHeader("Session-Id") + " ";

        if (url.startsWith("s3")) {
            url = s3Commons.getS3ObjectUrl(url);
        }

        HttpGet request = new HttpGet(url);

        fireCommons.addAuthenticationForFireRequest(httpAuth, url, request);
        request.addHeader("Range", "bytes=0-16");

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            if (response == null || response.getEntity() == null) {
                response_.setStatus(534);
                throw new ServerErrorException(sessionId + "LoadHeader: Error obtaining input stream for ", url);
            }
            try (DataInputStream content = new DataInputStream(response.getEntity().getContent())) {
                byte[] IV = new byte[16];
                content.readFully(IV);
                myHeaderCache.put(id, new EgaAESFileHeader(IV, "aes256", fileSize, url, sourceKey));
            }
        } catch (IOException ex) {
            throw new ServerErrorException(sessionId + "LoadHeader: " + ex.toString() + " :: ", url);
        }

    }

}
