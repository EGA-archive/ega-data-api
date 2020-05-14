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
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.ByteArrayInputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.cache2k.Cache;
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

import eu.elixir.ega.ebi.reencryptionmvc.cache2k.My2KCachePageFactory;
import eu.elixir.ega.ebi.reencryptionmvc.dto.EgaAESFileHeader;
import eu.elixir.ega.ebi.reencryptionmvc.dto.MyAwsConfig;
import eu.elixir.ega.ebi.reencryptionmvc.service.KeyService;
import eu.elixir.ega.ebi.reencryptionmvc.util.FireCommons;
import eu.elixir.ega.ebi.reencryptionmvc.util.S3Commons;

/**
 * Test class for {@link CacheResServiceImpl}.
 *
 * @author amohan
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({CacheResServiceImpl.class, HttpClientBuilder.class})
public class CacheResServiceImplTest {

    @InjectMocks
    private CacheResServiceImpl cacheResServiceImpl;

    @Mock
    private KeyService keyService;

    @Mock
    private MyAwsConfig myAwsConfig;

    @Mock
    private Cache<String, EgaAESFileHeader> myHeaderCache;

    @Mock
    private My2KCachePageFactory pageDowloader;
    
    @Mock
    private FireCommons fireCommons;
    
    @Mock
    private S3Commons s3Commons;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    /**
     * Test class for
     * {@link CacheResServiceImpl#transfer(String, String, String, String, String, String, String, long, long, long, String, String, HttpServletRequest, HttpServletResponse)}.
     * Verify code is executing without errors and also verifies only one call goes
     * to myPageCache.
     */
    @Test
    public void testTransfer() {
        try {
            setupMock();
            cacheResServiceImpl.transfer("aes256", "sourceKey", "sourceIV", "plain", "destinationKey", "destinationIV",
                    "/EGAZ00001257562/analysis/ALL.chr22.phase3_shapeit2_mvncall_integrated_v5a.20130502.genotypes.vcf.gz.cip",
                    0, 0, 37, "httpAuth", "id", new MockHttpServletRequest(), new MockHttpServletResponse());
            verify(pageDowloader, times(1)).downloadPage(anyString());
        } catch (Exception e) {
            fail("Should not have thrown an exception");
        }
    }

    /**
     * Method to Setup mock.
     *
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    private void setupMock() throws Exception {
        final String inputData = "test output forty one characters given.";

        final HttpClientBuilder mockHttpClientBuilder = mock(HttpClientBuilder.class);
        final CloseableHttpClient mockCloseableHttpClient = mock(CloseableHttpClient.class);
        final CloseableHttpResponse mockHttpResponse = mock(CloseableHttpResponse.class);
        final HttpEntity mockHttpEntity = mock(HttpEntity.class);

        mockStatic(HttpClientBuilder.class);
        when(HttpClientBuilder.create()).thenReturn(mockHttpClientBuilder);
        when(HttpClientBuilder.create()).thenReturn(mockHttpClientBuilder);
        when(mockHttpClientBuilder.build()).thenReturn(mockCloseableHttpClient);

        when(mockCloseableHttpClient.execute(any())).thenReturn(mockHttpResponse);
        when(mockHttpResponse.getEntity()).thenReturn(mockHttpEntity);
        when(mockHttpEntity.getContent()).thenReturn(new ByteArrayInputStream(inputData.getBytes()));
        when(myAwsConfig.getAwsAccessKeyId()).thenReturn("accessKeyId");
        when(myAwsConfig.getAwsSecretAccessKey()).thenReturn("secretAccesskey");
        when(myHeaderCache.containsKey(any())).thenReturn(Boolean.FALSE);
        when(pageDowloader.downloadPage(anyString())).thenReturn(new byte[] {});
    }

}
