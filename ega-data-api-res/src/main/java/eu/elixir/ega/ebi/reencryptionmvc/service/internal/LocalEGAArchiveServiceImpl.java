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
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;

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
    @Retryable(maxAttempts = 8, backoff = @Backoff(delay = 2000, multiplier = 2))
    @HystrixCommand
    public ArchiveSource getArchiveFile(String id, HttpServletResponse response) {
        ResponseEntity<EgaFile[]> responseEntity = restTemplate.getForEntity(FILEDATABASE_SERVICE + "/file/{fileId}", EgaFile[].class, id);
        EgaFile egaFile = responseEntity.getBody()[0];
        String url = egaFile.getFilePath();
        long size = egaFile.getFileSize();
        String header = egaFile.getHeader();
        try {
            String privateKey = keyService.getPrivateKey(headerFactory.getKeyIds(header.getBytes()).iterator().next()); // select first subkey
            Map.Entry<String, String> parsedHeader = parseHeader(header, privateKey);
            return new ArchiveSource(url, size, null, "aes256", parsedHeader.getKey(), parsedHeader.getValue());
        } catch (IOException | PGPException | BadBlockException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @HystrixCommand
    protected Map.Entry<String, String> parseHeader(String headerString, String key) throws IOException, PGPException, BadBlockException {
        final char[][] passphrase = new char[1][1];
        sharedKey.access(chars -> passphrase[0] = Arrays.copyOf(chars, chars.length));
        Header header = headerFactory.getHeader(headerString.getBytes(), key, passphrase[0]);
        Record record = header.getEncryptedHeader().getRecords().iterator().next();
        Base64.Encoder encoder = Base64.getEncoder();
        return new AbstractMap.SimpleEntry<>(encoder.encodeToString(record.getKey()), encoder.encodeToString(record.getIv()));
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
