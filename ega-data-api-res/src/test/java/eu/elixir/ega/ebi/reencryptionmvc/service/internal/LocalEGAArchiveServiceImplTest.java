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

import eu.elixir.ega.ebi.reencryptionmvc.dto.ArchiveSource;
import eu.elixir.ega.ebi.reencryptionmvc.dto.EgaFile;
import eu.elixir.ega.ebi.reencryptionmvc.service.KeyService;
import no.ifi.uio.crypt4gh.factory.HeaderFactory;
import no.ifi.uio.crypt4gh.pojo.*;
import org.bouncycastle.jcajce.provider.util.BadBlockException;
import org.bouncycastle.openpgp.PGPException;
import org.identityconnectors.common.security.GuardedString;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

import static eu.elixir.ega.ebi.shared.Constants.FILEDATABASE_SERVICE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Test class for {@link LocalEGAArchiveServiceImpl}.
 *
 * @author amohan
 */
@RunWith(SpringRunner.class)
public class LocalEGAArchiveServiceImplTest {

    @InjectMocks
    private LocalEGAArchiveServiceImpl localEGAArchiveService;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private KeyService keyService;

    @Mock
    private HeaderFactory headerFactory;

    @Before
    public void initMocks() throws IOException, PGPException, BadBlockException {
        MockitoAnnotations.initMocks(this);

        localEGAArchiveService.setSharedKey(new GuardedString(new char[]{}));

        final UnencryptedHeader unencryptedHeader = new UnencryptedHeader(null, 0, 0);
        final EncryptedHeader encryptedHeader = new EncryptedHeader(1, Collections.singletonList(new Record(0, 0, 0, 0, EncryptionAlgorithm.AES_256_CTR, "key".getBytes(), "iv".getBytes())));
        when(headerFactory.getHeader(Matchers.<byte[]>any(), anyString(), anyString())).thenReturn(new Header(unencryptedHeader, encryptedHeader));

        final ResponseEntity<EgaFile[]> mockResponseEntity = mock(ResponseEntity.class);
        final EgaFile[] body = new EgaFile[]{new EgaFile("id", "name", "/path/test.enc", 100, "ready", "header")};

        when(restTemplate.getForEntity(FILEDATABASE_SERVICE + "/file/{fileId}", EgaFile[].class, "id")).thenReturn(mockResponseEntity);
        when(mockResponseEntity.getStatusCode()).thenReturn(HttpStatus.OK);
        when(mockResponseEntity.getBody()).thenReturn(body);
    }

    /**
     * Test class for
     * {@link LocalEGAArchiveServiceImpl#getArchiveFile(String, HttpServletResponse)}.
     * Verify the archiveSource output parameters.
     */
    @Test
    public void testGetArchiveFile() {
        final ArchiveSource archiveSource = localEGAArchiveService.getArchiveFile("id", new MockHttpServletResponse());

        assertThat(archiveSource.getFileUrl(), equalTo("name"));
        assertThat(archiveSource.getSize(), equalTo(100L));
        assertThat(archiveSource.getEncryptionFormat(), equalTo("aes256"));
        assertThat(archiveSource.getEncryptionKey(), equalTo("a2V5")); //Base64 encoded string "key"
        assertThat(archiveSource.getEncryptionIV(), equalTo("aXY=")); //Base64 encoded string "iv"
    }

}
