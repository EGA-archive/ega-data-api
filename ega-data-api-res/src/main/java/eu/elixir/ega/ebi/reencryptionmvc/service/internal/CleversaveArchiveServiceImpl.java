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
package eu.elixir.ega.ebi.reencryptionmvc.service.internal;

import com.google.common.base.Strings;
import com.google.common.io.CountingInputStream;

import eu.elixir.ega.ebi.reencryptionmvc.config.NotFoundException;
import eu.elixir.ega.ebi.reencryptionmvc.config.ServerErrorException;
import eu.elixir.ega.ebi.reencryptionmvc.dto.ArchiveSource;
import eu.elixir.ega.ebi.reencryptionmvc.dto.CachePage;
import eu.elixir.ega.ebi.reencryptionmvc.dto.EgaAESFileHeader;
import eu.elixir.ega.ebi.reencryptionmvc.dto.EgaFile;
import eu.elixir.ega.ebi.reencryptionmvc.service.ArchiveAdapterService;
import eu.elixir.ega.ebi.reencryptionmvc.service.ArchiveService;
import eu.elixir.ega.ebi.reencryptionmvc.service.KeyService;
import eu.elixir.ega.ebi.reencryptionmvc.service.S3Service;
import htsjdk.samtools.seekablestream.cipher.ebi.Glue;
import lombok.extern.slf4j.Slf4j;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.cache2k.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static eu.elixir.ega.ebi.reencryptionmvc.config.Constants.FILEDATABASE_SERVICE;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author asenf
 */
@Service
@Profile("default")
@Primary
@EnableDiscoveryClient
@Slf4j
public class CleversaveArchiveServiceImpl implements ArchiveService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private KeyService keyService;

    @Autowired
    private ArchiveAdapterService archiveAdapterService;
    
    @Autowired
    private Cache<String, EgaAESFileHeader> myHeaderCache; 
    
    @Autowired
    private S3Service s3Service;
    
    @Value("#{base64EncodedCredentials}")
    private String base64EncodedCredentials;

    @Override
    @Cacheable(cacheNames = "archive")
    public ArchiveSource getArchiveFile(String id, HttpServletRequest request, HttpServletResponse response) {
		
		String sessionId= Strings.isNullOrEmpty(request.getHeader("Session-Id"))? "" : request.getHeader("Session-Id") + " ";
        // Get Filename from EgaFile ID - via DATA service (potentially multiple files)
        ResponseEntity<EgaFile[]> forEntity = restTemplate.getForEntity(FILEDATABASE_SERVICE + "/file/{fileId}", EgaFile[].class, id);
        response.setStatus(forEntity.getStatusCodeValue());
        if (forEntity.getStatusCode() != HttpStatus.OK) {
            return null;
        }

        EgaFile[] body = forEntity.getBody();
        String fileName = (body != null && body.length > 0) ? forEntity.getBody()[0].getFileName() : "";
        if ((body == null || body.length == 0)) {
            response.setStatus(forEntity.getStatusCodeValue());
            throw new NotFoundException(sessionId + "Can't obtain File data for ID", id);
        }
        if (fileName.startsWith("/fire")) fileName = fileName.substring(16);
        // Guess Encryption Format from File
        String encryptionFormat = fileName.toLowerCase().endsWith("gpg") ? "symmetricgpg" : "aes256";
        // Get Cleversafe URL from Filename via Fire
        String[] filePath = archiveAdapterService.getPath(fileName, sessionId);
        if (filePath == null || filePath[0] == null) {
            response.setStatus(530);
            throw new ServerErrorException(sessionId + "Fire Error in obtaining URL for ", fileName);
        }

        // Get EgaFile encryption Key
        String encryptionKey = keyService.getFileKey(id);
        if (encryptionKey == null || encryptionKey.length() == 0) {
            response.setStatus(532);
            throw new ServerErrorException(sessionId + "Error in obtaining Archive Key for ", fileName);
        }

        // Build result object and return it (auth is 'null' --> it is part of the URL now)
        return new ArchiveSource(filePath[0], Long.valueOf(filePath[1]), null, encryptionFormat, encryptionKey, null);
    }

    
    public CachePage loadPageCleversafe(int pageSize, String key) {
        
        String[] keys = key.split("\\_");

        String id = keys[0];
        int cachePage = Integer.parseInt(keys[1]);

        if (!myHeaderCache.containsKey(id)) { // Get Header (once in 24h)
            HttpServletRequest request = ((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getRequest();
            HttpServletResponse response = ((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getResponse();

            ArchiveSource source = getArchiveFile(id, request, response);

            // Obtain Signed S3 URL, place in Header Cache
            loadHeaderCleversafe(id, source.getFileUrl(), source.getAuth(), source.getSize(), request, response, source.getEncryptionKey());
            log.info(" --- " + id + " size: " + source.getSize() + " time to load: " + 0);
        } // Get Header - Complete

        HttpClient localHttpclient = HttpClientBuilder.create().build();

        // **
        // ** Load Cache Page [previous - header loaded only once]
        // **
        EgaAESFileHeader header = myHeaderCache.get(id);
        String sourceKey = header.getSourceKey();

        long startCoordinate = (long) cachePage * pageSize; // Account for IV at start of File
        long endCoordinate = startCoordinate + pageSize;
        long fileSize = header.getSize();
        endCoordinate = endCoordinate > fileSize ? fileSize : endCoordinate; // End of file

        // Prepare Request (containd query parameters
        String url = header.getUrl();
        HttpGet request = new HttpGet(url);

        // Add request header for Basic Auth
        request.addHeader("Authorization", "Basic ".concat(base64EncodedCredentials));
        
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
                    log.error("Error page " + key + " attempt " + pageCnt + ": " + th.toString());
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
    
    private void loadHeaderCleversafe(String id, String url, String httpAuth, long fileSize,  HttpServletRequest request_, HttpServletResponse response_, String sourceKey) {
        String sessionId = Strings.isNullOrEmpty(request_.getHeader("Session-Id")) ? ""
                : request_.getHeader("Session-Id") + " ";

        if (url.startsWith("s3")) {
            url = s3Service.getS3ObjectUrl(url);
        }

        // Load first 16 bytes; set stats
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet(url);

        if (httpAuth != null && httpAuth.length() > 0) { // Old: http Auth
            String encoding = java.util.Base64.getEncoder().encodeToString(httpAuth.getBytes());
            String auth = "Basic " + encoding;
            request.addHeader("Authorization", auth);
        } else if (!url.contains("X-Amz")) { // Not an S3 URL - Basic Auth embedded with URL
            request.addHeader("Authorization", "Basic " + base64EncodedCredentials);
        }

        byte[] IV = new byte[16];
        try {
            HttpResponse response = httpclient.execute(request);
            if (response == null || response.getEntity() == null) {
                response_.setStatus(534);
                throw new ServerErrorException(sessionId + "LoadHeader: Error obtaining input stream for ", url);
            }
            DataInputStream content = new DataInputStream(response.getEntity().getContent());
            content.readFully(IV);

            EgaAESFileHeader header = new EgaAESFileHeader(IV, "aes256", fileSize, url, sourceKey);
            myHeaderCache.put(id, header);
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new ServerErrorException(sessionId + "LoadHeader: " + ex.toString() + " :: ", url);
        }
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
