/*
 * Copyright 2018 ELIXIR EGA
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
package eu.elixir.ega.ebi.keyproviderservice.service;

import eu.elixir.ega.ebi.keyproviderservice.dto.KeyPath;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;

import java.util.Set;

/**
 * @author asenf
 */
public interface KeyService {

    String getFileKey(String fileId);

    PGPPrivateKey getPrivateKey(String keyId);

    KeyPath getPrivateKeyPath(String keyId);

    String getPrivateKeyString(String keyId);

    PGPPublicKey getPublicKeyFromPrivate(String keyId);

    String getPublicKey(String keyType, String keyId);

    Set<Long> getKeyIDs(String keyType);

}
