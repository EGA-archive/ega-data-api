/*
` * Copyright 2016 ELIXIR EGA
 * Copyright 2016 Alexander Senf
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
package eu.elixir.ega.ebi.keyproviderservice.service.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import eu.elixir.ega.ebi.keyproviderservice.config.MyCipherConfig;
import eu.elixir.ega.ebi.keyproviderservice.domain.entity.EncryptionKey;
import eu.elixir.ega.ebi.keyproviderservice.domain.repository.EncryptionKeyRepository;
import eu.elixir.ega.ebi.keyproviderservice.dto.KeyPath;
import eu.elixir.ega.ebi.keyproviderservice.service.KeyService;

/**
 * Test class for {@link KeyServiceImpl}.
 * 
 * @author amohan
 */
@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:application-test.properties")
public final class KeyServiceImplTest {

    private final String ID = "id";
    private final long KEY_ID = 1000l;
    private final String ENCRYPTION_KEY = "encryptionKey";
    private final String PUBLIC_KEY = "publicKey";
    private final String ASCII_ARMOURED_KEY = "AsciiArmouredKey";
    private final String KEY_TYPE = "keyType";

    @Autowired
    private KeyService keyService;

    @MockBean
    private MyCipherConfig myCipherConfig;

    @MockBean
    private EncryptionKeyRepository encryptionKeyRepository;

    /**
     * Test class for {@link KeyServiceImpl#getFileKey(String)}. Verify the
     * EncryptionKey.
     */
    @Test
    public void testGetFileKey() {
        final EncryptionKey fileKey = new EncryptionKey(100l, "alias", ENCRYPTION_KEY);
        when(encryptionKeyRepository.findById(ID)).thenReturn(fileKey);
        assertThat(keyService.getFileKey(ID), equalTo(ENCRYPTION_KEY));
    }

    /**
     * Test class for {@link KeyServiceImpl#getPrivateKey(String)}. Verify the
     * PrivateKey.
     */
    @Test
    public void testGetPrivateKey() {
        final PGPPrivateKey pgpPrivateKey = mock(PGPPrivateKey.class);
        when(myCipherConfig.getPrivateKey(KEY_ID)).thenReturn(pgpPrivateKey);
        assertThat(keyService.getPrivateKey(String.valueOf(KEY_ID)), equalTo(pgpPrivateKey));
    }

    /**
     * Test class for {@link KeyServiceImpl#getPrivateKey(String)}. Verify the
     * PrivateKeyPath.
     */
    @Test
    public void testGetPrivateKeyPath() {
        final KeyPath keypath = new KeyPath("keyPath", "keyPassPath");
        when(myCipherConfig.getKeyPaths(KEY_ID)).thenReturn(keypath);
        assertThat(keyService.getPrivateKeyPath(String.valueOf(KEY_ID)), equalTo(keypath));
    }

    /**
     * Test class for {@link KeyServiceImpl#getPrivateKeyString(String)}. Verify the
     * PrivateKeyString.
     */
    @Test
    public void testGetPrivateKeyString() {
        when(myCipherConfig.getAsciiArmouredKey(KEY_ID)).thenReturn(ASCII_ARMOURED_KEY);
        assertThat(keyService.getPrivateKeyString(String.valueOf(KEY_ID)), equalTo(ASCII_ARMOURED_KEY));
    }

    /**
     * Test class for {@link KeyServiceImpl#getPublicKey(String, String)}. Verify
     * the PublicKey.
     */
    @Test
    public void testGetPublicKey_Id() {
        when(myCipherConfig.getPublicKeyById(String.valueOf(KEY_ID))).thenReturn(PUBLIC_KEY);
        assertThat(keyService.getPublicKey("id", String.valueOf(KEY_ID)), equalTo(PUBLIC_KEY));
    }

    /**
     * Test class for {@link KeyServiceImpl#getPublicKey(String, String)}. Verify
     * the PublicKey.
     */
    @Test
    public void testGetPublicKey_Email() {
        when(myCipherConfig.getPublicKeyByEmail(String.valueOf(KEY_ID))).thenReturn(PUBLIC_KEY);
        assertThat(keyService.getPublicKey("email", String.valueOf(KEY_ID)), equalTo(PUBLIC_KEY));
    }

    /**
     * Test class for {@link KeyServiceImpl#getPublicKey(String, String)}. Verify
     * the PublicKey null.
     */
    @Test
    public void testGetPublicKey_Null() {
        assertThat(keyService.getPublicKey("test", String.valueOf(KEY_ID)), equalTo(null));
    }

    /**
     * Test class for {@link KeyServiceImpl#getPublicKeyFromPrivate(String)}. Verify
     * the pgpPublicKey.
     */
    @Test
    public void testGetPublicKeyFromPrivate() {
        final PGPPublicKey pgpPublicKey = mock(PGPPublicKey.class);
        when(myCipherConfig.getPublicKey(KEY_ID)).thenReturn(pgpPublicKey);
        assertThat(keyService.getPublicKeyFromPrivate(String.valueOf(KEY_ID)), equalTo(pgpPublicKey));
    }

    /**
     * Test class for {@link KeyServiceImpl#getKeyIDs(String)}. Verify the keyIds.
     */
    @Test
    public void testGetKeyIDs() {
        final Set<Long> keyIds = new HashSet<>();
        keyIds.add(1l);
        when(myCipherConfig.getKeyIDs()).thenReturn(keyIds);
        assertThat(keyService.getKeyIDs(KEY_TYPE), equalTo(keyIds));
    }

    @TestConfiguration
    static class KeyServiceImplTestContextConfiguration {
        @Bean
        public KeyService keyService() {
            return new KeyServiceImpl();
        }
    }

}