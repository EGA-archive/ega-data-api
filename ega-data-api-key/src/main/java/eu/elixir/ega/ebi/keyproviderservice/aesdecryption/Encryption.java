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
package eu.elixir.ega.ebi.keyproviderservice.aesdecryption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

public class Encryption {

    private final static Logger logger = LoggerFactory.getLogger(Encryption.class);

    /**
     * Returns configured cipher following the specification provided. This method is only recommended for known
     * working specifications.
     *
     * @param algorithm
     * @param encryptMode
     * @param secretKey
     * @param ivParameterSpec
     * @return Cipher
     * @throws AssertionError if specification is invalid or algorithm could not be found
     */
    public static Cipher getCipher(String algorithm, int encryptMode, SecretKey secretKey,
                                   IvParameterSpec ivParameterSpec) {
        try {
            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(encryptMode, secretKey, ivParameterSpec);
            return cipher;
        } catch (NoSuchPaddingException | InvalidAlgorithmParameterException | NoSuchAlgorithmException |
                InvalidKeyException e) {
            logger.error(e.getMessage(), e);
            throw new AssertionError(e);
        }
    }

    /**
     * Returns a secret key according algorithm and key specifications. This method is only recommended for known
     * working algorithm/specification pairs
     * @param algorithm
     * @param keySpec
     * @return
     * @throws AssertionError if algorithm could not be instanced or specification is not valid.
     */
    public static SecretKey getSecretKey(String algorithm, KeySpec keySpec) {
        try {
            return SecretKeyFactory.getInstance(algorithm).generateSecret(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            logger.error(e.getMessage(), e);
            throw new AssertionError(e);
        }
    }

}
