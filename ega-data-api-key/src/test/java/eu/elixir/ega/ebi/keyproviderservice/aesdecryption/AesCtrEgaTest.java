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
package eu.elixir.ega.ebi.keyproviderservice.aesdecryption;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.junit.Assert.assertEquals;

public class AesCtrEgaTest {

    @Test
    public void testCipEncryption() throws IOException {
        final byte[] data = "test file.".getBytes();
        final char[] password = "test".toCharArray();
        doEncrypt(data, password);

    }

    private byte[] doEncrypt(byte[] data, char[] password) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStream stream = new AesCtr256Ega().encrypt(password, baos);
        stream.write(data);
        byte[] encryptedData = baos.toByteArray();
        assertEquals(data.length + 16, encryptedData.length);
        return encryptedData;
    }

    @Test
    public void testCipDecryption() throws IOException {
        final String message = "test file.";
        final byte[] data = message.getBytes();
        final char[] password = "test".toCharArray();
        final byte[] encryptedMessage = doEncrypt(data, password);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(encryptedMessage);
        final InputStream decrypt = new AesCtr256Ega().decrypt(inputStream, password);
        byte[] buffer = new byte[16];
        int totalRead = 0;
        int read;
        while ((read = decrypt.read(buffer)) != -1) {
            totalRead += read;
        }
        assertEquals(message.length(), totalRead);
        assertEquals(message, new String(buffer, 0, totalRead));
    }

}