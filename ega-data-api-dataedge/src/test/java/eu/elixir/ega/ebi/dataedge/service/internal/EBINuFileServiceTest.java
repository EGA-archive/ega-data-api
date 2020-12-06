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
package eu.elixir.ega.ebi.dataedge.service.internal;

import eu.elixir.ega.ebi.commons.shared.dto.File;
import eu.elixir.ega.ebi.dataedge.exception.EgaFileNotFoundException;
import eu.elixir.ega.ebi.dataedge.exception.FileNotAvailableException;
import eu.elixir.ega.ebi.dataedge.exception.RangesNotSatisfiableException;
import eu.elixir.ega.ebi.dataedge.exception.UnretrievableFileException;
import eu.elixir.ega.ebi.dataedge.service.FileDatabaseClientService;
import eu.elixir.ega.ebi.dataedge.service.KeyService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.ega.fire.exceptions.ClientProtocolException;
import uk.ac.ebi.ega.fire.exceptions.FireServiceException;
import uk.ac.ebi.ega.fire.service.IFireService;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@Import(EBINuFileServiceTest.Configuration.class)
public class EBINuFileServiceTest {

    private static final File MOCK_FILE = new File(
            "EGATEST-0001",
            "EGA-TEST-DATASET",
            "test-file",
            "path/to/test/file",
            "test-file",
            1234L,
            "test-checksum",
            "test-checksum-type",
            "test-status"
    );
    @Autowired
    private EBINuFileService service;
    @MockBean
    private KeyService keyService;
    @MockBean
    private FileDatabaseClientService fileDatabaseClientService;
    @MockBean
    private IFireService fireService;

    @Test
    public void canGetPlainFileSize_OfPlainFile() throws EgaFileNotFoundException, UnretrievableFileException, FileNotAvailableException {
        // Arrange: Set up a mock file in the FileDatabase
        when(fileDatabaseClientService.getById(MOCK_FILE.getFileId())).thenReturn(MOCK_FILE);
        when(keyService.getEncryptionAlgorithm(MOCK_FILE.getFileId())).thenReturn("plain");

        // Act: Get the size of the mock file
        long plainSize = service.getPlainFileSize(MOCK_FILE.getFileId());

        // Assert: Check that the size matches what we said for the mock file
        Assert.assertEquals(MOCK_FILE.getFileSize(), plainSize);
    }

    @Test
    public void canGetPlainFileSize_OfAesEncryptedFile() throws EgaFileNotFoundException, UnretrievableFileException, FileNotAvailableException {
        // Arrange: Set up a mock file in the FileDatabase
        when(fileDatabaseClientService.getById(MOCK_FILE.getFileId())).thenReturn(MOCK_FILE);
        when(keyService.getEncryptionAlgorithm(MOCK_FILE.getFileId())).thenReturn("aes256");

        // Act: Get the size of the mock file
        long plainSize = service.getPlainFileSize(MOCK_FILE.getFileId());

        // Assert: Check that the size matches what we said for the mock file minus 16 bytes for the IV header
        Assert.assertEquals(MOCK_FILE.getFileSize() - 16L, plainSize);
    }

    @Test
    public void ifFileDoesNotExist_ThrowsEgaFileNotFoundException() throws EgaFileNotFoundException, UnretrievableFileException, FileNotAvailableException {
        // Arrange: File database cannot find this file
        when(fileDatabaseClientService.getById(MOCK_FILE.getFileId())).thenThrow(new EgaFileNotFoundException(MOCK_FILE.getFileId()));

        // Act: get the size of the file that does not exist
        EgaFileNotFoundException exception = null;
        try {
            service.getPlainFileSize(MOCK_FILE.getFileId());
        } catch (EgaFileNotFoundException e) {
            exception = e;
        }

        // Assert: we got the exception and it is the right file ID
        assertNotNull(exception);
        Assert.assertEquals(MOCK_FILE.getFileId(), exception.getFileId());
    }

    @Test
    public void ifFileIsNotFoundOnFire_ThrowsEgaFileNotFoundException() throws FileNotAvailableException, IOException, RangesNotSatisfiableException, UnretrievableFileException, EgaFileNotFoundException, URISyntaxException, FireServiceException, ClientProtocolException {
        // Arrange: File database contains this file but Fire throws not found
        when(fileDatabaseClientService.getById(MOCK_FILE.getFileId())).thenReturn(MOCK_FILE);
        when(keyService.getEncryptionAlgorithm(MOCK_FILE.getFileId())).thenReturn("plain");
        when(fireService.downloadByteRangeByPath(anyString(), anyLong(), anyLong())).thenThrow(new FileNotFoundException());

        // Act: download bytes from the file
        EgaFileNotFoundException exception = null;
        try {
            service.getSpecificByteRange(MOCK_FILE.getFileId(), 0, 100);
        } catch (EgaFileNotFoundException e) {
            exception = e;
        }

        // Assert: we got the exception and it is the right file ID
        assertNotNull(exception);
        Assert.assertEquals(MOCK_FILE.getFileId(), exception.getFileId());
    }

    @Test
    public void ifFileIsEncryptedInNonSupportedFormat_ThrowsUnretrievableFileException() throws EgaFileNotFoundException, FileNotAvailableException {
        // Arrange: Set up a mock file in an unsupported encrypted format
        when(fileDatabaseClientService.getById(MOCK_FILE.getFileId())).thenReturn(MOCK_FILE);
        when(keyService.getEncryptionAlgorithm(MOCK_FILE.getFileId())).thenReturn("symmetricgpg");

        // Act: Get the size of the file in the unsupported format
        UnretrievableFileException exception = null;
        try {
            service.getPlainFileSize(MOCK_FILE.getFileId());
        } catch (UnretrievableFileException e) {
            exception = e;
        }

        // Assert: we got the exception and it is the right file ID
        assertNotNull(exception);
        Assert.assertEquals(MOCK_FILE.getFileId(), exception.getFileId());
    }

    @Test
    public void ifFileExistsButIsNotAvailable_ThrowsFileNotAvailableException() throws EgaFileNotFoundException, UnretrievableFileException, FileNotAvailableException {
        // Arrange: Set up a mock file that is not available
        when(fileDatabaseClientService.getById(MOCK_FILE.getFileId())).thenThrow(new FileNotAvailableException(MOCK_FILE.getFileId()));

        // Act: Get the size of the file that is not available
        FileNotAvailableException exception = null;
        try {
            service.getPlainFileSize(MOCK_FILE.getFileId());
        } catch (FileNotAvailableException e) {
            exception = e;
        }

        // Assert: we got the exception and it is the right file ID
        assertNotNull(exception);
        Assert.assertEquals(MOCK_FILE.getFileId(), exception.getFileId());
    }

    @Test
    public void ifFireConnectionFails_ThrowsFileNotAvailableException() throws EgaFileNotFoundException, FileNotAvailableException, IOException, RangesNotSatisfiableException, UnretrievableFileException, URISyntaxException, FireServiceException, ClientProtocolException {
        // Arrange: File database contains this file but Fire throws a generic exception
        when(fileDatabaseClientService.getById(MOCK_FILE.getFileId())).thenReturn(MOCK_FILE);
        when(keyService.getEncryptionAlgorithm(MOCK_FILE.getFileId())).thenReturn("plain");
        when(fireService.downloadByteRangeByPath(anyString(), anyLong(), anyLong())).thenThrow(new FireServiceException("Test error", new RuntimeException()));

        // Act: download bytes from the file
        FileNotAvailableException exception = null;
        try {
            service.getSpecificByteRange(MOCK_FILE.getFileId(), 0, 100);
        } catch (FileNotAvailableException e) {
            exception = e;
        }

        // Assert: we got the exception and it is the right file ID
        assertNotNull(exception);
        Assert.assertEquals(MOCK_FILE.getFileId(), exception.getFileId());
        assertTrue(exception.getCause() instanceof FireServiceException);
    }

    @Test
    public void ifSpecifiedRangesCanNotBeSatisfied_ThrowsRangesNoSatisfiableException() throws EgaFileNotFoundException, FileNotAvailableException, UnretrievableFileException {
        // Arrange: Set up a mock file
        when(fileDatabaseClientService.getById(MOCK_FILE.getFileId())).thenReturn(MOCK_FILE);
        when(keyService.getEncryptionAlgorithm(MOCK_FILE.getFileId())).thenReturn("plain");

        // Act: Request a range that goes past the end of the file
        RangesNotSatisfiableException exception = null;
        try {
            service.getSpecificByteRange(MOCK_FILE.getFileId(),
                    MOCK_FILE.getFileSize() - 50L,
                    MOCK_FILE.getFileSize() + 50L);
        } catch (RangesNotSatisfiableException e) {
            exception = e;
        }

        // Assert: we got the exception and it is the right file ID and ranges
        assertNotNull(exception);
        Assert.assertEquals(MOCK_FILE.getFileId(), exception.getFileId());
        Assert.assertEquals(MOCK_FILE.getFileSize() - 50L, exception.getRangeStart());
        Assert.assertEquals(MOCK_FILE.getFileSize() + 50L, exception.getRangeEnd());
    }

    @TestConfiguration
    protected static class Configuration {
        @Bean
        public EBINuFileService ebiNuFileService(KeyService keyService,
                                                 FileDatabaseClientService fileDatabaseClientService,
                                                 IFireService fireService) {
            return new EBINuFileService(keyService, fileDatabaseClientService, fireService);
        }
    }
}
