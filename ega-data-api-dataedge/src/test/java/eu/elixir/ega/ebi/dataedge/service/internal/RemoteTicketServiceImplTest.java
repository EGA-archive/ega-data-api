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

import eu.elixir.ega.ebi.dataedge.dto.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Test class for {@link RemoteTicketServiceImpl}.
 *
 * @author amohan
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(RemoteTicketServiceImpl.class)
@TestPropertySource(locations = "classpath:application-test.properties")
public class RemoteTicketServiceImplTest {

    public static final String SERVICE_URL = "http://FILEDATABASE";
    public static final String RES_URL = "http://RES2";
    public static final String FILEID = "fileId";
    public static final String DATASET1 = "DATASET1";
    public static final String DATASET2 = "DATASET2";

    private Authentication authentication;
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private RemoteTicketServiceImpl remoteTicketServiceImpl;

    @Mock
    MyExternalConfig externalConfig;

    @Mock
    RestTemplate restTemplate;

    /**
     * Test class for
     * {@link RemoteTicketServiceImpl#getFile(Authentication, String, String, String, String, long, long, HttpServletRequest, HttpServletResponse)}.
     * Verify the output url is as per the logic.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void testGetTicket() {
        final String[] fields = {"fields"};
        final String[] tagds = {"tagds"};
        final String[] notags = {"notags"};
        final List<String> fieldsList = Arrays.asList(fields);
        final List<String> tagsList = Arrays.asList(tagds);
        final List<String> notagsList = Arrays.asList(notags);

        final ResponseEntity<HtsgetContainer> responseEntity = (ResponseEntity) remoteTicketServiceImpl.getTicket(
                authentication, FILEID, "plain", 0, "referenceName", "referenceMD5", "start", "end", fieldsList,
                tagsList, notagsList, httpServletRequest, new MockHttpServletResponse());
        final HtsgetResponse htsgetResponse = (HtsgetResponse) responseEntity.getBody().getHtsget();

        assertThat(htsgetResponse.getUrls()[0].getUrl(), equalTo(
                "egaExternalUrlbyid/file?accession=fileId&format=plain&start=start&end=end&chr=referenceName&fields=fields&tags=tagds&notags=notags"));

    }

    /**
     * Test class for
     * {@link RemoteTicketServiceImpl#getVariantTicket(Authentication, String, String, int, String, String, String, String, List, List, List, HttpServletRequest, HttpServletResponse)}.
     * Verify the output url is as per the logic.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void testGetVariantTicket() {
        final String[] fields = {"fields"};
        final String[] tagds = {"tagds"};
        final String[] notags = {"notags"};
        final List<String> fieldsList = Arrays.asList(fields);
        final List<String> tagsList = Arrays.asList(tagds);
        final List<String> notagsList = Arrays.asList(notags);

        final ResponseEntity<HtsgetContainer> responseEntity = (ResponseEntity) remoteTicketServiceImpl
                .getVariantTicket(authentication, FILEID, "plain", 0, "referenceName", "referenceMD5", "start", "end",
                        fieldsList, tagsList, notagsList, httpServletRequest, new MockHttpServletResponse());
        final HtsgetResponse htsgetResponse = (HtsgetResponse) responseEntity.getBody().getHtsget();

        assertThat(htsgetResponse.getUrls()[0].getUrl(), equalTo(
                "egaExternalUrlvariant/byid/file?accession=fileId&format=plain&start=start&end=end&chr=referenceName&fields=fields&tags=tagds&notags=notags"));

    }

    /**
     * Test class for {@link RemoteTicketServiceImpl#resUrl()}. Verify the output
     * resUrl.
     */
    @Test
    public void testResUrl() {
        final String resUrl = remoteTicketServiceImpl.resUrl();
        assertThat(resUrl, equalTo(RES_URL));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        authentication = mock(Authentication.class);
        httpServletRequest = mock(HttpServletRequest.class);

        final Collection authorities = new ArrayList<GrantedAuthority>();
        authorities.add(new SimpleGrantedAuthority(DATASET1));
        authorities.add(new SimpleGrantedAuthority(DATASET2));

        final ResponseEntity<FileDataset[]> forEntityDataset = mock(ResponseEntity.class);
        final FileDataset[] datasets = {new FileDataset(FILEID, DATASET1)};
        when(forEntityDataset.getBody()).thenReturn(datasets);

        final ResponseEntity<File[]> forEntity = mock(ResponseEntity.class);
        final File file = new File();
        file.setFileId(FILEID);
        file.setFileName("fileName");
        file.setFileSize(100L);
        final File[] files = {file};
        when(forEntity.getBody()).thenReturn(files);

        when(externalConfig.getEgaExternalUrl()).thenReturn("egaExternalUrl");
        when(authentication.getAuthorities()).thenReturn(authorities);
        when(httpServletRequest.getHeader(any())).thenReturn("token");
        when(restTemplate.getForEntity(SERVICE_URL + "/file/{fileId}/datasets", FileDataset[].class, FILEID))
                .thenReturn(forEntityDataset);
        when(restTemplate.getForEntity(SERVICE_URL + "/file/{fileId}", File[].class, FILEID)).thenReturn(forEntity);
    }
}
