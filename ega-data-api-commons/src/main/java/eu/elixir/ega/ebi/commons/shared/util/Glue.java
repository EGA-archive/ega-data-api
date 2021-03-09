package eu.elixir.ega.ebi.commons.shared.util;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class Glue {
    
    public SecretKey getKey(char[] password, int pw_strength) {
        // Key Generation
        byte[] salt = {(byte) -12, (byte) 34, (byte) 1, (byte) 0, (byte) -98, (byte) 223, (byte) 78, (byte) 21};
        SecretKeyFactory factory;
        SecretKey secret = null;
        try {
            factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            KeySpec spec = new PBEKeySpec(password, salt, 1024, pw_strength); // Password Strength - n bits
            SecretKey tmp = factory.generateSecret(spec);
            secret = new SecretKeySpec(tmp.getEncoded(), "AES");
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            log.error(ex.getMessage(), ex);
        }
        return secret;
    }
}
