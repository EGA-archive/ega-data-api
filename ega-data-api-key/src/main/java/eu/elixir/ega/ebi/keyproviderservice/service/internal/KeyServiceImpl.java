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
package eu.elixir.ega.ebi.keyproviderservice.service.internal;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.Set;

import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseBody;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

import eu.elixir.ega.ebi.keyproviderservice.aesdecryption.AesCtr256Ega;
import eu.elixir.ega.ebi.keyproviderservice.config.MyCipherConfig;
import eu.elixir.ega.ebi.keyproviderservice.domain.file.entity.FileKey;
import eu.elixir.ega.ebi.keyproviderservice.domain.file.repository.FileKeyRepository;
import eu.elixir.ega.ebi.keyproviderservice.domain.key.entity.EncryptionKey;
import eu.elixir.ega.ebi.keyproviderservice.domain.key.repository.EncryptionKeyRepository;
import eu.elixir.ega.ebi.keyproviderservice.dto.KeyPath;
import eu.elixir.ega.ebi.keyproviderservice.service.KeyService;

/**
 * @author asenf
 */
@Service
@Profile("db")
@EnableDiscoveryClient
public class KeyServiceImpl implements KeyService {

    @Autowired
    private MyCipherConfig myCipherConfig; // Private GPG Key(s)

    @Autowired
    private EncryptionKeyRepository encryptionKeyRepository; // Per-File AES Keys

    @Autowired
    private FileKeyRepository fileKeyRepository;

    @Value("${ega.key.dbPasswordDecryptKey}")
    private String dbPasswordDecryptKey;

    @Override
    @HystrixCommand
    @ResponseBody
    public String getFileKey(String id) {
        String key = "";
        try {
            Iterable<FileKey> fileKeys = fileKeyRepository.findByFileId(id);

            if (fileKeys.iterator().hasNext()) {
                FileKey fileKey = fileKeys.iterator().next();
                EncryptionKey encryptionKey = encryptionKeyRepository.findById(fileKey.getEncryptionKeyId());

                ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(encryptionKey.getEncryptionKey()));
                final InputStream decrypt = new AesCtr256Ega().decrypt(inputStream, dbPasswordDecryptKey.toCharArray());
                byte[] buffer = new byte[1024];
                int totalRead = 0;
                int read;
                while ((read = decrypt.read(buffer)) != -1) {
                    totalRead += read;
                }

                key = new String(buffer, 0, totalRead);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return key;
    }

    @Override
    @HystrixCommand
    @ResponseBody
    public PGPPrivateKey getPrivateKey(String keyId) {
        long lKeyId = Long.parseLong(keyId);
        return myCipherConfig.getPrivateKey(lKeyId);
    }

    @Override
    @HystrixCommand
    @ResponseBody
    public KeyPath getPrivateKeyPath(String keyId) {
        long lKeyId = Long.parseLong(keyId);
        return myCipherConfig.getKeyPaths(lKeyId);
    }

    @Override
    @HystrixCommand
    @ResponseBody
    public byte[] getPrivateKeyByte(String keyId) {
        long lKeyId = Long.parseLong(keyId);
        return myCipherConfig.getBinaryKey(lKeyId);
    }

    @Override
    @HystrixCommand
    @ResponseBody
    public String getPrivateKeyString(String keyId) {
        long lKeyId = Long.parseLong(keyId);
        return myCipherConfig.getAsciiArmouredKey(lKeyId);
    }

    @Override
    @HystrixCommand
    @ResponseBody
    public String getPublicKey(String keyType, String keyId) {
        if (keyType.equalsIgnoreCase("id")) {
            return myCipherConfig.getPublicKeyById(keyId);
        } else if (keyType.equalsIgnoreCase("email")) {
            return myCipherConfig.getPublicKeyByEmail(keyId);
        }
        return null;
    }

    @Override
    @HystrixCommand
    @ResponseBody
    public PGPPublicKey getPublicKeyFromPrivate(String keyId) {
        return myCipherConfig.getPublicKey(Long.parseLong(keyId));
    }

    @Override
    @HystrixCommand
    @ResponseBody
    public Set<Long> getKeyIDs(String keyType) {
        return this.myCipherConfig.getKeyIDs();
    }

}
