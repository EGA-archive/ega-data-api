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
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.fail;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import static org.mockito.Matchers.any;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * Test class for {@link KeyServiceImpl}.
 * 
 * @author amohan
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ KeyServiceImpl.class, Gson.class, IOUtils.class })
public class KeyServiceImplTest {

    private final String SERVICE_URL = "http://KEYSERVER";

    @InjectMocks
    private KeyServiceImpl keyServiceImpl;

    @Mock
    RestTemplate restTemplate;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    /**
     * Test class for {@link KeyServiceImpl#getFileKey(String)}. Verify the output
     * key.
     */
    @Test
    public void testGetFileKey() {
        final ResponseEntity<String> mockResponseEntity = mock(ResponseEntity.class);
        final String keyMock = "body Output";
        when(restTemplate.getForEntity(SERVICE_URL + "/keys/filekeys/{file_id}", String.class, "fileId"))
                .thenReturn(mockResponseEntity);
        when(mockResponseEntity.getBody()).thenReturn(keyMock);

        final String key = keyServiceImpl.getFileKey("fileId");

        assertThat(key, equalTo(keyMock));
    }

    /**
     * Test class for {@link KeyServiceImpl#getKeyPath(String)}. Verify the output
     * path.
     */
    @Test
    public void testGetKeyPath() {
        final ResponseEntity<KeyPath> mockResponseEntity = mock(ResponseEntity.class);
        final KeyPath keyPathsMock = new KeyPath( "path1", "path2" );
        when(restTemplate.getForEntity(SERVICE_URL + "/keys/retrieve/{keyId}/private/path", KeyPath.class, "key"))
                .thenReturn(mockResponseEntity);
        when(mockResponseEntity.getBody()).thenReturn(keyPathsMock);

        final KeyPath paths = keyServiceImpl.getKeyPath("key");

        assertThat(paths, equalTo(keyPathsMock));
    }

    /**
     * Test class for {@link LocalEgaKeyServiceImpl#getRSAKeyById(String)}. Verify
     * the output RSAKey.
     * 
     * @throws Exception
     */
    @Test
    public void testGetRSAKeyById() throws Exception {

        final String valueRSAKey = "9B7D2C34A366BF890C730641E6CECF6F";
        HashMap<String, String> test = new HashMap<>();
        test.put("public", valueRSAKey);
        final URL urlMock = mock(URL.class);
        final Gson gson = mock(Gson.class);
        final PemReader pemReader = mock(PemReader.class);
        final PemObject pemObject = mock(PemObject.class);
        final InputStream inputStream = mock(InputStream.class);

        mockStatic(IOUtils.class);
        whenNew(URL.class).withArguments(any()).thenReturn(urlMock);
        whenNew(Gson.class).withAnyArguments().thenReturn(gson);
        whenNew(PemReader.class).withAnyArguments().thenReturn(pemReader);
        when(IOUtils.toString(inputStream, Charset.defaultCharset())).thenReturn(valueRSAKey);
        when(gson.fromJson("{2=100}", HashMap.class)).thenReturn(test);
        when(urlMock.openStream()).thenReturn(inputStream);
        when(pemReader.readPemObject()).thenReturn(pemObject);
        when(pemObject.getContent()).thenReturn(valueRSAKey.getBytes());

        byte[] outputRSAKey = keyServiceImpl.getRSAKeyById("id");

        assertThat(outputRSAKey, equalTo(valueRSAKey.getBytes()));
    }

    /**
     * Test class for {@link LocalEgaKeyServiceImpl#getPGPPublicKeyById(String)}.
     * Verify code is executing without errors.
     * 
     * @throws Exception
     */
    @Test
    public void testGetPGPPublicKeyById() throws Exception {
        try {
            byte[] testPubKeyRing = Base64.decode("mQGiBEAR8jYRBADNifuSopd20JOQ5x30ljIaY0M6927+vo09NeNxS3KqItba"
                    + "nz9o5e2aqdT0W1xgdHYZmdElOHTTsugZxdXTEhghyxoo3KhVcNnTABQyrrvX"
                    + "qouvmP2fEDEw0Vpyk+90BpyY9YlgeX/dEA8OfooRLCJde/iDTl7r9FT+mts8"
                    + "g3azjwCgx+pOLD9LPBF5E4FhUOdXISJ0f4EEAKXSOi9nZzajpdhe8W2ZL9gc"
                    + "BpzZi6AcrRZBHOEMqd69gtUxA4eD8xycUQ42yH89imEcwLz8XdJ98uHUxGJi"
                    + "qp6hq4oakmw8GQfiL7yQIFgaM0dOAI9Afe3m84cEYZsoAFYpB4/s9pVMpPRH"
                    + "NsVspU0qd3NHnSZ0QXs8L8DXGO1uBACjDUj+8GsfDCIP2QF3JC+nPUNa0Y5t"
                    + "wKPKl+T8hX/0FBD7fnNeC6c9j5Ir/Fp/QtdaDAOoBKiyNLh1JaB1NY6US5zc"
                    + "qFks2seZPjXEiE6OIDXYra494mjNKGUobA4hqT2peKWXt/uBcuL1mjKOy8Qf"
                    + "JxgEd0MOcGJO+1PFFZWGzLQ3RXJpYyBILiBFY2hpZG5hICh0ZXN0IGtleSBv"
                    + "bmx5KSA8ZXJpY0Bib3VuY3ljYXN0bGUub3JnPohZBBMRAgAZBQJAEfI2BAsH"
                    + "AwIDFQIDAxYCAQIeAQIXgAAKCRAOtk6iUOgnkDdnAKC/CfLWikSBdbngY6OK"
                    + "5UN3+o7q1ACcDRqjT3yjBU3WmRUNlxBg3tSuljmwAgAAuQENBEAR8jgQBAC2"
                    + "kr57iuOaV7Ga1xcU14MNbKcA0PVembRCjcVjei/3yVfT/fuCVtGHOmYLEBqH"
                    + "bn5aaJ0P/6vMbLCHKuN61NZlts+LEctfwoya43RtcubqMc7eKw4k0JnnoYgB"
                    + "ocLXOtloCb7jfubOsnfORvrUkK0+Ne6anRhFBYfaBmGU75cQgwADBQP/XxR2"
                    + "qGHiwn+0YiMioRDRiIAxp6UiC/JQIri2AKSqAi0zeAMdrRsBN7kyzYVVpWwN"
                    + "5u13gPdQ2HnJ7d4wLWAuizUdKIQxBG8VoCxkbipnwh2RR4xCXFDhJrJFQUm+"
                    + "4nKx9JvAmZTBIlI5Wsi5qxst/9p5MgP3flXsNi1tRbTmRhqIRgQYEQIABgUC"
                    + "QBHyOAAKCRAOtk6iUOgnkBStAJoCZBVM61B1LG2xip294MZecMtCwQCbBbsk" + "JVCXP0/Szm05GB+WN+MOCT2wAgAA");

            final JcaPGPObjectFactory pgpFact = new JcaPGPObjectFactory(testPubKeyRing);
            final PGPPublicKeyRing mockPGPPublicKeyRing = (PGPPublicKeyRing) pgpFact.nextObject();
            final ArrayList testArray = new ArrayList();
            testArray.add(mockPGPPublicKeyRing);

            final URL urlMock = mock(URL.class);
            final InputStream inputStream = mock(InputStream.class);
            final PGPPublicKeyRingCollection pgp = mock(PGPPublicKeyRingCollection.class);

            whenNew(URL.class).withArguments(any()).thenReturn(urlMock);
            whenNew(PGPPublicKeyRingCollection.class).withAnyArguments().thenReturn(pgp);
            when(urlMock.openStream()).thenReturn(inputStream);
            when(pgp.getKeyRings()).thenReturn(testArray.iterator());

            keyServiceImpl.getPGPPublicKeyById("id");

        } catch (Exception e) {
            fail("Should not have thrown an exception");
        }
    }
}
