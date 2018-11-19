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

import eu.elixir.ega.ebi.dataedge.service.FileInfoService;
import eu.elixir.ega.ebi.shared.dto.File;
import eu.elixir.ega.ebi.shared.dto.FileDataset;
import eu.elixir.ega.ebi.dataedge.service.DownloaderLogService;
import htsjdk.samtools.cram.ref.CRAMReferenceSource;
import org.apache.commons.lang.NotImplementedException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static eu.elixir.ega.ebi.shared.Constants.FILEDATABASE_SERVICE;
import static eu.elixir.ega.ebi.shared.Constants.RES_SERVICE;
import static org.junit.Assert.fail;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Test class for {@link LocalEGARemoteFileServiceImpl}.
 *
 * @author amohan
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(LocalEGARemoteFileServiceImpl.class)
@TestPropertySource(locations = "classpath:application-test.properties")
public class LocalEGARemoteFileServiceImplTest {

    public static final String DATASET1 = "DATASET1";
    public static final String DATASET2 = "DATASET2";
    public static final String FILEID = "fileId";

    private Authentication auth;

    @InjectMocks
    private LocalEGARemoteFileServiceImpl localEGARemoteFileServiceImpl;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private DownloaderLogService downloaderLogService;

    @Mock
    private FileInfoService fileInfoService;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    /**
     * Test class for
     * {@link LocalEGARemoteFileServiceImpl#getFile(Authentication, String, String, String, String, long, long, HttpServletRequest, HttpServletResponse) }.
     * Verify code is executing without errors.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testGetFile() {

        try {
            auth = mock(Authentication.class);
            final Collection authorities = new ArrayList<GrantedAuthority>();
            authorities.add(new SimpleGrantedAuthority(DATASET1));
            authorities.add(new SimpleGrantedAuthority(DATASET2));

            final ResponseEntity<FileDataset[]> forEntityDataset = mock(ResponseEntity.class);
            final ResponseEntity<File[]> forEntity = mock(ResponseEntity.class);
            final ResponseEntity<Long> forSize = mock(ResponseEntity.class);

            final FileDataset[] datasets = {new FileDataset(FILEID, DATASET1)};
            final File f = new File();
            f.setFileId(FILEID);
            final File[] file = {f};

            when(auth.getAuthorities()).thenReturn(authorities);
            when(forEntityDataset.getBody()).thenReturn(datasets);
            when(forEntity.getBody()).thenReturn(file);
            when(forSize.getBody()).thenReturn(1000l);
            when(fileInfoService.getFileInfo(FILEID)).thenReturn(f);

            when(restTemplate.getForEntity(FILEDATABASE_SERVICE + "/file/{fileId}/datasets", FileDataset[].class, FILEID))
                    .thenReturn(forEntityDataset);
            when(restTemplate.getForEntity(FILEDATABASE_SERVICE + "/file/{fileId}", File[].class, FILEID))
                    .thenReturn(forEntity);
            when(restTemplate.getForEntity(RES_SERVICE + "/file/archive/{fileId}/size", Long.class, FILEID))
                    .thenReturn(forSize);

            localEGARemoteFileServiceImpl.getFile( FILEID, "plain", "destinationKey", "destinationIV", 0, 0,
                    new MockHttpServletRequest(), new MockHttpServletResponse());
        } catch (Exception e) {
            fail("Should not have thrown an exception");
        }

    }

    /**
     * Test class for
     * {@link LocalEGARemoteFileServiceImpl#getFileHead(Authentication, String, String, HttpServletRequest, HttpServletResponse)}.
     * Expected exception as operation Not supported yet.
     */
    @Test(expected = NotImplementedException.class)
    public void testGetFileHead() {
        localEGARemoteFileServiceImpl.getFileHead( FILEID, "plain", new MockHttpServletRequest(),
                new MockHttpServletResponse());
    }

    /**
     * Test class for
     * {@link LocalEGARemoteFileServiceImpl#getFileHeader(Authentication, String, String, String, CRAMReferenceSource)}.
     * Expected exception as operation Not supported yet.
     */
    @Test(expected = NotImplementedException.class)
    public void testGetFileHeader() {
        localEGARemoteFileServiceImpl.getFileHeader( FILEID, "plain", "destinationKey", null);
    }

    /**
     * Test class for
     * {@link LocalEGARemoteFileServiceImpl#getById(Authentication, String, String, String, String, long, long, List, List, List, boolean, String, String, HttpServletRequest, HttpServletResponse)}.
     * Expected exception as operation Not supported yet.
     */
    @Test(expected = NotImplementedException.class)
    public void testGetById() {
        localEGARemoteFileServiceImpl.getById( "idType", "accession", "plain", "reference", 0, 0, null, null, null,
                true, "destinationFormat", "destinationKey", new MockHttpServletRequest(),
                new MockHttpServletResponse());

    }

    /**
     * Test class for
     * {@link LocalEGARemoteFileServiceImpl#getVCFById(Authentication, String, String, String, String, long, long, List, List, List, boolean, String, String, HttpServletRequest, HttpServletResponse)}.
     * Expected exception as operation Not supported yet.
     */
    @Test(expected = NotImplementedException.class)
    public void testGetVCFById() {
        localEGARemoteFileServiceImpl.getVCFById( "idType", "accession", "plain", "reference", 0, 0, null, null,
                null, true, "destinationFormat", "destinationKey", new MockHttpServletRequest(),
                new MockHttpServletResponse());
    }

    /**
     * Test class for
     * {@link LocalEGARemoteFileServiceImpl#getHeadById(Authentication, String, String, HttpServletRequest, HttpServletResponse)}.
     * Expected exception as operation Not supported yet.
     */
    @Test(expected = NotImplementedException.class)
    public void testGetHeadById() {
        localEGARemoteFileServiceImpl.getHeadById( "idType", "accession", new MockHttpServletRequest(),
                new MockHttpServletResponse());
    }

}
