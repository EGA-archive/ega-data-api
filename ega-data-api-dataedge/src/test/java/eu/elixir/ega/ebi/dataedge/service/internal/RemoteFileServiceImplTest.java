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
import eu.elixir.ega.ebi.dataedge.service.DownloaderLogService;
import eu.elixir.ega.ebi.dataedge.service.FileInfoService;
import eu.elixir.ega.ebi.dataedge.service.PermissionsService;
import eu.elixir.ega.ebi.shared.dto.File;
import eu.elixir.ega.ebi.shared.dto.FileDataset;
import eu.elixir.ega.ebi.shared.dto.FileIndexFile;
import eu.elixir.ega.ebi.shared.dto.MyExternalConfig;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.cram.ref.CRAMReferenceSource;
import htsjdk.samtools.seekablestream.EgaSeekableCachedResStream;
import htsjdk.samtools.util.CloseableIterator;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.MyVCFFileReader;
import htsjdk.variant.vcf.VCFHeader;
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
import org.springframework.retry.support.RetryTemplate;
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
import java.util.List;

import static eu.elixir.ega.ebi.shared.Constants.FILEDATABASE_SERVICE;
import static eu.elixir.ega.ebi.shared.Constants.RES_SERVICE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.*;

/**
 * Test class for {@link RemoteFileServiceImpl}.
 *
 * @author amohan
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({RemoteFileServiceImpl.class, SamReaderFactory.class})
@TestPropertySource(locations = "classpath:application-test.properties")
public class RemoteFileServiceImplTest {

    public static final String DATASET1 = "DATASET1";
    public static final String DATASET2 = "DATASET2";
    public static final String FILEID = "fileId";

    private Authentication authentication;
    private SAMFileHeader samFileHeader;

    @InjectMocks
    private RemoteFileServiceImpl remoteFileServiceImpl;

    @Mock
    private LoadBalancerClient loadBalancer;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private RetryTemplate retryTemplate;

    @Mock
    MyExternalConfig externalConfig;

    @Mock
    private DownloaderLogService downloaderLogService;

    @Mock
    private FileInfoService fileInfoService;

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
     * Test class for
     * {@link RemoteFileServiceImpl#getFileHeader(Authentication, String, String, String, CRAMReferenceSource)}.
     * Verify code is executing without errors and checking output
     * samFileHeaderOutput.
     */
    @Test
    public void testGetFileHeader() {
        try {
            final CRAMReferenceSource cramReferenceSource = mock(CRAMReferenceSource.class);
            final Object samFileHeaderOutput = remoteFileServiceImpl.getFileHeader(FILEID, "plain",
                    "destinationKey", cramReferenceSource);
            assertThat(samFileHeaderOutput, equalTo(samFileHeader));
        } catch (Exception e) {
            fail("Should not have thrown an exception");
        }
    }

    /**
     * Test class for
     * {@link RemoteFileServiceImpl#getById(Authentication, String, String, String, String, long, long, List, List, List, boolean, String, String, HttpServletRequest, HttpServletResponse)}.
     * Verify code is executing without errors.
     */
    @Test
    public void testGetById() {
        try {
            remoteFileServiceImpl.getById("file", FILEID, "plain", "reference", 0, 0, null, null, null,
                    true, "destinationFormat", "destinationKey", new MockHttpServletRequest(),
                    new MockHttpServletResponse());
        } catch (Exception e) {
            fail("Should not have thrown an exception");
        }
    }

    /**
     * Test class for
     * {@link RemoteFileServiceImpl#getVCFById(Authentication, String, String, String, String, long, long, List, List, List, boolean, String, String, HttpServletRequest, HttpServletResponse)}.
     * Verify code is executing without errors.
     */
    @Test
    public void testGetVCFById() {
        try {
            remoteFileServiceImpl.getVCFById("file", FILEID, "plain", "reference", 0, 0, null, null,
                    null, true, "destinationFormat", "destinationKey", new MockHttpServletRequest(),
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
        assertThat(resURL, equalTo("http://res/"));
    }

    /**
     * Test class for {@link RemoteFileServiceImpl#fileDatabaseURL()}. Verify the
     * output downloadUrl.
     */
    @Test
    public void testFileDatabaseURL() {
        final String downloadURL = remoteFileServiceImpl.fileDatabaseURL();
        assertThat(downloadURL, equalTo("http://filedatabase/"));
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
        final EgaSeekableCachedResStream egaSeekableCachedResStream = mock(EgaSeekableCachedResStream.class);
        final SamReaderFactory samReaderFactory = mock(SamReaderFactory.class);
        final SamReader samReader = mock(SamReader.class);
        final MyVCFFileReader myVCFFileReader = mock(MyVCFFileReader.class);
        final VCFHeader vcfHeader = mock(VCFHeader.class);
        final CloseableIterator<VariantContext> closeableIterator = mock(CloseableIterator.class);

        final FileDataset[] datasets = {new FileDataset(FILEID, DATASET1)};
        final File f = new File();
        f.setFileId(FILEID);
        f.setFileName("fileName");
        f.setFileSize(100L);
        final File[] file = {f};
        authentication = mock(Authentication.class);
        samFileHeader = mock(SAMFileHeader.class);
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
        when(myVCFFileReader.getFileHeader()).thenReturn(vcfHeader);
        when(myVCFFileReader.iterator()).thenReturn(closeableIterator);

        whenNew(EgaSeekableCachedResStream.class).withAnyArguments().thenReturn(egaSeekableCachedResStream);
        whenNew(MyVCFFileReader.class).withAnyArguments().thenReturn(myVCFFileReader);

        mockStatic(SamReaderFactory.class);
        when(SamReaderFactory.make()).thenReturn(samReaderFactory);
        when(samReaderFactory.referenceSource(any())).thenReturn(samReaderFactory);
        when(samReaderFactory.validationStringency(any())).thenReturn(samReaderFactory);
        when(samReaderFactory.enable(any())).thenReturn(samReaderFactory);
        when(samReaderFactory.samRecordFactory(any())).thenReturn(samReaderFactory);
        when(samReaderFactory.open(any(SamInputResource.class))).thenReturn(samReader);
        when(samReader.getFileHeader()).thenReturn(samFileHeader);

        SimpleDiscoveryProperties.SimpleServiceInstance fileDatabaseServiceInstance = new SimpleDiscoveryProperties.SimpleServiceInstance(new URL("http://filedatabase/").toURI());
        when(loadBalancer.choose("FILEDATABASE")).thenReturn(fileDatabaseServiceInstance);
        SimpleDiscoveryProperties.SimpleServiceInstance resServiceInstance = new SimpleDiscoveryProperties.SimpleServiceInstance(new URL("http://res/").toURI());
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
