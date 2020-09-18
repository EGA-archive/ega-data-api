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

import com.google.common.io.CountingInputStream;
import com.google.gson.Gson;
import eu.elixir.ega.ebi.reencryptionmvc.dto.ArchiveSource;
import eu.elixir.ega.ebi.reencryptionmvc.dto.EgaAESFileHeader;
import eu.elixir.ega.ebi.reencryptionmvc.dto.EgaFile;
import eu.elixir.ega.ebi.reencryptionmvc.exception.ServerErrorException;
import eu.elixir.ega.ebi.reencryptionmvc.util.DecryptionUtils;
import eu.elixir.ega.ebi.reencryptionmvc.util.FireCommons;
import eu.elixir.ega.ebi.reencryptionmvc.util.S3Commons;
import htsjdk.samtools.seekablestream.cipher.ebi.Glue;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.cache2k.Cache;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;

/**
 * @author asenf
 */
@Slf4j
public class My2KCachePageFactory {
    private final CloseableHttpClient httpClient;
    private Cache<String, EgaAESFileHeader> myHeaderCache;
    private final int pageSize;
    private final LoadBalancerClient loadBalancer;
    private final FireCommons fireCommons;
    private final S3Commons s3Commons;

    public My2KCachePageFactory(final CloseableHttpClient httpClient, Cache<String, EgaAESFileHeader> myHeaderCache, LoadBalancerClient loadBalancer,
                                int pageSize, FireCommons fireCommons, S3Commons s3Commons) {
        this.httpClient = httpClient;
        this.myHeaderCache = myHeaderCache;
        this.loadBalancer = loadBalancer;
        this.pageSize = pageSize;
        this.fireCommons = fireCommons;
        this.s3Commons = s3Commons;
    }

    private String getServiceURL(String service) {
        return loadBalancer.choose(service).getUri().toString();
    }

    public byte[] downloadPage(String id, int cachePage) throws IOException {
        String sourceKey;
        EgaAESFileHeader header = getEgaAESFileHeader(id);
        sourceKey = header.getSourceKey();

        long startCoordinate = (long) cachePage * pageSize; // Account for IV at start of File
        long endCoordinate = startCoordinate + pageSize;
        long fileSize = header.getSize();
        endCoordinate = endCoordinate > fileSize ? fileSize : endCoordinate; // End of file

        HttpGet request = new HttpGet(header.getUrl());
        request.addHeader("Authorization", "Basic ".concat(fireCommons.getBase64EncodedCredentials()));

        // Add range header - logical (unencrypted) coordinates to file coordinates (add IV handling '+16')
        if ((startCoordinate + 16) >= header.getSize())
            return new byte[]{};

        String byteRange = "bytes=" + (startCoordinate + 16) + "-" + (endCoordinate + 16);
        request.addHeader("Range", byteRange);
        long pageSize_ = ((endCoordinate + 16) > header.getSize() ? header.getSize() : (endCoordinate + 16)) - (startCoordinate + 16);
        pageSize_ = pageSize > pageSize_ ? pageSize_ : pageSize;

        byte[] buffer = new byte[(int) pageSize_];
            int pageCnt = 0;
            boolean pageSuccess = false;
            do {
                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    if (response.getStatusLine().getStatusCode() != 200
                            && response.getStatusLine().getStatusCode() != 206) {
                        log.error("FIRE error loading Cache Page Code "
                                + response.getStatusLine().getStatusCode()
                                + " for id '" + id + "' page '" + cachePage + "'");
                        continue;
                    }

                    // Read response from HTTP call, count bytes read (encrypted Data)
                    try (CountingInputStream cIn = new CountingInputStream(response.getEntity().getContent());
                         DataInputStream dis = new DataInputStream(cIn);) {
                        dis.readFully(buffer);
                        pageSuccess = true;
                    }
                } catch (Throwable th) {
                    log.error("FIRE error loading Cache Page Code  for id '" + id + "' page '" + cachePage
                            + "' attempt '" + pageCnt + "' ", th);
                }
            } while (!pageSuccess && pageCnt++ < 3);

            if(!pageSuccess) {
                throw new ServerErrorException("FIRE error can't read data, file id " + id + " ,page " +cachePage);
            }
            
            // Decrypt, store plain in cache
            try {
                byte[] newIV = new byte[16]; // IV always 16 bytes long
                System.arraycopy(header.getIV(), 0, newIV, 0, 16); // preserved start value
                if (startCoordinate > 0) DecryptionUtils.byteIncrementFast(newIV, startCoordinate);
                return decrypt(buffer, sourceKey, newIV);
            } catch (Exception ex) {
                log.error("Error decrypting '" + byteRange + "' id '" + id + "' " + ex.getMessage(), ex);
                throw new ServerErrorException("Error decrypting '" + byteRange + "' id '" + id + "' " + ex.getMessage(), ex);
            } finally {
                request.releaseConnection();
            }
    }

    private EgaAESFileHeader getEgaAESFileHeader(String id) throws IOException {
        if (!myHeaderCache.containsKey(id)) { // Get Header (once in 24h)
            EgaFile[] files;
            String encryptionKey;
            files = getEgaFile(id);
            encryptionKey = getEncryptionKey(id);

            ArchiveSource source = new ArchiveSource(files[0].getFileName(), files[0].getFileSize(), "", "aes256", encryptionKey, null);
            String sourceKey = source.getEncryptionKey();

            String fileLocation = source.getFileUrl();
            String httpAuth = source.getAuth();
            long fileSize = source.getSize();

            // Obtain Signed S3 URL, place in Header Cache
            myHeaderCache.put(id, getFileEncryptionHeader(fileLocation, httpAuth, fileSize, sourceKey)
                    .orElseThrow(() -> new ServerErrorException("Header could not be loaded, File id", id)));
            log.info(" --- " + id + " size: " + fileSize + " time to load: " + 0); // dt);
        }
        return myHeaderCache.get(id);
    }

    private String getEncryptionKey(String id) throws IOException {
        String keyServerRequestUri = getServiceURL("KEYSERVER") + "/keys/filekeys/" + id;
        HttpGet keyRequest = new HttpGet(keyServerRequestUri);

        // encryptionKey
        try (CloseableHttpResponse response = httpClient.execute(keyRequest)) {
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new ServerErrorException("Retrieve header at: '" + keyServerRequestUri + "' returned error code: '" +
                        response.getStatusLine().getStatusCode() + "' File id", id);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String encryptionKey = reader.readLine().trim();
            reader.close();
            if (encryptionKey == null || encryptionKey.length() == 0) {
                throw new ServerErrorException("Retrieved header at: '" + keyServerRequestUri + "' is empty. File id", id);
            }
            return encryptionKey;
        } catch (IOException ex) {
            throw new ServerErrorException("Error Retrieve header File data", id);
        }
    }

    private EgaFile[] getEgaFile(String id) throws IOException {
        String fileDatabaseURL = getServiceURL("FILEDATABASE");
        HttpGet sourceRequest = new HttpGet(fileDatabaseURL + "/file/" + id);

        // EgaFile
        try (CloseableHttpResponse sourceResponse = httpClient.execute(sourceRequest)) {
            if (sourceResponse.getEntity() == null)
                throw new ServerErrorException("Error Attempting to Load Cache Header File data ", id);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(sourceResponse.getEntity().getContent()));
            Gson gson = new Gson();
            EgaFile[] files = gson.fromJson(reader, EgaFile[].class);
            reader.close();
            if (files == null || files.length == 0) {
                throw new ServerErrorException("Error Loading Cache Header File data", id);
            }
            return files;
        } catch (IOException ex) {
            throw new ServerErrorException("Error Loading Cache Header File data", id);
        }
    }

    private Optional<EgaAESFileHeader> getFileEncryptionHeader(String path, String httpAuth, long fileSize, String sourceKey) {
        String url;
        HttpGet request;
        if (path.startsWith("s3")) {
            url = s3Commons.getS3ObjectUrl(path);
            request = new HttpGet(url);
        } else {
            url = fireCommons.getFireObjectUrl(path);
            request = new HttpGet(url);
            fireCommons.addAuthenticationForFireRequest(httpAuth, url, request);
            request.addHeader("Range", "bytes=0-16");
        }

        byte[] IV = new byte[16];
        try (CloseableHttpResponse response = httpClient.execute(request);
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