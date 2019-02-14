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

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.ThreadLocalRandom;

public class Random {

    private final static Logger logger = LoggerFactory.getLogger(Random.class);

    private final static String dictionary = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz123456789";

    public static SecureRandom getSHA1PRNG() {
        return getSecureRandom("SHA1PRNG");
    }

    private static SecureRandom getSecureRandom(String algorithm) {
        try {
            return SecureRandom.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            logger.error(e.getMessage(), e);
            throw new AssertionError(e);
        }
    }

    public static char[] getRandomString(int size) {
        char[] randomString = new char[size];
        for (int i = 0; i < size; i++) {
            int randomNum = ThreadLocalRandom.current().nextInt(0, dictionary.length());
            randomString[i] = dictionary.charAt(randomNum);
        }
        return randomString;
    }

}
