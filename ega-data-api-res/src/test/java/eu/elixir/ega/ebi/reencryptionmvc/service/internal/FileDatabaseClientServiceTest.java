/*
 *
 * Copyright 2020 EMBL - European Bioinformatics Institute
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
 *
 */
package eu.elixir.ega.ebi.reencryptionmvc.service.internal;

import eu.elixir.ega.ebi.reencryptionmvc.dto.EgaFile;
import eu.elixir.ega.ebi.reencryptionmvc.exception.EgaFileNotFoundException;
import eu.elixir.ega.ebi.reencryptionmvc.exception.FileNotAvailableException;
import eu.elixir.ega.ebi.reencryptionmvc.service.FileDatabaseClientService;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static eu.elixir.ega.ebi.reencryptionmvc.config.Constants.FILEDATABASE_SERVICE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FileDatabaseClientServiceTest {
    @Test
    public void ifFileDatabaseCallIsOK_ReturnsFileDatabaseEntity() throws EgaFileNotFoundException, FileNotAvailableException {
        // Arrange: Make the rest template return an OK response
        final String fileId = "test-file";
        final EgaFile file = new EgaFile();

        RestTemplate template = mock(RestTemplate.class);
        when(template.getForEntity(FILEDATABASE_SERVICE + "/file/{fileId}", EgaFile[].class, fileId))
                .thenReturn(new ResponseEntity<>(new EgaFile[]{file}, HttpStatus.OK));
        FileDatabaseClientService client = new FileDatabaseClientServiceImpl(template);

        // Act: get the entity
        EgaFile response = client.getById(fileId);

        // Assert: check we got the right file
        assertSame(file, response);
    }

    @Test
    public void ifFileDatabaseCallIsNotFound_ThrowsEgaFileNotFoundException() throws FileNotAvailableException {
        // Arrange: Make the rest template return an OK response
        final String fileId = "test-file";

        RestTemplate template = mock(RestTemplate.class);
        when(template.getForEntity(FILEDATABASE_SERVICE + "/file/{fileId}", EgaFile[].class, fileId))
                .thenReturn(new ResponseEntity<>(HttpStatus.NOT_FOUND));
        FileDatabaseClientService client = new FileDatabaseClientServiceImpl(template);

        // Act: get the entity
        EgaFileNotFoundException exception = null;
        try {
            client.getById(fileId);
        } catch (EgaFileNotFoundException e) {
            exception = e;
        }

        // Assert: check we got the right exception
        assertNotNull(exception);
        assertEquals(fileId, exception.getFileId());
    }
}
