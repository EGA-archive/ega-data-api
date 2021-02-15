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

import eu.elixir.ega.ebi.commons.exception.NoContentException;
import eu.elixir.ega.ebi.commons.exception.UnavailableForLegalReasonsException;
import eu.elixir.ega.ebi.commons.shared.dto.File;
import eu.elixir.ega.ebi.commons.shared.dto.FileDataset;
import eu.elixir.ega.ebi.commons.shared.dto.FileIndexFile;
import eu.elixir.ega.ebi.commons.shared.dto.MyExternalConfig;
import eu.elixir.ega.ebi.commons.shared.service.DownloaderLogService;
import eu.elixir.ega.ebi.commons.shared.service.FileInfoService;
import eu.elixir.ega.ebi.dataedge.dto.HttpResult;
import eu.elixir.ega.ebi.dataedge.service.FileLengthService;
import eu.elixir.ega.ebi.dataedge.service.KeyService;
import eu.elixir.ega.ebi.dataedge.utils.SimpleSeekableStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

import static eu.elixir.ega.ebi.commons.config.Constants.FILEDATABASE_SERVICE;
import static eu.elixir.ega.ebi.commons.config.Constants.RES_SERVICE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

/**
 * Test class for {@link RemoteFileServiceImpl}.
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({RemoteFileServiceImpl.class})
@TestPropertySource(locations = "classpath:application-test.properties")
public class RemoteFileServiceImplTest {

    public static final String DATASET1 = "DATASET1";
    public static final String DATASET2 = "DATASET2";
    public static final String FILEID = "fileId";

    private Authentication authentication;

    @InjectMocks
    private RemoteFileServiceImpl remoteFileServiceImpl;

    @Mock
    private LoadBalancerClient loadBalancer;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    MyExternalConfig externalConfig;

    @Mock
    private DownloaderLogService downloaderLogService;

    @Mock
    private FileInfoService fileInfoService;
    
    @Mock
    private FileLengthService fileLengthService;
    
    @Mock
    private KeyService keyService;

    /**
     * Test class for
     * {@link RemoteFileServiceImpl#getFile(Authentication, String, String, String, String, long, long, HttpServletRequest, HttpServletResponse)}.
     * Verify code is executing without errors.
     */
    @Test
    public void testGetFile() {
        try {
            remoteFileServiceImpl.getFile(FILEID, "plain", "destinationKey", "destinationIV", 0, 0,
                    new MockHttpServletRequest(), new MockHttpServletResponse());
        } catch (Exception e) {
            fail("Should not have thrown an exception");
        }

    }

    @Test(expected = NoContentException.class)
    public void getFile_WhenGivenFileEncryptionAlgorithmGpg_ThenThrowsNoContentException() {
        when(keyService.getEncryptionAlgorithm(FILEID)).thenReturn("gpg");
        remoteFileServiceImpl.getFile(FILEID, "plain", "destinationKey", "destinationIV", 0, 0,
                new MockHttpServletRequest(), new MockHttpServletResponse());
    }

    @Test(expected = UnavailableForLegalReasonsException.class)
    public void getFile_WhenGivenFileStatusUnavailable_ThenThrowsUnavailableForLegalReasonsException() {
        final File file = new File();
        file.setFileId(FILEID);
        file.setFileName("fileName");
        file.setFileSize(100L);
        file.setFileStatus("unavailable");
        when(fileInfoService.getFileInfo(FILEID)).thenReturn(file);

        remoteFileServiceImpl.getFile(FILEID, "plain", "destinationKey", "destinationIV", 0, 0,
                new MockHttpServletRequest(), new MockHttpServletResponse());
    }
    
    /**
     * Test class for
     * {@link RemoteFileServiceImpl#getFileHead(Authentication, String, String, HttpServletRequest, HttpServletResponse)}.
     * Verify code is executing without errors.
     */
    @Test
    public void testGetFileHead() {
        try {
            remoteFileServiceImpl.getFileHead(FILEID, "plain", new MockHttpServletRequest(),
                    new MockHttpServletResponse());
        } catch (Exception e) {
            fail("Should not have thrown an exception");
        }
    }

    /**
     * Test class for {@link RemoteFileServiceImpl#resURL()}. Verify the output
     * resURL.
     */
    @Test
    public void testResURL() {
        final String resURL = remoteFileServiceImpl.resURL();
        assertThat(resURL, equalTo(RES_SERVICE));
    }

    /**
     * Test class for {@link RemoteFileServiceImpl#fileDatabaseURL()}. Verify the
     * output downloadUrl.
     */
    @Test
    public void testFileDatabaseURL() {
        final String downloadURL = remoteFileServiceImpl.fileDatabaseURL();
        assertThat(downloadURL, equalTo(FILEDATABASE_SERVICE));
    }

    /**
     * Test class for
     * {@link RemoteFileServiceImpl#getHeadById(Authentication, String, String, HttpServletRequest, HttpServletResponse)}.
     * Verify the response status code.
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void testGetHeadById() {
        final ResponseEntity response = remoteFileServiceImpl.getHeadById( "file", FILEID,
                new MockHttpServletRequest(), new MockHttpServletResponse());
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Before
    public void initMocks() throws Exception {
        MockitoAnnotations.initMocks(this);

        final Collection authorities = new ArrayList<GrantedAuthority>();
        authorities.add(new SimpleGrantedAuthority(DATASET1));
        authorities.add(new SimpleGrantedAuthority(DATASET2));

        final ResponseEntity<FileDataset[]> forEntityDataset = mock(ResponseEntity.class);
        final ResponseEntity<File[]> forEntity = mock(ResponseEntity.class);
        final ResponseEntity<Long> forSize = mock(ResponseEntity.class);
        final ResponseEntity<FileIndexFile[]> forResponseEntity = mock(ResponseEntity.class);
        final HttpResult xferResult = mock(HttpResult.class);
        final SimpleSeekableStream simpleSeekableStream = mock(SimpleSeekableStream.class);

        final FileDataset[] datasets = {new FileDataset(FILEID, DATASET1)};
        final File f = new File();
        f.setFileId(FILEID);
        f.setFileName("fileName");
        f.setFileStatus("available");
        f.setFileSize(100L);
        final File[] file = {f};
        authentication = mock(Authentication.class);
        final FileIndexFile fi = new FileIndexFile();
        fi.setFileId(FILEID);
        fi.setIndexFileId("indexFileId");
        final FileIndexFile[] fileIndexFiles = {fi};

        when(fileInfoService.getFileInfo(FILEID)).thenReturn(f);
        when(fileInfoService.getFileInfo("indexFileId")).thenReturn(f);

        when(authentication.getAuthorities()).thenReturn(authorities);
        when(forEntityDataset.getBody()).thenReturn(datasets);
        when(forEntity.getBody()).thenReturn(file);
        when(forSize.getBody()).thenReturn(1000L);
        when(forResponseEntity.getBody()).thenReturn(fileIndexFiles);

        whenNew(SimpleSeekableStream.class).withAnyArguments().thenReturn(simpleSeekableStream);

        when(keyService.getEncryptionAlgorithm(FILEID)).thenReturn("aes256");
        FILEDATABASE_SERVICE = "http://filedatabase/";
        RES_SERVICE = "http://res2/";

        SimpleDiscoveryProperties.SimpleServiceInstance fileDatabaseServiceInstance = new SimpleDiscoveryProperties.SimpleServiceInstance(new URL(FILEDATABASE_SERVICE).toURI());
        when(loadBalancer.choose("FILEDATABASE")).thenReturn(fileDatabaseServiceInstance);
        SimpleDiscoveryProperties.SimpleServiceInstance resServiceInstance = new SimpleDiscoveryProperties.SimpleServiceInstance(new URL(RES_SERVICE).toURI());
        when(loadBalancer.choose("RES2")).thenReturn(resServiceInstance);

        when(restTemplate.getForEntity(FILEDATABASE_SERVICE + "/file/{fileId}/datasets", FileDataset[].class, FILEID))
                .thenReturn(forEntityDataset);
        when(restTemplate.getForEntity(FILEDATABASE_SERVICE + "/file/{fileId}/datasets", FileDataset[].class, "indexFileId"))
                .thenReturn(forEntityDataset);
        when(restTemplate.getForEntity(FILEDATABASE_SERVICE + "/file/{fileId}", File[].class, FILEID)).thenReturn(forEntity);
        when(restTemplate.getForEntity(FILEDATABASE_SERVICE + "/file/{fileId}", File[].class, "indexFileId"))
                .thenReturn(forEntity);
        when(restTemplate.getForEntity(FILEDATABASE_SERVICE + "/file/{fileId}/index", FileIndexFile[].class, FILEID))
                .thenReturn(forResponseEntity);
        when(restTemplate.getForEntity(RES_SERVICE + "/file/archive/{fileId}/size", Long.class, FILEID))
                .thenReturn(forSize);
        when(restTemplate.execute(any(), any(), any(), any())).thenReturn(xferResult);
    }

}
