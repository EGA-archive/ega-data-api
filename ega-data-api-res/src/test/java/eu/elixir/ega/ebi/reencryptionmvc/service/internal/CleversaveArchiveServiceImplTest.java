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
import eu.elixir.ega.ebi.reencryptionmvc.service.ArchiveAdapterService;
import eu.elixir.ega.ebi.reencryptionmvc.service.KeyService;

/**
 * Test class for {@link CleversaveArchiveServiceImpl}.
 * 
 * @author amohan
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(CleversaveArchiveServiceImpl.class)
public class CleversaveArchiveServiceImplTest {

    public static final String SERVICE_URL = "http://FILEDATABASE";

    @InjectMocks
    private CleversaveArchiveServiceImpl cleversaveArchiveServiceImpl;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private KeyService keyService;

    @Mock
    private ArchiveAdapterService archiveAdapterService;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    /**
     * Test class for
     * {@link CleversaveArchiveServiceImpl#getArchiveFile(String, HttpServletResponse)}.
     * Verify the archiveSource output parameters.
     */
    @Test
    public void testGetArchiveFile() {
        final ResponseEntity<EgaFile[]> mockResponseEntity = mock(ResponseEntity.class);
        final EgaFile[] body = new EgaFile[1];
        body[0] = new EgaFile("fileId0", "/fire/0000TR.CEL.gpg", 100, "fileStatus0");

        final String pathInput = "/EGAZ00001257562/analysis/ALL.chr22.phase3_shapeit2_mvncall_integrated_v5a.20130502.genotypes.vcf.gz.cip";
        final String object_get = "http://egaread:8oAgZc0DH6dR@10.32.25.44/ega/06ca84a54eeadf8acd3cf05691652d5e0014";
        final String object_length = "214453766";
        final String object_storage_class = "CLEVERSAFE";
        final String encryptionKey = "encryptionKey";

        final String[] filePath = { object_get, object_length, object_storage_class, pathInput };
        when(restTemplate.getForEntity(SERVICE_URL + "/file/{fileId}", EgaFile[].class, "id"))
                .thenReturn(mockResponseEntity);
        when(mockResponseEntity.getStatusCode()).thenReturn(HttpStatus.OK);
        when(mockResponseEntity.getBody()).thenReturn(body);
        when(archiveAdapterService.getPath(anyString())).thenReturn(filePath);
        when(keyService.getFileKey(anyString())).thenReturn(encryptionKey);

        final ArchiveSource archiveSource = cleversaveArchiveServiceImpl.getArchiveFile("id",
                new MockHttpServletResponse());
        
        assertThat(archiveSource.getFileUrl(), equalTo(filePath[0]));
        assertThat(archiveSource.getSize(), equalTo(Long.valueOf(filePath[1])));
        assertThat(archiveSource.getEncryptionFormat(), equalTo("symmetricgpg"));
        assertThat(archiveSource.getEncryptionKey(), equalTo(encryptionKey));

    }

}
