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

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import eu.elixir.ega.ebi.reencryptionmvc.service.KeyService;
import htsjdk.samtools.seekablestream.ISeekableStreamFactory;
import htsjdk.samtools.seekablestream.SeekableStream;
import htsjdk.samtools.seekablestream.cipher.SeekableAESCipherStream;

/**
 * Test class for {@link LocalEgaServiceImpl}.
 * 
 * @author amohan
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({LocalEgaServiceImpl.class, IOUtils.class})
public class LocalEgaServiceImplTest {

    @InjectMocks
    private LocalEgaServiceImpl localEgaServiceImpl;

    @Mock
    private ISeekableStreamFactory seekableStreamFactory;

    @Mock
    private KeyService keyService;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    /**
     * Test class for
     * {@link LocalEgaServiceImpl#transfer(String, String, String, String, String, String, long, long, long, String, String, HttpServletRequest, HttpServletResponse)}.
     * Verify code is executing without errors.
     * 
     * @throws Exception
     */
    @Test
    public void testTransfer() throws Exception {
        try {
            setupMock();
            localEgaServiceImpl.transfer("aes256", "sourceKey", "plain", "destinationKey", "destinationIV",
                    "/EGAZ00001257562/analysis/ALL.chr22.phase3_shapeit2_mvncall_integrated_v5a.20130502.genotypes.vcf.gz.cip",
                    0, 0, 37, "httpAuth", "id", new MockHttpServletRequest(), new MockHttpServletResponse());

        } catch (Exception e) {
            fail("Should not have thrown an exception");
        }
    }

    /**
     * Method to Setup mock.
     * 
     * @throws Exception
     */
    private void setupMock() throws Exception {

        final SeekableStream seekableStream = mock(SeekableStream.class);
        final SeekableAESCipherStream seekableAESCipherStream = mock(SeekableAESCipherStream.class);
        
        mockStatic(IOUtils.class);
        whenNew(SeekableAESCipherStream.class).withAnyArguments().thenReturn(seekableAESCipherStream);
        
        when(IOUtils.copyLarge(any(InputStream.class), any(OutputStream.class))).thenReturn(1L);        
        when(seekableStreamFactory.getStreamFor(any(String.class))).thenReturn(seekableStream);
        when(keyService.getRSAKeyById(any(String.class))).thenReturn("9B7D2C34A366BF890C730641E6CECF6F".getBytes());
        when(keyService.getPGPPublicKeyById(any(String.class))).thenReturn(null);

    }
}
