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
package eu.elixir.ega.ebi.shared.config;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;


public class VerifyMessageNew {

    private ArrayList<String> messages = new ArrayList<>();

    /**
     * The constructor of VerifyMessage class retrieves the byte arrays from the
     * File and prints the message only if the signature is verified.
     *
     * @param message the message to verify.
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public VerifyMessageNew(String message) throws Exception {
        // Separate out provided signature
        String[] items = message.split(",");
        byte[] sig = Base64.getDecoder().decode(items[items.length - 1]);

        // Transform list of DatasetIDs to byte array (same as used for signature)
        String sigString = message.substring(0, message.lastIndexOf(",") + 1);
        byte[] lst = (sigString).getBytes();

        String keyFile = "publicKey";
        if (verifySignature(lst, sig, keyFile)) {
            this.messages = new ArrayList<String>(Collections.singletonList(sigString.substring(0, message.lastIndexOf(","))));

        } else {
            System.out.println("NO!!");
        }

    }

    /**
     * Method for signature verification that initializes with the Public Key,
     * updates the data to be verified and then verifies them using the signature.
     *
     * @param data the data to verify.
     * @param signature the expected signature.
     * @param keyFile string description of the keyfile to use.
     * @return true if the signature was verified, false if not.
     * @throws Exception
     */
    private boolean verifySignature(byte[] data, byte[] signature, String keyFile) throws Exception {
        Signature sig = Signature.getInstance("SHA1withRSA");
        sig.initVerify(getPublic(keyFile));
        sig.update(data);

        return sig.verify(signature);
    }

    /**
     * Method to retrieve the Public Key from a file
     *
     * @param filename Filename of the file to load the public key from.
     * @return the public key for the filename.
     * @throws Exception
     */
    private PublicKey getPublic(String filename) throws Exception {
        ClassPathResource cpr = new ClassPathResource(filename);
        byte[] keyBytes = FileCopyUtils.copyToByteArray(cpr.getInputStream());
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }

    /**
     * Returns the list of currently held permissions
     *
     * @return the permission list
     */
    public ArrayList<String> getPermissions() {
        ArrayList<String> permissions = new ArrayList<>();
        for (int i = 0; i < this.messages.size() - 1; i++) {
            permissions.add(this.messages.get(i));
        }
        return permissions; //this.messages;
    }

}
