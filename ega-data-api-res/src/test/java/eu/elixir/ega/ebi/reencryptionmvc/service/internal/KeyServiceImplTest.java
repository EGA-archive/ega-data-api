/*
 * Copyright 2016 ELIXIR EBI
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
package eu.elixir.ega.ebi.reencryptionmvc.service.internal;

import com.google.gson.Gson;
import eu.elixir.ega.ebi.reencryptionmvc.dto.KeyPath;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static eu.elixir.ega.ebi.shared.Constants.KEYS_SERVICE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Test class for {@link KeyServiceImpl}.
 *
 * @author amohan
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({KeyServiceImpl.class, Gson.class, IOUtils.class})
public class KeyServiceImplTest {

    @InjectMocks
    private KeyServiceImpl keyServiceImpl;

    @Mock
    private RestTemplate restTemplate;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    /**
     * Test method for {@link KeyServiceImpl#getFileKey(String)}. Verify the output
     * key.
     */
    @Test
    public void testGetFileKey() {
        final ResponseEntity<String> mockResponseEntity = mock(ResponseEntity.class);
        final String keyMock = "body Output";
        when(restTemplate.getForEntity(KEYS_SERVICE + "/keys/filekeys/{fileId}", String.class, "fileId"))
                .thenReturn(mockResponseEntity);
        when(mockResponseEntity.getBody()).thenReturn(keyMock);

        final String key = keyServiceImpl.getFileKey("fileId");

        assertThat(key, equalTo(keyMock));
    }

    /**
     * Test method for {@link KeyServiceImpl#getKeyPath(String)}. Verify the output
     * path.
     */
    @Test
    public void testGetKeyPath() {
        final ResponseEntity<KeyPath> mockResponseEntity = mock(ResponseEntity.class);
        final KeyPath keyPathsMock = new KeyPath("path1", "path2");
        when(restTemplate.getForEntity(KEYS_SERVICE + "/keys/retrieve/{keyId}/private/path", KeyPath.class, "key"))
                .thenReturn(mockResponseEntity);
        when(mockResponseEntity.getBody()).thenReturn(keyPathsMock);

        final KeyPath paths = keyServiceImpl.getKeyPath("key");

        assertThat(paths, equalTo(keyPathsMock));
    }

    /**
     * Test method for {@link KeyServiceImpl#getPublicKey(String)}. Verify
     * the output RSAKey.
     */
    @Test
    public void testGetPublicKey() {
        final ResponseEntity<String> mockResponseEntity = mock(ResponseEntity.class);
        final String keyMock = "key";
        when(restTemplate.getForEntity(KEYS_SERVICE + "/keys/retrieve/{keyId}/public", String.class, "test@elixir.org"))
                .thenReturn(mockResponseEntity);
        when(mockResponseEntity.getBody()).thenReturn(keyMock);

        final String key = keyServiceImpl.getPublicKey("test@elixir.org");

        assertThat(key, equalTo(keyMock));
    }

    /**
     * Test method for {@link KeyServiceImpl#getPrivateKey(String)}.
     * Verify code is executing without errors.
     */
    @Test
    public void testGetPrivateKey() {
        final ResponseEntity<String> mockResponseEntity = mock(ResponseEntity.class);
        final String keyMock = "key";
        when(restTemplate.getForEntity(KEYS_SERVICE + "/keys/retrieve/{keyId}/private/key", String.class, "id"))
                .thenReturn(mockResponseEntity);
        when(mockResponseEntity.getBody()).thenReturn(keyMock);

        final String key = keyServiceImpl.getPrivateKey("id");

        assertThat(key, equalTo(keyMock));
    }

}
