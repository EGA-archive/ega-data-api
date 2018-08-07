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
package eu.elixir.ega.ebi.keyproviderservice.config;

import eu.elixir.ega.ebi.keyproviderservice.dto.KeyPath;
import eu.elixir.ega.ebi.keyproviderservice.dto.PublicKey;
import org.bouncycastle.bcpg.ArmoredInputStream;
import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.bouncycastle.openpgp.operator.PGPDigestCalculatorProvider;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.security.Security;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.tomcat.util.http.fileupload.util.Streams;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.openpgp.operator.KeyFingerPrintCalculator;
import org.bouncycastle.openpgp.operator.PGPDigestCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyEncryptorBuilder;
import org.identityconnectors.common.security.GuardedString;

/**
 * @author asenf
 */
public class MyCipherConfig {

    @Autowired
    private RestTemplate restTemplate;

    // Actual Key objects (not sure if this is necessary)
    private HashMap<Long, PGPPrivateKey> pgpPrivateKeys = new HashMap<>();

    // Paths
    private HashMap<Long, KeyPath> keyPaths = new HashMap<>();
    
    // Re-Armoured Key String
    private HashMap<Long, String> armouredKey = new HashMap<>();

    // Public Key URL
    // https://github.com/mailvelope/keyserver/blob/master/README.md
    private String publicKeyUrl;

    // Actual keys - keep locked in encrypted memory
    private GuardedString sharedKey;
    private GuardedString egaLegacy;
    
    // Load (1) Private Key
    public MyCipherConfig(String[] keyPath, 
                          String[] keyPassPath, 
                          String sharedKeyPath,
                          String publicKeyUrl, 
                          String egaLegacyPath) {
        this.publicKeyUrl = publicKeyUrl;

        if (keyPath == null) {
            return;
        }

        // Shared Key between Services (implemetation using char array)
        if (sharedKeyPath!=null && sharedKeyPath.length()>0) {
            char[] buf = new char[128]; // limit password length to 128 characters
            try {
                int cnt = readCharArray(buf, sharedKeyPath);
                this.sharedKey = new GuardedString( Arrays.copyOfRange(buf, 0, cnt) );
            } catch (IOException ex) {
                Logger.getLogger(MyCipherConfig.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        // Get Key ID and store both Key paths and Key objects in a Hash Map
        for (int i = 0; i < keyPath.length; i++) {
            try {
                String keyAsString = readFileAsString(keyPath[i]).trim();
                PGPPublicKey pgpPublicKey = extractPublicKey(keyAsString);
                
                PGPPrivateKey pgpPrivateKey = extractKey(keyAsString, readFileAsString(keyPassPath[i]).trim());
                long keyId = pgpPrivateKey.getKeyID();
                // Store the Key Object
                pgpPrivateKeys.put(keyId, pgpPrivateKey);
                // Store the set of Paths to Key and Passphrase
                keyPaths.put(keyId, new KeyPath(keyPath[i], keyPassPath[i].trim()));
                // Store Re-Armoured Key String
                String reArmouredKey = reArmourKey(pgpPublicKey, pgpPrivateKey);
                armouredKey.put(keyId, reArmouredKey);
            } catch (IOException ex) {
                Logger.getLogger(MyCipherConfig.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        // EGA Legacy Code (implemetation using scanner)
        if (egaLegacyPath!=null && egaLegacyPath.length()>0) {
            try {
                FileInputStream fis = new FileInputStream(egaLegacyPath);
                Scanner scanner = new Scanner(fis);
                if (scanner.hasNextLine()) {                    
                    this.egaLegacy = new GuardedString(scanner.nextLine().toCharArray());
                }
            } catch (FileNotFoundException ex) {
                Logger.getLogger(MyCipherConfig.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
            
    }

    /*
     * Accessors - Private Key, and Paths - Used by REST endpoint
     */
    public PGPPrivateKey getPrivateKey(Long keyId) {
        return this.pgpPrivateKeys.get(keyId);
    }

    public PGPPublicKey getPublicKey(Long keyId) {
        PGPPublicKey key = null;
        try {
            key = new PGPPublicKey(this.pgpPrivateKeys.get(keyId).getPublicKeyPacket(), new JcaKeyFingerprintCalculator());
        } catch (PGPException ex) {
            Logger.getLogger(MyCipherConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
        return key;
    }

    public KeyPath getKeyPaths(Long keyId) {
        return this.keyPaths.get(keyId);
    }

    public String getAsciiArmouredKey(long keyId) {
        return this.armouredKey.get(keyId);
    }
    
    public Set<Long> getKeyIDs() {
        return this.pgpPrivateKeys.keySet();
    }

    // https://github.com/mailvelope/keyserver/blob/master/README.md
    public String getPublicKeyById(String id) {
        ResponseEntity<PublicKey> publicKey = restTemplate.getForEntity(this.publicKeyUrl + "?id=" + id, PublicKey.class);
        return publicKey.getBody().getPublicKeyArmored();
    }

    // https://github.com/mailvelope/keyserver/blob/master/README.md
    public String getPublicKeyByEmail(String email) {
        ResponseEntity<PublicKey> publicKey = restTemplate.getForEntity(this.publicKeyUrl + "?email=" + email, PublicKey.class);
        return publicKey.getBody().getPublicKeyArmored();
    }

    public String getFileKey(String fileId) {
        final StringBuilder clearKey = new StringBuilder();
        egaLegacy.access(new GuardedString.Accessor() {
            @Override
            public void access(final char[] clearChars) {
                clearKey.append(clearChars);
            }
        });
        return clearKey.toString();
    }
    /*
     * Utility Functions
     */
    private PGPPublicKey extractPublicKey(String sKey) {
        try {
            PGPSecretKey secretKey = getSecretKey(sKey);
            return secretKey.getPublicKey();
        } catch (IOException | PGPException ex) {
            System.out.println(ex.toString());
        }
       return null;
    }
    
    public PGPPrivateKey extractKey(String sKey, String sPass) {
        PGPPrivateKey key = null;

        try {
            PGPSecretKey secretKey = getSecretKey(sKey);

            // Extract Private Key from Secret Key
            PGPDigestCalculatorProvider digestCalc = new JcaPGPDigestCalculatorProviderBuilder().build();
            PBESecretKeyDecryptor decryptor = new JcePBESecretKeyDecryptorBuilder(digestCalc).build(sPass.toCharArray());

            key = secretKey.extractPrivateKey(decryptor);
        } catch (IOException | PGPException ex) {
            System.out.println(ex.toString());
        }

        return key;
    }

    // Return the contents of a file as String
    public String readFileAsString(String filePath) throws java.io.IOException {
        StringBuilder fileData = new StringBuilder(1000);
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        char[] buf = new char[1024];
        int numRead;
        while ((numRead = reader.read(buf)) != -1) {
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
            buf = new char[1024];
        }
        reader.close();
        return fileData.toString();
    }

    /*
     * Helper Functions
     */
    // Extract Secret Key from File
    private PGPSecretKey getSecretKey(String privateKeyData) throws IOException, PGPException {
        try (InputStream privateKeyStream = new ArmoredInputStream(new ByteArrayInputStream(privateKeyData.getBytes("UTF-8")))) {
            PGPSecretKeyRingCollection pgpSec = new PGPSecretKeyRingCollection(PGPUtil.getDecoderStream(privateKeyStream), new JcaKeyFingerprintCalculator());
            Iterator keyRingIterator = pgpSec.getKeyRings();
            while (keyRingIterator.hasNext()) {
                PGPSecretKeyRing keyRing = (PGPSecretKeyRing) keyRingIterator.next();
                Iterator keyIterator = keyRing.getSecretKeys();
                while (keyIterator.hasNext()) {
                    PGPSecretKey key = (PGPSecretKey) keyIterator.next();

                    if (key.isSigningKey()) {
                        return key;
                    }
                }
            }
        }
        throw new IllegalArgumentException("Can't find signing key in key ring.");
    }

    private int readCharArray(char[] buf, String path) throws FileNotFoundException, IOException {
        FileReader fr = new FileReader(path);
        int count;
        count = fr.read(buf);
        return count;
    }

    private String reArmourKey(PGPPublicKey pgpPub, PGPPrivateKey pgpPriv) {
        String key = null;
        
        try {
            PGPSecretKeyRing secRing = newSecKey(pgpPub, pgpPriv);
            byte[] encoded = secRing.getEncoded();

            ByteArrayInputStream bais = new ByteArrayInputStream(encoded);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ArmoredOutputStream armoredOutputStream = new ArmoredOutputStream(baos);
        
            Streams.copy(bais, armoredOutputStream, true);
            armoredOutputStream.flush();
            armoredOutputStream.close();
            
            key = baos.toString();
        } catch (IOException ex) {
            Logger.getLogger(MyCipherConfig.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(MyCipherConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return key;
    }

    private PGPSecretKeyRing newSecKey(PGPPublicKey pgpPub, PGPPrivateKey pgpPriv) throws Exception {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        PGPKeyPair ecdsaKeyPair = new PGPKeyPair(pgpPub, pgpPriv);

        final StringBuilder clearKey = new StringBuilder();
        sharedKey.access(new GuardedString.Accessor() {
            @Override
            public void access(final char[] clearChars) {
                clearKey.append(clearChars);
            }
        });
        char[] passPhrase = clearKey.toString().toCharArray();
        PGPDigestCalculator sha1Calc = new JcaPGPDigestCalculatorProviderBuilder().build().get(HashAlgorithmTags.SHA1);
        PGPKeyRingGenerator keyRingGen = new PGPKeyRingGenerator(PGPSignature.POSITIVE_CERTIFICATION, ecdsaKeyPair,
                 "key@ega.org", sha1Calc, null, null, new JcaPGPContentSignerBuilder(ecdsaKeyPair.getPublicKey().getAlgorithm(), HashAlgorithmTags.SHA1), new JcePBESecretKeyEncryptorBuilder(PGPEncryptedData.AES_256, sha1Calc).setProvider("BC").build(passPhrase));

        //PGPPublicKeyRing pubRing = keyRingGen.generatePublicKeyRing();
        PGPSecretKeyRing secRing = keyRingGen.generateSecretKeyRing();

        KeyFingerPrintCalculator fingerCalc = new JcaKeyFingerprintCalculator();

        //PGPPublicKeyRing pubRingEnc = new PGPPublicKeyRing(pubRing.getEncoded(), fingerCalc);
        PGPSecretKeyRing secRingEnc = new PGPSecretKeyRing(secRing.getEncoded(), fingerCalc);
        
        return secRingEnc;
    }
    
}
