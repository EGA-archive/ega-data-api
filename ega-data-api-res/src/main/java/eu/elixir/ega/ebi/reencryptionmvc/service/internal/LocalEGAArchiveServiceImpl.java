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
package eu.elixir.ega.ebi.reencryptionmvc.service.internal;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import eu.elixir.ega.ebi.reencryptionmvc.dto.ArchiveSource;
import eu.elixir.ega.ebi.reencryptionmvc.dto.EgaFile;
import eu.elixir.ega.ebi.reencryptionmvc.service.ArchiveService;
import eu.elixir.ega.ebi.reencryptionmvc.service.KeyService;
import no.ifi.uio.crypt4gh.factory.HeaderFactory;
import no.ifi.uio.crypt4gh.pojo.Header;
import no.ifi.uio.crypt4gh.pojo.Record;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.jcajce.provider.util.BadBlockException;
import org.bouncycastle.openpgp.PGPException;
import org.identityconnectors.common.security.GuardedString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

import static eu.elixir.ega.ebi.shared.Constants.FILEDATABASE_SERVICE;

/**
 * @author asenf
 */
@Service
@Profile("LocalEGA")
@Primary
@EnableDiscoveryClient
public class LocalEGAArchiveServiceImpl implements ArchiveService {

    private RestTemplate restTemplate;
    private KeyService keyService;
    private HeaderFactory headerFactory;
    private GuardedString sharedKey;

    @Override
    //@Retryable(maxAttempts = 8, backoff = @Backoff(delay = 2000, multiplier = 2))
    @HystrixCommand
    public ArchiveSource getArchiveFile(Integer id, HttpServletResponse response) {
        ResponseEntity<EgaFile[]> responseEntity = restTemplate.getForEntity(FILEDATABASE_SERVICE + "/file/{fileId}", EgaFile[].class, id);
        System.out.println(FILEDATABASE_SERVICE + "/file/" + id);
        EgaFile egaFile = responseEntity.getBody()[0];
        String url = egaFile.getFileId().toString();
        System.out.println(egaFile.getFileName());
        long size = egaFile.getFileSize();
        System.out.println(egaFile.getFileSize());
        String header = egaFile.getHeader();
        System.out.println("Header" + header.getBytes());
        try {
            byte[] headerBytes = Hex.decodeHex(header.toCharArray());
            Collection<String> keyIds = headerFactory.getKeyIds(headerBytes);
            System.out.println("Header" + keyIds);
            String actual_key = new BigInteger(Hex.decodeHex(keyIds.iterator().next().toCharArray())).toString();
            System.out.println("actual_key" + actual_key);
            String privateKey = keyService.getPrivateKey(actual_key); // select first subkey
            System.out.println(privateKey);
            Map.Entry<String, String> parsedHeader = parseHeader(headerBytes, privateKey);
            System.out.println(parsedHeader.getKey());
            System.out.println(parsedHeader.getValue());
            return new ArchiveSource(url, size, null, "aes256", parsedHeader.getKey(), parsedHeader.getValue());
        } catch (IOException | PGPException | BadBlockException | DecoderException e) {
            System.out.println(e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @HystrixCommand
    protected Map.Entry<String, String> parseHeader(byte[] headerBytes, String key) throws IOException, PGPException, BadBlockException {
        final char[][] passphrase = new char[1][1];
        sharedKey.access(chars -> passphrase[0] = Arrays.copyOf(chars, chars.length));
        System.out.println(passphrase[0]);
        System.out.println(headerBytes);
        System.out.println(key);
        Header header = headerFactory.getHeader(headerBytes, key, passphrase[0]);
        Record record = header.getEncryptedHeader().getRecords().iterator().next();
        //Base64.Encoder encoder = Base64.getEncoder();
        return new AbstractMap.SimpleEntry<>(Hex.encodeHexString(record.getKey()), Hex.encodeHexString(record.getIv()));
    }

    @Autowired
    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Autowired
    public void setKeyService(KeyService keyService) {
        this.keyService = keyService;
    }

    @Autowired
    public void setHeaderFactory(HeaderFactory headerFactory) {
        this.headerFactory = headerFactory;
    }

    @Autowired
    public void setSharedKey(GuardedString sharedKey) {
        this.sharedKey = sharedKey;
    }

}
