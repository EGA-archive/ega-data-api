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
package eu.elixir.ega.ebi.reencryptionmvc.service.internal;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestOutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;
import java.util.ArrayList;
import java.util.Iterator;

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
import org.apache.http.impl.client.HttpClientBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.KeyFingerPrintCalculator;
import org.bouncycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.bouncycastle.openpgp.operator.bc.BcPublicKeyDataDecryptorFactory;
import org.cache2k.Cache;

import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;

import eu.elixir.ega.ebi.reencryptionmvc.cache2k.My2KCachePageFactory;
import eu.elixir.ega.ebi.reencryptionmvc.dto.EgaAESFileHeader;
import eu.elixir.ega.ebi.reencryptionmvc.dto.KeyPath;
import eu.elixir.ega.ebi.reencryptionmvc.exception.GeneralStreamingException;
import eu.elixir.ega.ebi.reencryptionmvc.exception.ServerErrorException;
import eu.elixir.ega.ebi.reencryptionmvc.service.KeyService;
import eu.elixir.ega.ebi.reencryptionmvc.service.ResService;
import eu.elixir.ega.ebi.reencryptionmvc.util.FireCommons;
import eu.elixir.ega.ebi.reencryptionmvc.util.S3Commons;
import htsjdk.samtools.seekablestream.FakeSeekableStream;
import htsjdk.samtools.seekablestream.SeekableBasicAuthHTTPStream;
import htsjdk.samtools.seekablestream.SeekableHTTPStream;
import htsjdk.samtools.seekablestream.SeekablePathStream;
import htsjdk.samtools.seekablestream.SeekableStream;
import htsjdk.samtools.seekablestream.cipher.ebi.GPGOutputStream;
import htsjdk.samtools.seekablestream.cipher.ebi.GPGStream;
import htsjdk.samtools.seekablestream.cipher.ebi.Glue;
import htsjdk.samtools.seekablestream.cipher.ebi.RemoteSeekableCipherStream;
import htsjdk.samtools.seekablestream.cipher.ebi.SeekableCipherStream;
import htsjdk.samtools.seekablestream.ebi.BufferedBackgroundSeekableInputStream;
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
    
    public CacheResServiceImpl(KeyService keyService, Cache<String, EgaAESFileHeader> myHeaderCache,
            My2KCachePageFactory pageDowloader, FireCommons fireCommons, S3Commons s3Commons) {
        this.keyService = keyService;
        this.myHeaderCache = myHeaderCache;
        this.pageDowloader = pageDowloader;
        this.fireCommons = fireCommons;
        this.s3Commons = s3Commons;
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

    private static void byte_increment_fast(byte[] data, long increment) {
        long countdown = increment / 16; // Count number of block updates

        ArrayList<Integer> digits_ = new ArrayList<>();
        int cnt = 0;
        long d = 256, cn = 0;
        while (countdown > cn && d > 0) {
            int l = (int) ((countdown % d) / (d / 256));
            digits_.add(l);
            cn += (l * (d / 256));
            d *= 256;
        }
        int size = digits_.size();
        int[] digits = new int[size];
        for (int i = 0; i < size; i++) {
            digits[size - 1 - i] = digits_.get(i); // intValue()
        }

        int cur_pos = data.length - 1, carryover = 0, delta = data.length - digits.length;

        for (int i = cur_pos; i >= delta; i--) { // Work on individual digits
            int digit = digits[i - delta] + carryover; // convert to integer
            int place = (int) (data[i] & 0xFF); // convert data[] to integer
            int new_place = digit + place;
            if (new_place >= 256) carryover = 1;
            else carryover = 0;
            data[i] = (byte) (new_place % 256);
        }

        // Deal with potential last carryovers
        cur_pos -= digits.length;
        while (carryover == 1 && cur_pos >= 0) {
            data[cur_pos]++;
            if (data[cur_pos] == 0) carryover = 1;
            else carryover = 0;
            cur_pos--;
        }
    }
    
    @Override
    public long transfer(String sourceFormat,
                         String sourceKey,
                         String sourceIV,
                         String destintionFormat,
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

		String sessionId= Strings.isNullOrEmpty(request.getHeader("Session-Id"))? "" : request.getHeader("Session-Id") + " ";
		
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

        int errorLocation = 0;
        try {
            // Get Send Stream - http Response, wrap in Digest Stream
            outStream = response.getOutputStream();
            encryptedDigest = MessageDigest.getInstance("MD5");
            encryptedDigestOut = new DigestOutputStream(outStream, encryptedDigest);

            // Generate Encrypting OutputStream
            eOut = getTarget(encryptedDigestOut,
                    destintionFormat,
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
            else if (sourceFormat.equalsIgnoreCase("symmetricgpg")) {
                getSource(sourceFormat, sourceKey, fileLocation, httpAuth, fileSize);
            }
            if (endCoordinate > fileSize)
                endCoordinate = fileSize;
            // Adjust start coordinate requested - to match 16 byte block structure
            if (destintionFormat.toLowerCase().startsWith("aes") &&
                    destinationIV != null && destinationIV.length() > 0) {
                long blockStart = (startCoordinate / 16) * 16;
                int blockDelta = (int) (startCoordinate - blockStart);
                startCoordinate -= blockDelta;
            }
            long bytesToTransfer = fileSize - startCoordinate - (endCoordinate > 0 ? (fileSize - endCoordinate) : 0);
            long bytesTransferred = 0;
            errorLocation = 2;

            int startPage = (int) (startCoordinate / BUFFER_SIZE);
            int pageOffset = (int) (startCoordinate - ((long) startPage * (long) BUFFER_SIZE));
            String key = id + "_" + startPage;

            while (bytesTransferred < bytesToTransfer) {
                errorLocation = 3;

                key = id + "_" + startPage;
                byte[] page = pageDowloader.downloadPage(key);
                errorLocation = 4;
                if (page == null)
                    throw new GeneralStreamingException(sessionId + " Error getting page " + key);

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
            return bytesTransferred;
        } catch (Exception ex) {
            log.error(sessionId + " Error Location: " + errorLocation + "\n" + ex.toString());
            throw new GeneralStreamingException(sessionId + " Error Location: " + errorLocation + "\n" + ex.toString(), 10);
        } finally {
            try {
                in.close();
                encryptedDigestOut.close();
                eOut.close();
            } catch (Exception ex) {
                log.error(sessionId + " Error Location: " + errorLocation + "\n" + ex.toString());
                throw new GeneralStreamingException(sessionId.concat(" ").concat(ex.toString()), 5);
            }
        }
    }

    // Return Unencrypted Seekable Stream from Source
    private SeekableStream getSource(String sourceFormat,
                                     String sourceKey,
                                     String fileLocation,
                                     String httpAuth,
                                     long fileSize) {

        SeekableStream fileIn = null; // Source of File
        SeekableStream plainIn = null; // Return Stream - a Decrypted File
        try {
            // Obtain Input Stream - from a File or an HTTP server; or an S3 Bucket
            if (fileLocation.toLowerCase().startsWith("http")) { // Access Cleversafe Need Basic Auth here!
                URL url = new URL(fileLocation);
                fileIn = httpAuth == null ? new SeekableHTTPStream(url) : new SeekableBasicAuthHTTPStream(url, httpAuth);
            } else if (fileLocation.toLowerCase().startsWith("s3")) { // S3
                URL url = new URL(s3Commons.getS3ObjectUrl(fileLocation));
                fileIn = new SeekableHTTPStream(url);
            } else { // No Protocol -- Assume File Path
                fileLocation = "file://" + fileLocation;
                Path filePath = Paths.get(new URI(fileLocation));
                fileIn = new SeekablePathStream(filePath);
            }
            // Wrap source Stream in Buffered Background Stream - pre-load source data
            fileIn = new BufferedBackgroundSeekableInputStream(fileIn);

            // Obtain Plain Input Stream
            if (sourceFormat.equalsIgnoreCase("plain")) {
                plainIn = fileIn; // No Decryption Necessary
            } else if (sourceFormat.equalsIgnoreCase("aes128")) {
                plainIn = new SeekableCipherStream(fileIn, sourceKey.toCharArray(), (int) BUFFER_SIZE, 128);
            } else if (sourceFormat.equalsIgnoreCase("aes256")) {
                //plainIn = new EgaSeekableCipherStream(fileIn, sourceKey.toCharArray(), BUFFER_SIZE, 256);
                plainIn = new RemoteSeekableCipherStream(fileIn, sourceKey.toCharArray(), (int) BUFFER_SIZE, 256);
            } else if (sourceFormat.equalsIgnoreCase("symmetricgpg")) {
                plainIn = getSymmetricGPGDecryptingInputStream(fileIn, sourceKey);
            } else if (sourceFormat.toLowerCase().startsWith("publicgpg")) {
                plainIn = getAsymmetricGPGDecryptingInputStream(fileIn, sourceKey, sourceFormat);
            }
        } catch (IOException | URISyntaxException ex) {
            log.error(ex.getMessage(), ex);
        }

        return plainIn;
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
            SecretKey secret = Glue.getInstance().getKey(destinationKey.toCharArray(), bits);
            // Determine random IV - either random, or specified. Account for starting offset
            byte[] random_iv = new byte[16];
            if (IVSpecified) {
                //byte[] dIV = Base64.decode(destinationIV);
                byte[] dIV = java.util.Base64.getDecoder().decode(destinationIV);
                System.arraycopy(dIV, 0, random_iv, 0, 16);
                byte_increment_fast(random_iv, startCoordinate);
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

        } else if (destinationFormat.toLowerCase().startsWith("publicgpg")) {
            PGPPublicKey gpgKey = getPublicGPGKey(destinationFormat);
            out = new GPGOutputStream(outStream, gpgKey); // Public Key GPG
        }

        return out;
    }

    /*
     * Archive Related Helper Functions -- GPG
     */
    private SeekableStream getSymmetricGPGDecryptingInputStream(InputStream c_in, String sourceKey) {
        Security.addProvider(new BouncyCastleProvider());
        InputStream in = c_in;

        try {
            // Load key, if not provided. Details in config XML file
            if (sourceKey == null || sourceKey.length() == 0) {
                KeyPath keyPath = keyService.getKeyPath("SymmetricGPG");
                BufferedReader br = new BufferedReader(new FileReader(keyPath.getKeyPath()));
                sourceKey = br.readLine();
                br.close();
            }

            in = GPGStream.getDecodingGPGInoutStream(in, sourceKey.toCharArray());

        } catch (IOException | PGPException | NoSuchProviderException ex) {
            log.error("GOPG Error " +ex.getMessage(), ex);
        }

        return new FakeSeekableStream(in);
    }

    private SeekableStream getAsymmetricGPGDecryptingInputStream(InputStream c_in, String sourceKey, String sourceFormat) {
        Security.addProvider(new BouncyCastleProvider());
        InputStream in = null;

        try {
            KeyPath keyPath = sourceFormat.equalsIgnoreCase("publicgpg_sanger") ?
                    keyService.getKeyPath("PrivateGPG_Sanger") :
                    keyService.getKeyPath("PrivateGPG");

            BufferedReader br = new BufferedReader(new FileReader(keyPath.getKeyPassPath()));
            String key = br.readLine();
            br.close();

            InputStream keyIn = new BufferedInputStream(new FileInputStream(keyPath.getKeyPath()));

            PGPObjectFactory pgpF = new PGPObjectFactory(c_in, fingerPrintCalculater);
            PGPEncryptedDataList enc;

            Object o = pgpF.nextObject();
            //
            // the first object might be a PGP marker packet.
            //
            if (o instanceof PGPEncryptedDataList) {
                enc = (PGPEncryptedDataList) o;
            } else {
                enc = (PGPEncryptedDataList) pgpF.nextObject();
            }

            //
            // find the secret key
            //
            Iterator<PGPPublicKeyEncryptedData> it = enc.getEncryptedDataObjects();
            PGPPrivateKey sKey = null;
            PGPPublicKeyEncryptedData pbe = null;
            PGPSecretKeyRingCollection pgpSec = new PGPSecretKeyRingCollection(
                    PGPUtil.getDecoderStream(keyIn), fingerPrintCalculater);

            while (sKey == null && it.hasNext()) {
                try {
                    pbe = it.next();

                    PGPSecretKey pgpSecKey = pgpSec.getSecretKey(pbe.getKeyID());
                    if (pgpSecKey != null) {
                        PBESecretKeyDecryptor decryptor = new BcPBESecretKeyDecryptorBuilder(new BcPGPDigestCalculatorProvider()).build(key.toCharArray());
                        sKey = pgpSecKey.extractPrivateKey(decryptor);
                    }
                } catch (Throwable t) {
                    log.error(t.getMessage(), t);
                }
            }

            if (sKey == null) {
                throw new IllegalArgumentException("secret key for message not found.");
            }

            BcPublicKeyDataDecryptorFactory pkddf = new BcPublicKeyDataDecryptorFactory(sKey);
            InputStream clear = pbe.getDataStream(pkddf);

            PGPObjectFactory plainFact = new PGPObjectFactory(clear, fingerPrintCalculater);

            Object message = plainFact.nextObject();

            if (message instanceof PGPCompressedData) {
                PGPCompressedData cData = (PGPCompressedData) message;
                PGPObjectFactory pgpFact = new PGPObjectFactory(cData.getDataStream(), fingerPrintCalculater);

                message = pgpFact.nextObject();
            }

            if (message instanceof PGPLiteralData) {
                PGPLiteralData ld = (PGPLiteralData) message;
                in = ld.getInputStream();
            }
        } catch (IOException | PGPException ex) {
            log.error(ex.getMessage(), ex);
        }

        return new FakeSeekableStream(in);
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
        
        try (CloseableHttpClient httpclient = HttpClientBuilder.create().build();
                CloseableHttpResponse response = httpclient.execute(request)) {
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
