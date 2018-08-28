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

import static eu.elixir.ega.ebi.shared.Constants.FILEDATABASE_SERVICE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.client.RestTemplate;

import eu.elixir.ega.ebi.reencryptionmvc.dto.ArchiveSource;
import eu.elixir.ega.ebi.reencryptionmvc.dto.EgaFile;
import eu.elixir.ega.ebi.reencryptionmvc.service.KeyService;

/**
 * Test class for {@link GenericArchiveServiceImpl}.
 * 
 * @author amohan
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(GenericArchiveServiceImpl.class)
public class GenericArchiveServiceImplTest {

    @InjectMocks
    private GenericArchiveServiceImpl genericArchiveServiceImpl;

    @Mock
    RestTemplate restTemplate;

    @Mock
    private KeyService keyService;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    /**
     * Test class for
     * {@link GenericArchiveServiceImpl#getArchiveFile(String, HttpServletResponse)}.
     * Verify the archiveSource output parameters.
     */
    @Test
    public void testGetArchiveFile() {
        final ResponseEntity<EgaFile[]> mockResponseEntity = mock(ResponseEntity.class);
        final EgaFile[] body = new EgaFile[1];
        body[0] = new EgaFile("fileId0", "/fire/0000TR.CEL.gpg", 100, "fileStatus0");
        final String encryptionKey = "encryptionKey";

        when(restTemplate.getForEntity(FILEDATABASE_SERVICE + "/file/{fileId}", EgaFile[].class, "id"))
                .thenReturn(mockResponseEntity);
        when(mockResponseEntity.getStatusCode()).thenReturn(HttpStatus.OK);
        when(mockResponseEntity.getBody()).thenReturn(body);
        when(keyService.getFileKey(anyString())).thenReturn(encryptionKey);

        final ArchiveSource archiveSource = genericArchiveServiceImpl.getArchiveFile("id", new MockHttpServletResponse());
        
        assertThat(archiveSource.getFileUrl(), equalTo(body[0].getFileName()));
        assertThat(archiveSource.getSize(), equalTo(body[0].getFileSize()));
        assertThat(archiveSource.getEncryptionFormat(), equalTo("symmetricgpg"));
        assertThat(archiveSource.getEncryptionKey(), equalTo(encryptionKey));
    }
}
