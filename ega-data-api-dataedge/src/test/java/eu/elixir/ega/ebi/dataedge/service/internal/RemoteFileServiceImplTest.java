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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
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

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;

import eu.elixir.ega.ebi.dataedge.dto.File;
import eu.elixir.ega.ebi.dataedge.dto.FileDataset;
import eu.elixir.ega.ebi.dataedge.dto.FileIndexFile;
import eu.elixir.ega.ebi.dataedge.dto.HttpResult;
import eu.elixir.ega.ebi.dataedge.dto.MyExternalConfig;
import eu.elixir.ega.ebi.dataedge.service.DownloaderLogService;
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

/**
 * Test class for {@link RemoteFileServiceImpl}.
 * 
 * @author amohan
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ RemoteFileServiceImpl.class, SamReaderFactory.class })
@TestPropertySource(locations = "classpath:application-test.properties")
public class RemoteFileServiceImplTest {

    private final String SERVICE_URL = "http://FILEDATABASE";
    private final String RES_URL = "http://RES2";
    private final String DATASET1 = "DATASET1";
    private final String DATASET2 = "DATASET2";
    private final String FILEID = "fileId";
    private final String HOMEPAGE_URL = "http://HomePageUrl";

    private Authentication authentication;
    private SAMFileHeader samFileHeader;

    @InjectMocks
    private RemoteFileServiceImpl remoteFileServiceImpl;

    @Mock
    RestTemplate restTemplate;

    @Mock
    RetryTemplate retryTemplate;

    @Mock
    MyExternalConfig externalConfig;

    @Mock
    private DownloaderLogService downloaderLogService;

    @Mock
    private EurekaClient discoveryClient;

    /**
     * Test class for
     * {@link RemoteFileServiceImpl#getFile(Authentication, String, String, String, String, long, long, HttpServletRequest, HttpServletResponse)}.
     * Verify code is executing without errors.
     */
    @Test
    public void testGetFile() {
        try {
            remoteFileServiceImpl.getFile(authentication, FILEID, "plain", "destinationKey", "destinationIV", 0, 0,
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
            remoteFileServiceImpl.getFileHead(authentication, FILEID, "plain", new MockHttpServletRequest(),
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
            final Object samFileHeaderOutput = remoteFileServiceImpl.getFileHeader(authentication, FILEID, "plain",
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

            remoteFileServiceImpl.getById(authentication, "file", FILEID, "plain", "reference", 0, 0, null, null, null,
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
            remoteFileServiceImpl.getVCFById(authentication, "file", FILEID, "plain", "reference", 0, 0, null, null,
                    null, true, "destinationFormat", "destinationKey", new MockHttpServletRequest(),
                    new MockHttpServletResponse());

        } catch (Exception e) {
            fail("Should not have thrown an exception");
        }
    }

    /**
     * Test class for {@link RemoteFileServiceImpl#resUrl()}. Verify the output
     * resUrl.
     */
    @Test
    public void testResUrl() {
        final String resUrl = remoteFileServiceImpl.resUrl();
        assertThat(resUrl, equalTo(HOMEPAGE_URL));
    }

    /**
     * Test class for {@link RemoteFileServiceImpl#downloaderUrl()}. Verify the
     * output downloadUrl.
     */
    @Test
    public void testDownloadUrl() {
        final String downloadUrl = remoteFileServiceImpl.downloaderUrl();
        assertThat(downloadUrl, equalTo(HOMEPAGE_URL));
    }

    /**
     * Test class for
     * {@link RemoteFileServiceImpl#getHeadById(Authentication, String, String, HttpServletRequest, HttpServletResponse)}.
     * Verify the response status code.
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void testGetHeadById() {
        final ResponseEntity response = remoteFileServiceImpl.getHeadById(authentication, "file", FILEID,
                new MockHttpServletRequest(), new MockHttpServletResponse());
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
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
        final InstanceInfo instance = mock(InstanceInfo.class);
        final EgaSeekableCachedResStream egaSeekableCachedResStream = mock(EgaSeekableCachedResStream.class);
        final SamReaderFactory samReaderFactory = mock(SamReaderFactory.class);
        final SamReader samReader = mock(SamReader.class);
        final MyVCFFileReader myVCFFileReader = mock(MyVCFFileReader.class);
        final VCFHeader vcfHeader = mock(VCFHeader.class);
        final CloseableIterator<VariantContext> closeableIterator = mock(CloseableIterator.class);

        final FileDataset[] datasets = { new FileDataset(FILEID, DATASET1) };
        final File f = new File();
        f.setFileId(FILEID);
        f.setFileName("fileName");
        f.setFileSize(100l);
        final File[] file = { f };
        authentication = mock(Authentication.class);
        samFileHeader = mock(SAMFileHeader.class);
        final FileIndexFile fi = new FileIndexFile();
        fi.setFileId(FILEID);
        fi.setIndexFileId("indexFileId");
        final FileIndexFile[] fileIndexFiles = { fi };

        when(authentication.getAuthorities()).thenReturn(authorities);
        when(forEntityDataset.getBody()).thenReturn(datasets);
        when(forEntity.getBody()).thenReturn(file);
        when(forSize.getBody()).thenReturn(1000l);
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

        when(restTemplate.getForEntity(SERVICE_URL + "/file/{file_id}/datasets", FileDataset[].class, FILEID))
                .thenReturn(forEntityDataset);
        when(restTemplate.getForEntity(SERVICE_URL + "/file/{file_id}/datasets", FileDataset[].class, "indexFileId"))
                .thenReturn(forEntityDataset);
        when(restTemplate.getForEntity(SERVICE_URL + "/file/{file_id}", File[].class, FILEID)).thenReturn(forEntity);
        when(restTemplate.getForEntity(SERVICE_URL + "/file/{file_id}", File[].class, "indexFileId"))
                .thenReturn(forEntity);
        when(restTemplate.getForEntity(SERVICE_URL + "/file/{file_id}/index", FileIndexFile[].class, FILEID))
                .thenReturn(forResponseEntity);
        when(restTemplate.getForEntity(RES_URL + "/file/archive/{file_id}/size", Long.class, FILEID))
                .thenReturn(forSize);
        when(restTemplate.execute(any(), any(), any(), any())).thenReturn(xferResult);
        when(instance.getHomePageUrl()).thenReturn(HOMEPAGE_URL);
        when(discoveryClient.getNextServerFromEureka("RES2", false)).thenReturn(instance);
        when(discoveryClient.getNextServerFromEureka("FILEDATABASE", false)).thenReturn(instance);
    }

}
