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

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import eu.elixir.ega.ebi.keyproviderservice.config.MyCipherConfig;
import eu.elixir.ega.ebi.keyproviderservice.dto.KeyPath;
import eu.elixir.ega.ebi.keyproviderservice.service.KeyService;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Set;
import org.springframework.context.annotation.Profile;

/**
 * @author asenf
 */
@Service
@Profile("!db")
@EnableDiscoveryClient
public class KeyServiceImplNoDB implements KeyService {

    @Autowired
    private MyCipherConfig myCipherConfig; // Private GPG Key(s)

    @Override
    @HystrixCommand
    @ResponseBody
    public String getFileKey(String id) {
        return myCipherConfig.getFileKey(id);
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
