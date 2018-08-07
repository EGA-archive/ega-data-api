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

import static com.amazonaws.HttpMethod.GET;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.MalformedURLException;
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
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.google.common.io.ByteStreams;

import eu.elixir.ega.ebi.reencryptionmvc.config.GeneralStreamingException;
import eu.elixir.ega.ebi.reencryptionmvc.config.ServerErrorException;
import eu.elixir.ega.ebi.reencryptionmvc.dto.CachePage;
import eu.elixir.ega.ebi.reencryptionmvc.dto.EgaAESFileHeader;
import eu.elixir.ega.ebi.reencryptionmvc.dto.KeyPath;
import eu.elixir.ega.ebi.reencryptionmvc.dto.MyAwsConfig;
import eu.elixir.ega.ebi.reencryptionmvc.service.KeyService;
import eu.elixir.ega.ebi.reencryptionmvc.service.ResService;
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


/**
 * @author asenf
 */
@Service
@Profile("default")
@Primary
@EnableDiscoveryClient
public class CacheResServiceImpl implements ResService {

    @Autowired
    private KeyService keyService;

    @Autowired
    private MyAwsConfig myAwsConfig;

    @Autowired
    private Cache<String, EgaAESFileHeader> myHeaderCache;

    @Autowired
    private Cache<String, CachePage> myPageCache;

    /**
     * Background processing
     */
    ExecutorService executorService2 = Executors.newFixedThreadPool(200);

    /**
     * Size of a byte buffer to read/write file (for Random Stream)
     */
    //private static final int BUFFER_SIZE = 16;
    private static final long BUFFER_SIZE = 1024 * 1024 * 12;
    private static final int MAX_CONCURRENT = 4;

    private static ConcurrentHashMap loadQueue = new ConcurrentHashMap<>(); 
    private static Set concurrentHashSet = loadQueue.newKeySet();
    
    /**
     * Bouncy Castle code for Public Key encrypted Files
     */
    private static final KeyFingerPrintCalculator fingerPrintCalculater = new BcKeyFingerprintCalculator();
    private static final BcPGPDigestCalculatorProvider calc = new BcPGPDigestCalculatorProvider();

    /*
     * Perform Data Transfer Requested by File Controller
     */

    @Override
    public void transfer(String sourceFormat,
                         String sourceKey,
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

        // Check if File Header is in Cache - otherwise Load it
        if (!myHeaderCache.containsKey(id))
            loadHeaderCleversafe(id, fileLocation, httpAuth, fileSize, response, sourceKey);

        // Streams and Digests for this data transfer
        OutputStream outStream = null;
        MessageDigest encryptedDigest = null;
        DigestOutputStream encryptedDigestOut = null;
        OutputStream eOut = null;

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
                throw new GeneralStreamingException("Output Stream (ReEncryption Stage) Null", 2);
            }

            // Transfer Loop - Get data from Cache, write it - until done!
            SeekableStream sIn = null;
            if (sourceFormat.equalsIgnoreCase("aes128") || sourceFormat.equalsIgnoreCase("aes256"))
                fileSize = fileSize - 16; // Adjust CIP File Size (subtracting 16 bytes)
            else if (sourceFormat.equalsIgnoreCase("symmetricgpg")) {
                sIn = getSource(sourceFormat, sourceKey, fileLocation, httpAuth, fileSize);
            }
            if (endCoordinate > fileSize)
                endCoordinate = fileSize;
            // Adjust start coordinate requested - to match 16 byte block structure
            if (destintionFormat.toLowerCase().startsWith("aes") && 
                    destinationIV!= null && destinationIV.length() > 0) {
                long blockStart = (startCoordinate / 16) * 16;
                int blockDelta = (int) (startCoordinate - blockStart);
                startCoordinate -= blockDelta;
            }
            long bytesToTransfer = fileSize - startCoordinate - (endCoordinate > 0 ? (fileSize - endCoordinate) : 0);
            long bytesTransferred = 0;
            errorLocation = 2;

            int cacheStartPage = (int) (startCoordinate / BUFFER_SIZE);
            int cacheEndPage = (int) ((endCoordinate > 0 ? endCoordinate : fileSize) / BUFFER_SIZE);

            // Cache Page Key
            int cachePage = cacheStartPage;
            int cachePageOffset = (int) (startCoordinate - ((long) cachePage * (long) BUFFER_SIZE));
            String key = id + "_" + cachePage;

            while (bytesTransferred < bytesToTransfer) {
                errorLocation = 3;
                
                // New: Cache Loader takes care of loading autonomously, based on Key
                key = id + "_" + cachePage;
                byte[] get = myPageCache.get(key).getPage(); // Get Cache page that contains requested data
                errorLocation = 4;
                if (get == null)
                    throw new GeneralStreamingException("Error getting Cache Page " + key + " at Stage ", 8);
                // Prefetch Loop (prefetch next few pages in background)
                for (int prefetch = 1; prefetch <= MAX_CONCURRENT; prefetch++) {
                    if ((cachePage + prefetch) < cacheEndPage) { // no paged past end of file
                        final String key__ = id + "_" + (cachePage + prefetch);
                        if (!myPageCache.containsKey(key__) && !concurrentHashSet.contains(key__)) {
                            concurrentHashSet.add(key__);
                            //Executors.newSingleThreadExecutor().execute(new Runnable() {
                            Executors.newCachedThreadPool().execute(new Runnable() {
                                @Override
                                public void run() {
                                    byte[] page = myPageCache.get(key__).getPage();
                                    concurrentHashSet.remove(key__);
                                }
                            });
                        }
                    }
                }
                errorLocation = 5;

                ByteArrayInputStream bais = new ByteArrayInputStream(get); // Wrap byte array
                bais.skip(cachePageOffset); // first cache page

                // At this point the plain data is in a cache page
                InputStream in;
                long delta = bytesToTransfer - bytesTransferred;
                if (delta < get.length) {
                    in = ByteStreams.limit(bais, delta);
                } else {
                    in = bais;
                }
                cachePageOffset = 0;

                // Copy the specified contents - decrypting through input, encrypting through output
                long bytes = ByteStreams.copy(in, eOut);
                errorLocation = 6;
                bytesTransferred += bytes;
                cachePage += 1;
            }
        } catch (Exception ex) {
            throw new GeneralStreamingException("Error Location: " + errorLocation + "\n" + ex.toString(), 10);
        } finally {
            try {
                // Close all Streams in reverse order (theoretically only the first should be necessary)
                eOut.close();
                encryptedDigestOut.close();

                // Compute Digests
                byte[] encryptedDigest_ = encryptedDigest.digest();
                BigInteger bigIntEncrypted = new BigInteger(1, encryptedDigest_);
                String encryptedHashtext = bigIntEncrypted.toString(16);
                while (encryptedHashtext.length() < 32) {
                    encryptedHashtext = "0" + encryptedHashtext;
                }

            } catch (Exception ex) {
                throw new GeneralStreamingException(ex.toString(), 5);
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
                URL url = new URL(getS3ObjectUrl(fileLocation));        
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
            System.out.println(" ** " + ex.toString());
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
        boolean IVSpecified = (destinationIV != null && destinationIV.length()>0);

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
            System.out.println("GOPG Error " + ex.toString());
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
                    if (pgpSecKey == null) {
                        sKey = null;
                    } else {
                        PBESecretKeyDecryptor decryptor = new BcPBESecretKeyDecryptorBuilder(new BcPGPDigestCalculatorProvider()).build(key.toCharArray());
                        sKey = pgpSecKey.extractPrivateKey(decryptor);
                    }
                } catch (Throwable t) {
                    System.out.println("Error -- " + t.getLocalizedMessage());
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
            System.out.println(" *** " + ex.toString());
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
            } catch (IOException ex) {
                ;
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
            boolean encryptionKeyFound = false;

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

    private void loadHeaderCleversafe(String id, String url, String httpAuth,
                                      long fileSize, HttpServletResponse response_, String sourceKey) {
        boolean close = false;

        if (url.startsWith("s3")) {
            url = getS3ObjectUrl(url);
        }
        
        // Load first 16 bytes; set stats
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet(url);

        if (httpAuth != null && httpAuth.length() > 0) { // Old: http Auth
            String encoding = java.util.Base64.getEncoder().encodeToString(httpAuth.getBytes());
            String auth = "Basic " + encoding;
            request.addHeader("Authorization", auth);
        } else if (!url.contains("X-Amz")) {        // Not an S3 URL - Basic Auth embedded with URL
//            close = true;
            try {
                URL url_ = new URL(url);
                if (url_.getUserInfo() != null) {
                    //String encoding = new sun.misc.BASE64Encoder().encode(url_.getUserInfo().getBytes());
                    //encoding = encoding.replaceAll("\n", "");
                    String encoding = java.util.Base64.getEncoder().encodeToString(url_.getUserInfo().getBytes());
                    String auth = "Basic " + encoding;
                    request.addHeader("Authorization", auth);
                }
            } catch (MalformedURLException ex) {
            }
        }  

        byte[] IV = new byte[16];
        try {
            HttpResponse response = httpclient.execute(request);
            if (response == null || response.getEntity() == null) {
                response_.setStatus(534);
                throw new ServerErrorException("LoadHeader: Error obtaining input stream for ", url);
            }
            DataInputStream content = new DataInputStream(response.getEntity().getContent());
            content.readFully(IV);
            if (close) content.close();

            EgaAESFileHeader header = new EgaAESFileHeader(IV, "aes256", fileSize, url, sourceKey);
            myHeaderCache.put(id, header);
        } catch (IOException ex) {
            throw new ServerErrorException("LoadHeader: " + ex.toString() + " :: ", url);
        }
    }
    
    private String getS3ObjectUrl(String fileLocation) {
        final String bucket = fileLocation.substring(5, fileLocation.indexOf("/", 5));
        final String awsPath = fileLocation.substring(fileLocation.indexOf("/", 5) + 1);
        
        final AWSCredentials credentials = new BasicAWSCredentials(myAwsConfig.getAwsAccessKeyId(),
                myAwsConfig.getAwsSecretAccessKey());
        final AmazonS3 s3 = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials)).withPathStyleAccessEnabled(true)
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(myAwsConfig.getAwsEndpointUrl(), myAwsConfig.getAwsRegion()))
                .build();
        
        Date expiration = new Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += (1000 * 3600) * 24 ;
        expiration.setTime(expTimeMillis);
        
        GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucket, awsPath).withMethod(GET)
                .withExpiration(expiration);
        URL url = s3.generatePresignedUrl(generatePresignedUrlRequest);
        return url.toString();
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
}
