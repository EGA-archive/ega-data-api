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
package eu.elixir.ega.ebi.reencryptionmvc.service;

import eu.elixir.ega.ebi.reencryptionmvc.dto.KeyPath;
import org.springframework.cache.annotation.Cacheable;

/**
 * @author asenf
 */
@Cacheable
public interface KeyService {

    String getFileKey(String fileId);

    KeyPath getKeyPath(String key);

    String getPublicKey(String id);

    String getPrivateKey(String id);

    String getEncryptionAlgorithm(String id);
}
