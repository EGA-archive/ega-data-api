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
package eu.elixir.ega.ebi.reencryptionmvc.cache2k;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;

import com.google.common.io.CountingInputStream;
import com.google.gson.Gson;

import eu.elixir.ega.ebi.reencryptionmvc.dto.ArchiveSource;
import eu.elixir.ega.ebi.reencryptionmvc.dto.CachePage;
import eu.elixir.ega.ebi.reencryptionmvc.dto.EgaAESFileHeader;
import eu.elixir.ega.ebi.reencryptionmvc.dto.EgaFile;
import eu.elixir.ega.ebi.reencryptionmvc.exception.ServerErrorException;
import eu.elixir.ega.ebi.reencryptionmvc.util.FireCommons;
import eu.elixir.ega.ebi.reencryptionmvc.util.S3Commons;
import htsjdk.samtools.seekablestream.cipher.ebi.Glue;
import lombok.extern.slf4j.Slf4j;

/**
 * @author asenf
 */
@Slf4j
public class My2KCachePageFactory implements FactoryBean<Cache<String, CachePage>> { // extends SimpleJdbcDaoSupport

    private Cache<String, EgaAESFileHeader> myHeaderCache;
    private final int pageSize;
    private final int pageCount;
    private final LoadBalancerClient loadBalancer;
    private final FireCommons fireCommons;
    private final S3Commons s3Commons;

    public My2KCachePageFactory(Cache<String, EgaAESFileHeader> myHeaderCache, LoadBalancerClient loadBalancer,
            int pageSize, int pageCount, FireCommons fireCommons, S3Commons s3Commons) {
        this.myHeaderCache = myHeaderCache;
        this.loadBalancer = loadBalancer;
        this.pageSize = pageSize;
        this.pageCount = pageCount;
        this.fireCommons = fireCommons;
        this.s3Commons = s3Commons;
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
        String[] keys = key.split("\\_");

        String id = keys[0];
        int cachePage = Integer.parseInt(keys[1]);

        String httpAuth = "";
        String sourceKey;
        if (!myHeaderCache.containsKey(id)) { // Get Header (once in 24h)
            EgaFile[] files;
            String encryptionKey;
            try(CloseableHttpClient localHttpclient = HttpClientBuilder.create().build()) {
                files = getEgaFile(id, key, localHttpclient);
                encryptionKey = getEncryptionKey(id, key, localHttpclient);

            } catch (Exception ex) {
                throw new ServerErrorException("Error Loading Cache Header", key);
            }

            ArchiveSource source = new ArchiveSource(files[0].getFileName(), files[0].getFileSize(), "", "aes256", encryptionKey, null);
            sourceKey = source.getEncryptionKey();

            String fileLocation = source.getFileUrl();
            httpAuth = source.getAuth();
            long fileSize = source.getSize();

            // Obtain Signed S3 URL, place in Header Cache
            myHeaderCache.put(id, getFileEncryptionHeader(id, fileLocation, httpAuth, fileSize, sourceKey)
                    .orElseThrow(()-> new ServerErrorException("Header could not be loaded", key)));
            log.info(" --- " + id + " size: " + fileSize + " time to load: " + 0); // dt);
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
        request.addHeader("Authorization", "Basic ".concat(fireCommons.getBase64EncodedCredentials()));

        // Add range header - logical (unencrypted) coordinates to file coordinates (add IV handling '+16')
        if ((startCoordinate + 16) >= header.getSize())
            return new CachePage(new byte[]{});

        String byteRange = "bytes=" + (startCoordinate + 16) + "-" + (endCoordinate + 16);
        request.addHeader("Range", byteRange);
        long pageSize_ = ((endCoordinate + 16) > header.getSize() ? header.getSize() : (endCoordinate + 16)) - (startCoordinate + 16);
        pageSize_ = pageSize > pageSize_ ? pageSize_ : pageSize;

        byte[] buffer = new byte[(int) pageSize_];
        byte[] decrypted;
        try(CloseableHttpClient localHttpclient = HttpClientBuilder.create().build()) {
            // Attemp loading page 3 times (mask object store read errors)
            int pageCnt = 0;
            boolean pageSuccess;
            do {
                try (CloseableHttpResponse response = localHttpclient.execute(request)) {

                    if (response.getStatusLine().getStatusCode() != 200
                            && response.getStatusLine().getStatusCode() != 206)
                        throw new ServerErrorException(
                                "FIRE error Loading Cache Page Code " + response.getStatusLine().getStatusCode() + " for ",
                                key);

                    // Read response from HTTP call, count bytes read (encrypted Data)
                    try (CountingInputStream cIn = new CountingInputStream(response.getEntity().getContent());
                         DataInputStream dis = new DataInputStream(cIn);) {
                        dis.readFully(buffer);
                        pageSuccess = true;
                    }
                } catch (Throwable th) {
                    pageSuccess = false;
                    log.error("FIRE error page " + key + " attempt " + pageCnt + ": " + th.toString());
                }
            } while (!pageSuccess && pageCnt++ < 3);

            // Decrypt, store plain in cache
            byte[] newIV = new byte[16]; // IV always 16 bytes long
            System.arraycopy(header.getIV(), 0, newIV, 0, 16); // preserved start value
            if (startCoordinate > 0) byteIncrementFast(newIV, startCoordinate);
            decrypted = decrypt(buffer, sourceKey, newIV);
        } catch (UnsupportedOperationException th) {
            log.error("HTTP GET ERROR -1 " + th.toString() + "   -- " + byteRange + "\n" + url);
            throw new ServerErrorException("Error Loading Cache Page -1 " + th.toString() + " for ", key);
        } catch (IOException th) {
            log.error("HTTP GET ERROR 0 " + th.toString() + "   -- " + byteRange + "\n" + url);
            throw new ServerErrorException("Error Loading Cache Page 0 " + th.toString() + " for ", key);
        } catch (IllegalBlockSizeException ex) {
            log.error("HTTP GET ERROR 1 " + ex.toString() + "   -- " + byteRange + "\n" + url);
            throw new ServerErrorException("Error Loading Cache Page 1 " + ex.toString() + " for ", key);
        } catch (BadPaddingException ex) {
            log.error("HTTP GET ERROR 2 " + ex.toString() + "   -- " + byteRange + "\n" + url);
            throw new ServerErrorException("Error Loading Cache Page 2 " + ex.toString() + " for ", key);
        } catch (Exception ex) {
            log.error("HTTP GET ERROR 3 " + ex.toString() + "   -- " + byteRange + "\n" + url);
            throw new ServerErrorException("Error Loading Cache Page 3 " + ex.toString() + " for ", key);
        } finally {
            request.releaseConnection();
        }

        return new CachePage(decrypted);
    }

    private String getEncryptionKey(String id, String key, HttpClient localHttpclient)
            throws IOException, ClientProtocolException {
        String keyServerURL = getServiceURL("KEYSERVER");
        HttpGet keyRequest = new HttpGet(keyServerURL + "/keys/filekeys/" + id);

        // encryptionKey
        HttpResponse keyResponse = localHttpclient.execute(keyRequest);
        if (keyResponse.getEntity() == null) {
            throw new ServerErrorException("Error Attempting to Load Cache Header key ", key);
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(keyResponse.getEntity().getContent()));
        String encryptionKey = reader.readLine().trim();
        reader.close();
        if (encryptionKey == null || encryptionKey.length() == 0) {
            throw new ServerErrorException("Error Loading Cache Header File key", key);
        }
        return encryptionKey;
    }

    private EgaFile[] getEgaFile(String id, String key, HttpClient localHttpclient)
            throws IOException, ClientProtocolException {
        String fileDatabaseURL = getServiceURL("FILEDATABASE");
        HttpGet sourceRequest = new HttpGet(fileDatabaseURL + "/file/" + id);
        
        // EgaFile
        HttpResponse sourceResponse = localHttpclient.execute(sourceRequest);
        if (sourceResponse.getEntity() == null)
            throw new ServerErrorException("Error Attempting to Load Cache Header File data ", key);
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(sourceResponse.getEntity().getContent()));
        Gson gson = new Gson();
        EgaFile[] files = gson.fromJson(reader, EgaFile[].class);
        reader.close();
        if (files == null || files.length == 0) {
            throw new ServerErrorException("Error Loading Cache Header File data", key);
        }
        return files;
    }

    private Optional<EgaAESFileHeader> getFileEncryptionHeader(String id, String path, String httpAuth, long fileSize, String sourceKey) {
        String url;
        
        HttpGet request;

        if (path.startsWith("s3")) {
            url = s3Commons.getS3ObjectUrl(path);
            request = new HttpGet(url);
        } else {
            url = fireCommons.getFireObjectUrl(path);
            request = new HttpGet(url);
            fireCommons.addAuthenticationForFireRequest(httpAuth, url, request);
        }

        byte[] IV = new byte[16];
        try (   CloseableHttpClient httpclient = HttpClientBuilder.create().build();
                CloseableHttpResponse response = httpclient.execute(request);
                DataInputStream content = new DataInputStream(response.getEntity().getContent());) {
            content.readFully(IV);
            return Optional.of(new EgaAESFileHeader(IV, "aes256", fileSize, url, sourceKey));        
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
            return Optional.empty();
        }
    }

    /*
     * Decryption Function
     */
    private byte[] decrypt(byte[] cipherText, String encryptionKey, byte[] IV) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
        SecretKey key_ = Glue.getInstance().getKey(encryptionKey.toCharArray(), 256);
        cipher.init(Cipher.DECRYPT_MODE, key_, new IvParameterSpec(IV));
        return cipher.doFinal(cipherText);
    }

}