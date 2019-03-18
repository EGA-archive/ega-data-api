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
package eu.elixir.ega.ebi.dataedge.service.internal;

import static eu.elixir.ega.ebi.shared.Constants.FILEDATABASE_SERVICE;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.net.URI;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.elixir.ega.ebi.shared.dto.DownloadEntry;
import eu.elixir.ega.ebi.shared.dto.EventEntry;
import eu.elixir.ega.ebi.shared.service.internal.RemoteDownloaderLogServiceImpl;

/**
 * Test class for {@link RemoteDownloaderLogServiceImpl}.
 *
 * @author amohan
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(RemoteDownloaderLogServiceImpl.class)
@TestPropertySource(locations = "classpath:application-test.properties")
public class RemoteDownloaderLogServiceImplTest {

    @InjectMocks
    private RemoteDownloaderLogServiceImpl remoteDownloaderLogServiceImpl;

    @Mock
    private RestTemplate restTemplate;
    
    @Mock
    private ObjectMapper objectMapper;

    @Before
    public void initMocks() throws Exception {
        MockitoAnnotations.initMocks(this);

        final URI uriMock = mock(URI.class);
        final ResponseEntity<String> responseString = mock(ResponseEntity.class);

        whenNew(URI.class).withAnyArguments().thenReturn(uriMock);
        when(restTemplate.postForEntity(eq(FILEDATABASE_SERVICE + "/log/download/"), any(), eq(String.class))).thenReturn(responseString);
        when(restTemplate.postForEntity(eq(FILEDATABASE_SERVICE + "/log/event/"), any(), eq(String.class))).thenReturn(responseString);
    }

    /**
     * Test class for
     * {@link RemoteDownloaderLogServiceImpl#logDownload(DownloadEntry)}. Verify
     * code is executing without errors.
     */
    @Test
    public void testLogDownload() {
        try {
            final DownloadEntry downloadEntry = new DownloadEntry();
            downloadEntry.setFileId("fileId");
            remoteDownloaderLogServiceImpl.logDownload(downloadEntry);
        } catch (Exception e) {
            fail("Should not have thrown an exception");
        }

    }

    /**
     * Test class for {@link RemoteDownloaderLogServiceImpl#logEvent(EventEntry)}.
     * Verify code is executing without errors.
     */
    @Test
    public void testLogEvent() {

        try {
            final EventEntry eventEntry = new EventEntry();
            eventEntry.setEventId("eventId");

            remoteDownloaderLogServiceImpl.logEvent(eventEntry);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("Should not have thrown an exception");
        }

    }
}
