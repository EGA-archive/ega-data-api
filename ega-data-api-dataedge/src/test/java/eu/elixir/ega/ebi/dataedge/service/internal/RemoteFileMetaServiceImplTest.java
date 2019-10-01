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

import eu.elixir.ega.ebi.shared.dto.DownloadEntry;
import eu.elixir.ega.ebi.shared.dto.File;
import eu.elixir.ega.ebi.shared.dto.FileDataset;
import eu.elixir.ega.ebi.shared.service.internal.RemoteDownloaderLogServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collection;

import static eu.elixir.ega.ebi.dataedge.config.Constants.FILEDATABASE_SERVICE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Test class for {@link RemoteFileMetaServiceImpl}.
 *
 * @author amohan
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(RemoteFileMetaServiceImpl.class)
@TestPropertySource(locations = "classpath:application-test.properties")
public class RemoteFileMetaServiceImplTest {

    public static final String DATASET1 = "DATASET1";
    public static final String DATASET2 = "DATASET2";
    public static final String FILEID = "fileId";

    @InjectMocks
    private RemoteFileMetaServiceImpl remoteFileMetaServiceImpl;

    @Mock
    RestTemplate syncRestTemplate;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    /**
     * Test class for
     * {@link RemoteDownloaderLogServiceImpl#logDownload(DownloadEntry)}. Verify the
     * fileOutput datasetId.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void testLogDownload() {
        final Authentication auth = mock(Authentication.class);
        final ResponseEntity<FileDataset[]> forEntityDataset = mock(ResponseEntity.class);
        final FileDataset[] datasets = {new FileDataset(FILEID, DATASET1)};
        final Collection authorities = new ArrayList<GrantedAuthority>();
        authorities.add(new SimpleGrantedAuthority(DATASET1));
        authorities.add(new SimpleGrantedAuthority(DATASET2));
        final ResponseEntity<File[]> forEntity = mock(ResponseEntity.class);
        final File f = new File();
        f.setFileId(FILEID);
        final File[] files = {f};

        when(auth.getAuthorities()).thenReturn(authorities);
        when(syncRestTemplate.getForEntity(FILEDATABASE_SERVICE + "/file/{fileId}/datasets", FileDataset[].class, FILEID))
                .thenReturn(forEntityDataset);
        when(syncRestTemplate.getForEntity(FILEDATABASE_SERVICE + "/file/{fileId}", File[].class, FILEID))
                .thenReturn(forEntity);
        when(forEntityDataset.getBody()).thenReturn(datasets);
        when(forEntity.getBody()).thenReturn(files);

        final File fileOutput = remoteFileMetaServiceImpl.getFile(auth, FILEID);

        assertThat(fileOutput.getDatasetId(), equalTo(DATASET1));
    }

    /**
     * Test class for
     * {@link RemoteDownloaderLogServiceImpl#logDownload(DownloadEntry)}. Verify the
     * fileOutput fileId.
     */
    @Test
    public void testGetDatasetFiles() {
        final File f = new File();
        f.setFileId(FILEID);
        final File[] files = {f};

        when(syncRestTemplate.getForObject(FILEDATABASE_SERVICE + "/datasets/{datasetId}/files", File[].class, DATASET1))
                .thenReturn(files);

        final Iterable<File> fileOutput = remoteFileMetaServiceImpl.getDatasetFiles(DATASET1);

        assertThat(fileOutput.iterator().next().getFileId(), equalTo(f.getFileId()));
    }

}
