/*
 * Copyright 2017 ELIXIR EGA
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
package eu.elixir.ega.ebi.reencryptionmvc.config;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.google.common.io.CountingInputStream;
import com.google.gson.Gson;
import eu.elixir.ega.ebi.reencryptionmvc.dto.ArchiveSource;
import eu.elixir.ega.ebi.reencryptionmvc.dto.CachePage;
import eu.elixir.ega.ebi.reencryptionmvc.dto.EgaAESFileHeader;
import eu.elixir.ega.ebi.reencryptionmvc.dto.EgaFile;
import eu.elixir.ega.ebi.reencryptionmvc.service.internal.CacheResServiceImpl;
import htsjdk.samtools.seekablestream.SeekableHTTPStream;
import htsjdk.samtools.seekablestream.SeekablePathStream;
import htsjdk.samtools.seekablestream.SeekableStream;
import htsjdk.samtools.seekablestream.cipher.ebi.Glue;
import htsjdk.samtools.seekablestream.cipher.ebi.RemoteSeekableCipherStream;
import htsjdk.samtools.seekablestream.cipher.ebi.SeekableCipherStream;
import htsjdk.samtools.seekablestream.ebi.AsyncBufferedSeekableHTTPStream;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.amazonaws.HttpMethod.GET;

/**
 * @author asenf
 */
public class My2KCachePageFactory implements FactoryBean<Cache<String, CachePage>> { //extends SimpleJdbcDaoSupport

    private final Cache<String, EgaAESFileHeader> myHeaderCache;

    private final int pageSize;
    private final int pageCount;
    private final LoadBalancerClient loadBalancer;
    private final String awsAccessKeyId;
    private final String awsSecretAccessKey;
    private final String awsEndpointUrl;
    private final String awsRegion;
    private final String fireUrl;
    private final String fireArchive;
    private final String fireKey;

    public My2KCachePageFactory(LoadBalancerClient loadBalancer,
                                int pageSize,
                                int pageCount,
                                String awsAccessKeyId,
                                String awsSecretAccessKey,
                                String fireUrl,
                                String fireArchive,
                                String fireKey,
                                String awsEndpointUrl,
                                String awsRegion) {

        this.myHeaderCache = (new My2KCacheFactory()).getObject(); //myCache;

        this.loadBalancer = loadBalancer;
        this.pageSize = pageSize;
        this.pageCount = pageCount;
        this.awsAccessKeyId = awsAccessKeyId;
        this.awsSecretAccessKey = awsSecretAccessKey;
        this.awsEndpointUrl = awsEndpointUrl;
        this.awsRegion = awsRegion;
        this.fireUrl = fireUrl;
        this.fireArchive = fireArchive;
        this.fireKey = fireKey;
    }

    private String getServiceURL(String service) {
        return loadBalancer.choose(service).getUri().toString();
    }

    private static void byteIncrementFast(byte[] data, long increment) {
        long countdown = increment / 16; // Count number of block updates

        ArrayList<Integer> digits_ = new ArrayList<>();
        long d = 256;
        long cn = 0;
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
            int place = data[i] & 0xFF; // convert data[] to integer
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
    public Cache<String, CachePage> getObject() {
        return new Cache2kBuilder<String, CachePage>() {
        }
                .expireAfterWrite(10, TimeUnit.MINUTES)    // expire/refresh after 10 minutes
                .resilienceDuration(45, TimeUnit.SECONDS) // cope with at most 45 seconds
                // outage before propagating
                // exceptions
                .refreshAhead(false)                      // keep fresh when expiring
                .loader(this::loadPage)                   // auto populating function
                .keepDataAfterExpired(false)
                .loaderExecutor(Executors.newFixedThreadPool(1280))
                .loaderThreadCount(640)
                .entryCapacity(this.pageCount)
                .build();
    }

    @Override
    public Class<?> getObjectType() {
        return Cache.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    /*
     * Cache Page Loader
     * Derive Path and Coordinates from Key
     */
    private CachePage loadPage(String key) {
        HttpClient localHttpclient = HttpClientBuilder.create().build();
        String[] keys = key.split("\\_");

        String id = keys[0];
        int cachePage = Integer.parseInt(keys[1]);

        String httpAuth = "";
        String sourceKey;
        if (!myHeaderCache.containsKey(id)) { // Get Header (once in 24h)
            ArchiveSource source;

            String fileDatabaseURL = getServiceURL("FILEDATABASE");
            HttpGet sourceRequest = new HttpGet(fileDatabaseURL + "/file/" + id);
            String keyServerURL = getServiceURL("KEYSERVER");
            HttpGet keyRequest = new HttpGet(keyServerURL + "/keys/filekeys/" + id);

            EgaFile[] files;
            String encryptionKey;
            try {
                // EgaFile
                HttpResponse sourceResponse = localHttpclient.execute(sourceRequest);
                if (sourceResponse.getEntity() == null)
                    throw new ServerErrorException("Error Attempting to Load Cache Header File data ", key);
                BufferedReader reader = new BufferedReader(new InputStreamReader(sourceResponse.getEntity().getContent()));
                Gson gson = new Gson();
                files = gson.fromJson(reader, EgaFile[].class);
                reader.close();
                if (files == null || files.length == 0) {
                    throw new ServerErrorException("Error Loading Cache Header File data", key);
                }

                // encryptionKey
                HttpResponse keyResponse = localHttpclient.execute(keyRequest);
                if (keyResponse.getEntity() == null) {
                    throw new ServerErrorException("Error Attempting to Load Cache Header key ", key);
                }
                reader = new BufferedReader(new InputStreamReader(keyResponse.getEntity().getContent()));
                encryptionKey = reader.readLine().trim();
                reader.close();
                if (encryptionKey == null || encryptionKey.length() == 0) {
                    throw new ServerErrorException("Error Loading Cache Header File key", key);
                }

            } catch (Exception ex) {
                throw new ServerErrorException("Error Loading Cache Header", key);
            }

            source = new ArchiveSource(files[0].getFileName(), files[0].getFileSize(), "", "aes256", encryptionKey, null);
            sourceKey = source.getEncryptionKey();

            String fileLocation = source.getFileUrl();
            httpAuth = source.getAuth();
            long fileSize = source.getSize();

            // Obtain Signed S3 URL, place in Header Cache
            loadHeaderCleversafe(id, fileLocation, httpAuth, fileSize, sourceKey);
            System.out.println(" --- " + id + " size: " + fileSize + " time to load: " + 0); //dt);
        } // Get Header - Complete

        // **
        // ** Load Cache Page [previous - header loaded only once]
        // **
        EgaAESFileHeader header = myHeaderCache.get(id);
        sourceKey = header.getSourceKey();

        long startCoordinate = (long) cachePage * pageSize; // Account for IV at start of File
        long endCoordinate = startCoordinate + pageSize;
        long fileSize = header.getSize();
        endCoordinate = endCoordinate > fileSize ? fileSize : endCoordinate; // End of file

        // Prepare Request (containd query parameters
        String url = header.getUrl();
        HttpGet request = new HttpGet(url);

        // Add request header for Basic Auth (for CleverSafe)
        if (httpAuth != null && httpAuth.length() > 0) {
            request.addHeader("Authorization", httpAuth);
        }

        // Add range header - logical (unencrypted) coordinates to file coordinates (add IV handling '+16')
        if ((startCoordinate + 16) >= header.getSize())
            return new CachePage(new byte[]{});
        //String byteRange = "bytes=" + (startCoordinate+16) + "-" + ((endCoordinate+16)>header.getSize()?header.getSize():(endCoordinate+16));
        String byteRange = "bytes=" + (startCoordinate + 16) + "-" + (endCoordinate + 16);
        request.addHeader("Range", byteRange);
        long pageSize_ = ((endCoordinate + 16) > header.getSize() ? header.getSize() : (endCoordinate + 16)) - (startCoordinate + 16);
        pageSize_ = pageSize > pageSize_ ? pageSize_ : pageSize;

        byte[] buffer = new byte[(int) pageSize_];
        byte[] decrypted;
        try {
            // Attemp loading page 3 times (mask object store read errors)
            int pageCnt = 0;
            boolean pageSuccess;
            do {
                try {
                    // Run the request
                    HttpResponse response = localHttpclient.execute(request);
                    if (response.getStatusLine().getStatusCode() != 200 && response.getStatusLine().getStatusCode() != 206)
                        throw new ServerErrorException("Error Loading Cache Page Code " + response.getStatusLine().getStatusCode() + " for ", key);

                    // Read response from HTTP call, count bytes read (encrypted Data)
                    CountingInputStream cIn = new CountingInputStream(response.getEntity().getContent());
                    DataInputStream dis = new DataInputStream(cIn);
                    dis.readFully(buffer);
                    pageSuccess = true;
                } catch (Throwable th) {
                    pageSuccess = false;
                    System.out.println("Error page " + key + " attempt " + pageCnt + ": " + th.toString());
                }
            } while (!pageSuccess && pageCnt++ < 3);

            // Decrypt, store plain in cache
            byte[] newIV = new byte[16]; // IV always 16 bytes long
            System.arraycopy(header.getIV(), 0, newIV, 0, 16); // preserved start value
            if (startCoordinate > 0) byteIncrementFast(newIV, startCoordinate);
            decrypted = decrypt(buffer, sourceKey, newIV);
        } catch (UnsupportedOperationException th) {
            System.out.println("HTTP GET ERROR -1 " + th.toString() + "   -- " + byteRange + "\n" + url);
            throw new ServerErrorException("Error Loading Cache Page -1 " + th.toString() + " for ", key);
        } catch (IOException th) {
            System.out.println("HTTP GET ERROR 0 " + th.toString() + "   -- " + byteRange + "\n" + url);
            throw new ServerErrorException("Error Loading Cache Page 0 " + th.toString() + " for ", key);
        } catch (IllegalBlockSizeException ex) {
            System.out.println("HTTP GET ERROR 1 " + ex.toString() + "   -- " + byteRange + "\n" + url);
            throw new ServerErrorException("Error Loading Cache Page 1 " + ex.toString() + " for ", key);
        } catch (BadPaddingException ex) {
            System.out.println("HTTP GET ERROR 2 " + ex.toString() + "   -- " + byteRange + "\n" + url);
            throw new ServerErrorException("Error Loading Cache Page 2 " + ex.toString() + " for ", key);
        } catch (Exception ex) {
            System.out.println("HTTP GET ERROR 3 " + ex.toString() + "   -- " + byteRange + "\n" + url);
            throw new ServerErrorException("Error Loading Cache Page 3 " + ex.toString() + " for ", key);
        } finally {
            request.releaseConnection();
        }

        return new CachePage(decrypted);
    }

    private void loadHeaderCleversafe(String id, String path, String httpAuth, long fileSize, String sourceKey) {
        String url;

        if (path.startsWith("s3")) {
            url = getS3ObjectUrl(id, path, httpAuth, fileSize, sourceKey);
        } else {
            String path_ = path.toLowerCase().startsWith("/fire/a/") ? path.substring(16) : path;
            String[] url__ = getPath(path_);
            url = url__[0];
        }

        // Load first 16 bytes; set stats
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet(url);

        if (httpAuth != null && httpAuth.length() > 0) { // Old: http Auth
            //String encoding = new sun.misc.BASE64Encoder().encode(httpAuth.getBytes());
            //encoding = encoding.replaceAll("\n", "");
            String encoding = java.util.Base64.getEncoder().encodeToString(httpAuth.getBytes());
            String auth = "Basic " + encoding;
            request.addHeader("Authorization", auth);
        } else if (!url.contains("X-Amz")) {        // Not an S3 URL - Basic Auth embedded with URL
            try {
                URL url_ = new URL(url);
                if (url_.getUserInfo() != null) {
                    //String encoding = new sun.misc.BASE64Encoder().encode(url_.getUserInfo().getBytes());
                    //encoding = encoding.replaceAll("\n", "");
                    String encoding = java.util.Base64.getEncoder().encodeToString(url_.getUserInfo().getBytes());
                    String auth = "Basic " + encoding;
                    request.addHeader("Authorization", auth);
                }
            } catch (MalformedURLException ignored) {
            }
        }                                           // S3 URL: Use as it is given!

        byte[] IV = new byte[16];
        try {
            HttpResponse response = httpclient.execute(request);
            DataInputStream content = new DataInputStream(response.getEntity().getContent());
            content.readFully(IV);
            //if (close) content.close();

            EgaAESFileHeader header = new EgaAESFileHeader(IV, "aes256", fileSize, url, sourceKey);
            myHeaderCache.put(id, header);
        } catch (IOException ex) {
            Logger.getLogger(CacheResServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private String getS3ObjectUrl(String id, String fileLocation, String httpAuth, long fileSize, String sourceKey) {
        System.out.println("Inside load loadHeaders3 - 2" + awsEndpointUrl + "==" + awsRegion);
        // Load first 16 bytes; set stats
        final String bucket = fileLocation.substring(5, fileLocation.indexOf("/", 5));
        final String awsPath = fileLocation.substring(fileLocation.indexOf("/", 5) + 1);

        final AWSCredentials credentials = new BasicAWSCredentials(awsAccessKeyId, awsSecretAccessKey);
        final AmazonS3 s3 = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials)).withPathStyleAccessEnabled(true)
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(awsEndpointUrl, awsRegion))
                .build();

        Date expiration = new Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += (1000 * 3600) * 24;
        expiration.setTime(expTimeMillis);

        GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucket, awsPath).withMethod(GET)
                .withExpiration(expiration);
        URL url = s3.generatePresignedUrl(generatePresignedUrlRequest);

        return url.toString();
    }

    /*
     * Archive Related Helper Functions -- AES
     */

    /*
     * Decryption Function
     */
    private byte[] decrypt(byte[] cipherText, String encryptionKey, byte[] IV) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
        SecretKey key_ = Glue.getInstance().getKey(encryptionKey.toCharArray(), 256);
        cipher.init(Cipher.DECRYPT_MODE, key_, new IvParameterSpec(IV));
        return cipher.doFinal(cipherText);
    }

    // Return Unencrypted Seekable Stream from Source
//    @HystrixCommand
    private SeekableStream getSource(String sourceFormat,
                                     String sourceKey,
                                     String fileLocation,
                                     String httpAuth,
                                     long fileSize) {

        SeekableStream fileIn; // Source of File
        SeekableStream plainIn = null; // Return Stream - a Decrypted File
        try {
            // Obtain Input Stream - from a File or an HTTP server; or an S3 Bucket
            if (fileLocation.toLowerCase().startsWith("http")) { // Access Cleversafe Need Basic Auth here!
                URL url = new URL(fileLocation);
                //fileIn = httpAuth==null?new SeekableHTTPStream(url):
                //                        new EgaSeekableHTTPStream(url, null, httpAuth, fileSize);
                /** start cache code **/
                fileIn = httpAuth == null ? new AsyncBufferedSeekableHTTPStream(url) :
                        new AsyncBufferedSeekableHTTPStream(url, null, httpAuth, fileSize);
                /** end cache code **/
            } else if (fileLocation.toLowerCase().startsWith("s3")) { // S3
                String awsPath = fileLocation.substring(23); // Strip "S3://"
                String bucket = fileLocation.substring(5, 20);
                AWSCredentials credentials = new BasicAWSCredentials(this.awsAccessKeyId, this.awsSecretAccessKey);
                AmazonS3 s3 = new AmazonS3Client(credentials);
                //S3Object object = s3.getObject(bucket, awsPath);
                //fileIn = new EgaFakeSeekableStream(object.getObjectContent()); // ??
                URL url = s3.getUrl(bucket, awsPath);
                fileIn = new SeekableHTTPStream(url);
            } else { // No Protocol -- Assume File Path
                fileLocation = "file://" + fileLocation;
                Path filePath = Paths.get(new URI(fileLocation));
                fileIn = new SeekablePathStream(filePath);
            }

            // Obtain Plain Input Stream
            if (sourceFormat.equalsIgnoreCase("plain")) {
                plainIn = fileIn; // No Decryption Necessary
            } else if (sourceFormat.equalsIgnoreCase("aes128")) {
                plainIn = new SeekableCipherStream(fileIn, sourceKey.toCharArray(), pageSize, 128);
            } else if (sourceFormat.equalsIgnoreCase("aes256")) {
                //plainIn = new SeekableCipherStream(fileIn, sourceKey.toCharArray(), BUFFER_SIZE, 256);
                plainIn = new RemoteSeekableCipherStream(fileIn, sourceKey.toCharArray(), pageSize, 256);
                //} else if (sourceFormat.equalsIgnoreCase("symmetricgpg")) {
                //    plainIn = getSymmetricGPGDecryptingInputStream(fileIn, sourceKey);
                //} else if (sourceFormat.toLowerCase().startsWith("publicgpg")) {
                //    plainIn = getAsymmetricGPGDecryptingInputStream(fileIn, sourceKey, sourceFormat);
            }
        } catch (IOException | URISyntaxException ex) {
            System.out.println(" ** " + ex.toString());
        }

        return plainIn;
    }

    private String[] getPath(String path) {
        if (path.equalsIgnoreCase("Virtual File")) return new String[]{"Virtual File"};

        try {
            String[] result = new String[4]; // [0] name [1] stable_id [2] size [3] rel path
            result[0] = "";
            result[1] = "";
            result[3] = path;
            String path_ = path;

            // Sending Request; 4 re-try attempts
            int reTryCount = 4;
            int responseCode = 0;
            HttpURLConnection connection = null;
            do {
                try {
                    connection = (HttpURLConnection) (new URL(fireUrl)).openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("X-FIRE-Archive", fireArchive);
                    connection.setRequestProperty("X-FIRE-Key", fireKey);
                    connection.setRequestProperty("X-FIRE-FilePath", path_);

                    // Reading Response - with Retries
                    responseCode = connection.getResponseCode();
                    //System.out.println("Response Code " + responseCode);
                } catch (Throwable th) {
                    System.out.println("FIRE error: " + th.toString());
                }
                if (responseCode != 200) {
                    connection = null;
                    Thread.sleep(500);
                }
            } while (responseCode != 200 && --reTryCount > 0);

            // if Response OK
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;

                ArrayList<String[]> paths = new ArrayList<>();

                String object_get = "",             // 1
                        object_head = "",            // 2
                        object_md5 = "",             // 3
                        object_length = "",          // 4
                        object_url_expire = "",      // 5
                        object_storage_class = "";   // 6
                while ((inputLine = in.readLine()) != null) {
                    if (inputLine.startsWith("OBJECT_GET"))
                        object_get = inputLine.substring(inputLine.indexOf("http://")).trim();
                    if (inputLine.startsWith("OBJECT_HEAD"))
                        object_head = inputLine.substring(inputLine.indexOf(" ") + 1).trim();
                    if (inputLine.startsWith("OBJECT_MD5"))
                        object_md5 = inputLine.substring(inputLine.indexOf(" ") + 1).trim();
                    if (inputLine.startsWith("OBJECT_LENGTH"))
                        object_length = inputLine.substring(inputLine.indexOf(" ") + 1).trim();
                    if (inputLine.startsWith("OBJECT_URL_EXPIRE"))
                        object_url_expire = inputLine.substring(inputLine.indexOf(" ") + 1).trim();
                    if (inputLine.startsWith("OBJECT_STORAGE_CLASS"))
                        object_storage_class = inputLine.substring(inputLine.indexOf(" ") + 1).trim();
                    if (inputLine.startsWith("END"))
                        paths.add(new String[]{object_get, object_length, object_storage_class});
                }
                in.close();

                if (paths.size() > 0) {
                    for (String[] e : paths) {
                        if (!e[0].toLowerCase().contains("/ota/")) { // filter out tape archive
                            result[0] = e[0];   // GET Url
                            result[1] = e[1];   // Length
                            result[2] = e[2];   // Storage CLass
                            break;              // Pick first non-tape entry
                        }
                    }
                }
            }

            return result;
        } catch (Exception e) {
            System.out.println("Path = " + path);
            System.out.println(e.toString());
            //e.printStackTrace();
        }

        return null;
    }

}