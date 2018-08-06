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
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import eu.elixir.ega.ebi.reencryptionmvc.dto.MyFireConfig;

/**
 * Test class for {@link ArchiveAdapterServiceImpl}.
 * 
 * @author amohan
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(ArchiveAdapterServiceImpl.class)
public class ArchiveAdapterServiceImplTest {

    @InjectMocks
    private ArchiveAdapterServiceImpl archiveAdapterServiceImpl;

    @Mock
    private MyFireConfig myFireConfig;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    /**
     * Test class for {@link ArchiveAdapterServiceImpl#getPath(String)}. Verify
     * output length and its values.
     * 
     * @throws Exception
     */
    @Test
    public void testGetPath() throws Exception {

        final String pathInput = "/EGAZ0000125/analysis/ALL.chr22.phase3_shapeit2_mvncall_integrated_v5a.20130502.genotypes.vcf.gz.cip";
        final String object_get = "http://read:8oSSaB@10.10.10.10/ega/06ca84a54eeadf8acd3cf05691652d5e0014";
        final String object_length = "214453766";
        final String object_storage_class = "CLEVERSAFE";

        final BufferedReader bufferedReaderMock = mock(BufferedReader.class);
        final URL urlMock = mock(URL.class);
        final HttpURLConnection connectionMock = mock(HttpURLConnection.class);

        whenNew(URL.class).withArguments(any(String.class)).thenReturn(urlMock);
        whenNew(BufferedReader.class).withArguments(any()).thenReturn(bufferedReaderMock);

        when(myFireConfig.getFireUrl()).thenReturn("testUrl");
        when(myFireConfig.getFireArchive()).thenReturn("fireArchive");
        when(myFireConfig.getFireKey()).thenReturn("fireKey");
        when(connectionMock.getResponseCode()).thenReturn(200);
        when(connectionMock.getInputStream()).thenReturn(new ByteArrayInputStream("test stream".getBytes()));
        when(urlMock.openConnection()).thenReturn(connectionMock);
        when(bufferedReaderMock.readLine()).thenReturn("BEGIN").thenReturn("OBJECT_GET " + object_get)
                .thenReturn(
                        "OBJECT_HEAD http://read:8oSSaB@10.10.10.10/ega/06ca84a54eeadf8acd3cf05691652d5e0014")
                .thenReturn("OBJECT_MD5 fa402efda9fdb3cf73799d854c52120c").thenReturn("OBJECT_LENGTH " + object_length)
                .thenReturn("OBJECT_URL_EXPIRE 2018-03-20T09:34:46.040868")
                .thenReturn("OBJECT_STORAGE_CLASS " + object_storage_class).thenReturn("END").thenReturn(null);

        final String[] ouput = archiveAdapterServiceImpl.getPath(pathInput);

        assertThat(ouput.length, equalTo(4));
        assertThat(ouput[0], equalTo(object_get));
        assertThat(ouput[1], equalTo(object_length));
        assertThat(ouput[2], equalTo(object_storage_class));
        assertThat(ouput[3], equalTo(pathInput));

    }

}
