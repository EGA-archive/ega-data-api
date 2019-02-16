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

import org.bouncycastle.util.io.Streams;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Provides functionality to encrypt files using Alexander's AES flavour.
 */

@Component
public class AesCtr256Ega extends JdkEncryptionAlgorithm {
    private static final int ITERATION_COUNT = 1024;

    private static int KEY_SIZE = 256;

    private static byte[] DEFAULT_SALT = new byte[]{-12, 34, 1, 0, -98, -33, 78, 21};

    private IvParameterSpec ivParameterSpec;

    private SecretKey secretKey;

    @Override
    protected void initializeRead(InputStream inputStream, char[] password) throws IOException {
        byte[] IV = new byte[16];
        Streams.readFully(inputStream, IV);
        ivParameterSpec = new IvParameterSpec(IV);
        secretKey = getKey(password, DEFAULT_SALT);
    }

    @Override
    protected void initializeWrite(char[] password, OutputStream outputStream) throws IOException {
        byte[] randomBytes = new byte[16];
        Random.getSHA1PRNG().nextBytes(randomBytes);
        outputStream.write(randomBytes);
        ivParameterSpec = new IvParameterSpec(randomBytes);
        secretKey = getKey(password, DEFAULT_SALT);
    }

    @Override
    protected Cipher getCipher(int encryptMode) {
        return Encryption.getCipher("AES/CTR/NoPadding", encryptMode, secretKey, ivParameterSpec);
    }

    public static SecretKeySpec getKey(char[] password, byte[] salt) {
        PBEKeySpec pBEKeySpec = new PBEKeySpec(password, salt, ITERATION_COUNT, KEY_SIZE);
        return new SecretKeySpec(Encryption.getSecretKey("PBKDF2WithHmacSHA1", pBEKeySpec).getEncoded(), "AES");
    }

}
