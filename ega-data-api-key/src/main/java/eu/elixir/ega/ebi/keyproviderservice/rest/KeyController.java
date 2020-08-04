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
package eu.elixir.ega.ebi.keyproviderservice.rest;

import eu.elixir.ega.ebi.keyproviderservice.dto.IdFormat;
import eu.elixir.ega.ebi.keyproviderservice.dto.KeyPath;
import eu.elixir.ega.ebi.keyproviderservice.service.KeyService;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author asenf
 */
@RestController
@RequestMapping("/keys")
public class KeyController {

    @Autowired
    private KeyService keyService;

    /*
     * Getting the AES key for a specific file
     */
    @GetMapping(value = "/filekeys/{fileId}")
    @ResponseBody
    public String getFileKey(@PathVariable String fileId) {
        return keyService.getFileKey(fileId);
    }

    @GetMapping(value = "/encryptionalgorithm/{fileId}")
    @ResponseBody
    public String getEncryptionAlgorithm(@PathVariable String fileId) {
        return keyService.getEncryptionAlgorithm(fileId);
    }
    
    /*
     * Getting a Public Key
     * - Getting our own Public Key
     * - Getting a User Public Key, by ID or by Email
     */
    @GetMapping(value = "/retrieve/{keyId}/public")
    @ResponseBody
    public PGPPublicKey getPublicKeyFromPrivate(@PathVariable String keyId,
                                                @RequestParam(required = false, defaultValue = "num") String idFormat) {
        return keyService.getPublicKeyFromPrivate(convertToNumeric(keyId, idFormat));
    }

    @GetMapping(value = "/retrieve/{keyId}/public/{keyType}")
    @ResponseBody
    public String getPublicKey(@PathVariable String keyId,
                               @PathVariable String keyType,
                               @RequestParam(required = false, defaultValue = "num") String idFormat) {
        return keyService.getPublicKey(keyType, convertToNumeric(keyId, idFormat));
    }

    /*
     * Getting a Private Key
     * - Getting the instantiated Object (/object)
     * - Getting the paths to the ASCII Armoured Key and Passphrase (/path)
     * - Getting a re-Armoured String of the Key (/key)
     */
    @GetMapping(value = "/retrieve/{keyId}/private/object")
    @ResponseBody
    public PGPPrivateKey getPrivateKey(@PathVariable String keyId,
                                       @RequestParam(required = false, defaultValue = "num") String idFormat) {
        return keyService.getPrivateKey(convertToNumeric(keyId, idFormat));
    }

    @GetMapping(value = "/retrieve/{keyId}/private/path")
    @ResponseBody
    public KeyPath getPrivateKeyPath(@PathVariable String keyId,
                                     @RequestParam(required = false, defaultValue = "num") String idFormat) {
        return keyService.getPrivateKeyPath(convertToNumeric(keyId, idFormat));
    }

    @GetMapping(value = "/retrieve/{keyId}/private/bin")
    @ResponseBody
    public byte[] getPrivateKeyByte(@PathVariable String keyId,
                                    @RequestParam(required = false, defaultValue = "num") String idFormat) {
        return keyService.getPrivateKeyByte(convertToNumeric(keyId, idFormat));
    }

    @GetMapping(value = "/retrieve/{keyId}/private/key")
    @ResponseBody
    public String getPrivateKeyString(@PathVariable String keyId,
                                      @RequestParam(required = false, defaultValue = "num") String idFormat) {
        return keyService.getPrivateKeyString(convertToNumeric(keyId, idFormat));
    }

    /*
     * Return all current Key IDs
     */
    @GetMapping(value = "/retrieve/{keyType}/ids")
    @ResponseBody
    public Set<String> getPublicKey(@PathVariable String keyType,
                                    @RequestParam(required = false, defaultValue = "num") String idFormat) {
        if (IdFormat.NUM == IdFormat.valueOf(idFormat.toUpperCase())) {
            return keyService.getKeyIDs(keyType).stream().map(String::valueOf).collect(Collectors.toSet());
        }
        return keyService.getKeyIDs(keyType).stream().map(Long::toHexString).collect(Collectors.toSet());
    }

    private String convertToNumeric(String keyId, String idFormat) {
        if (IdFormat.NUM == IdFormat.valueOf(idFormat.toUpperCase())) {
            return keyId;
        }
        return String.valueOf(new BigInteger(keyId, 16).longValue());
    }

}
