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
import eu.elixir.ega.ebi.commons.shared.service.FileInfoService;
import eu.elixir.ega.ebi.dataedge.exception.EgaFileNotFoundException;
import eu.elixir.ega.ebi.dataedge.exception.FileNotAvailableException;
import eu.elixir.ega.ebi.dataedge.exception.RangesNotSatisfiableException;
import eu.elixir.ega.ebi.dataedge.exception.UnretrievableFileException;
import eu.elixir.ega.ebi.dataedge.service.KeyService;
import htsjdk.samtools.seekablestream.cipher.ebi.Glue;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestContextManager;
import uk.ac.ebi.ega.fire.exceptions.ClientProtocolException;
import uk.ac.ebi.ega.fire.exceptions.FireServiceException;
import uk.ac.ebi.ega.fire.service.IFireService;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import static eu.elixir.ega.ebi.dataedge.service.internal.EBINuFileService.ENCRYPTION_AES128;
import static eu.elixir.ega.ebi.dataedge.service.internal.EBINuFileService.ENCRYPTION_AES256;
import static eu.elixir.ega.ebi.dataedge.service.internal.EBINuFileService.ENCRYPTION_PLAIN;
import static eu.elixir.ega.ebi.dataedge.service.internal.EBINuFileService.FILE_STATUS_AVAILABLE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
@Import(EBINuFileServiceGetByteRangesTest.Configuration.class)
public class EBINuFileServiceGetByteRangesTest {

    public static final String ENCRYPTION_KEY = "test-key";
    private static final String MOCK_FILE_ID = "EGATEST-0001";
    private static final int MOCK_FILE_PLAIN_SIZE = 500;
    private static HashMap<String, byte[]> encryptedData;
    public final String encryptionType;
    public final long startByte;
    public final long endByte;
    @Autowired
    private EBINuFileService service;
    @MockBean
    private KeyService keyService;
    @MockBean
    private FileInfoService fileInfoService;
    @MockBean
    private IFireService fireService;
    public EBINuFileServiceGetByteRangesTest(String encryptionType, long rangeStart, long rangeEnd) {
        this.encryptionType = encryptionType;
        this.startByte = rangeStart;
        this.endByte = rangeEnd;
    }

    @BeforeClass
    public static void makeEncryptedData() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, IOException {
        byte[] plainData = new byte[MOCK_FILE_PLAIN_SIZE];
        for (int i = 0; i < plainData.length; i++) {
            plainData[i] = (byte) (i % 0xff);
        }
        encryptedData = new HashMap<>();
        encryptedData.put(ENCRYPTION_PLAIN, plainData);
        encryptedData.put(ENCRYPTION_AES128, encryptAes(plainData, 128));
        encryptedData.put(ENCRYPTION_AES256, encryptAes(plainData, 256));
    }

    @Parameterized.Parameters(name = "Encryption \"{0}\", range [{1}-{2}]")
    public static Collection<Object[]> parameters() {
        Collection<Object[]> params = new ArrayList<>();
        for (String encryptionType : new String[]{ENCRYPTION_PLAIN, ENCRYPTION_AES128, ENCRYPTION_AES256}) {
            for (long i = 0; i < MOCK_FILE_PLAIN_SIZE - 1; i += 10) {
                params.add(new Object[]{encryptionType, i, i + 9});
                params.add(new Object[]{encryptionType, i / 2, MOCK_FILE_PLAIN_SIZE - 1 - (i / 2)});
                params.add(new Object[]{encryptionType, 0, i});
                params.add(new Object[]{encryptionType, i, MOCK_FILE_PLAIN_SIZE - 1});
            }
        }
        return params;
    }

    private static byte[] encryptAes(byte[] plainData, int bits) throws NoSuchAlgorithmException, IOException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException {
        SecretKey secret = Glue.getInstance().getKey(EBINuFileServiceGetByteRangesTest.ENCRYPTION_KEY.toCharArray(), bits);

        byte[] random_iv = new byte[16];
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        random.nextBytes(random_iv);
        AlgorithmParameterSpec paramSpec = new IvParameterSpec(random_iv);

        try (ByteArrayOutputStream outStream = new ByteArrayOutputStream()) {
            outStream.write(random_iv);
            Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding"); // load a cipher AES / Segmented Integer Counter
            cipher.init(Cipher.ENCRYPT_MODE, secret, paramSpec);
            try (CipherOutputStream cipherStream = new CipherOutputStream(outStream, cipher)) {
                cipherStream.write(plainData);
            }

            return outStream.toByteArray();
        }
    }

    @Before
    public void setup() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);
        service.invalidateAESHeaderCache();
    }

    @Test
    public void canGetSpecificByteRange() throws RangesNotSatisfiableException, EgaFileNotFoundException, FileNotAvailableException, UnretrievableFileException, IOException, FireServiceException, ClientProtocolException, URISyntaxException {
        // Arrange
        // * Set up the data for the file in the mock FIRE client
        final String firePath = "file://path/to/test/file";
        byte[] payload = encryptedData.get(encryptionType);
        when(fireService.downloadByteRangeByPath(eq(firePath), anyLong(), anyLong()))
                .then(invocationOnMock -> {
                    int startByte = invocationOnMock.getArgumentAt(1, Long.class).intValue();
                    int endByte = invocationOnMock.getArgumentAt(2, Long.class).intValue();
                    return new ByteArrayInputStream(payload, startByte, endByte - startByte + 1);
                });

        // * Set up a mock file in the filedatabase
        File mockFile = new File();
        mockFile.setFileId(MOCK_FILE_ID);
        mockFile.setDisplayFilePath(firePath);
        mockFile.setFileSize(payload.length);
        mockFile.setFileStatus(FILE_STATUS_AVAILABLE);
        when(fileInfoService.getFileInfo(any())).thenReturn(mockFile);

        when(keyService.getEncryptionAlgorithm(MOCK_FILE_ID)).thenReturn(encryptionType);
        when(keyService.getFileKey(MOCK_FILE_ID)).thenReturn(ENCRYPTION_KEY);

        // Act: get the bytes from the file
        InputStream stream = service.getSpecificByteRange(MOCK_FILE_ID, startByte, endByte);

        // Assert: make sure we got the expected number of bytes and they are what we expected
        assertNotNull(stream);
        byte[] bytes = IOUtils.toByteArray(stream);
        assertEquals(endByte - startByte + 1, bytes.length);
        for (int i = 0; i < bytes.length; ++i) {
            assertEquals((byte) ((startByte + i) % 0xff), bytes[i]);
        }
    }

    @TestConfiguration
    protected static class Configuration {
        @Bean
        public EBINuFileService ebiNuFileService(KeyService keyService,
                                                 FileInfoService fileInfoService,
                                                 IFireService fireService) {
            return new EBINuFileService(keyService, fileInfoService, fireService);
        }
    }
}
