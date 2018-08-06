/*
 * Copyright 2016 ELIXIR EGA
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
package eu.elixir.ega.ebi.reencryptionmvc.rest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cache2k.Cache;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import eu.elixir.ega.ebi.reencryptionmvc.dto.ArchiveSource;
import eu.elixir.ega.ebi.reencryptionmvc.dto.CachePage;
import eu.elixir.ega.ebi.reencryptionmvc.service.ArchiveService;
import eu.elixir.ega.ebi.reencryptionmvc.service.ResService;

/**
 * Test class for {@link FileController}.
 * 
 * @author amohan
 */
@RunWith(SpringRunner.class)
@WebMvcTest(FileController.class)
@TestPropertySource(locations = "classpath:application-test.properties")
public class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ResService resService;
    
    @MockBean
    private Cache<String, CachePage> myPageCache;

    @MockBean
    private ArchiveService archiveService;

    /**
     * Test
     * {@link FileController#getFile(String, String, String, String, String, long, long, long, String, String, HttpServletRequest, HttpServletResponse)}.
     * Verify the api call returns status is OK.
     * 
     * @throws Exception
     */
    @Test
    public void testGetFile() throws Exception {

        commonMockMethod();
        final MockHttpServletResponse response = mockMvc.perform(
                get("/file").param("filePath", "/nfs/ega/EGAZ0/EGAF").param("id", "id").session(new MockHttpSession()))
                .andReturn().getResponse();
        assertThat(response.getStatus(), equalTo(OK.value()));
    }

    /**
     * Test
     * {@link FileController#getArchiveFileSize(String, HttpServletRequest, HttpServletResponse)}.
     * Verify the api call returns status is OK.
     * 
     * @throws Exception
     */
    @Test
    public void testGetArchiveFile() throws Exception {

        final ArchiveSource archiveSource = new ArchiveSource();
        archiveSource.setEncryptionFormat("AES");

        commonMockMethod();
        when(archiveService.getArchiveFile(any(String.class), any(HttpServletResponse.class)))
                .thenReturn(archiveSource);

        final MockHttpServletResponse response = mockMvc
                .perform(get("/file/archive/id").param("destinationFormat", "plain").session(new MockHttpSession()))
                .andReturn().getResponse();
        assertThat(response.getStatus(), equalTo(OK.value()));
    }

    /**
     * Test
     * {@link FileController#getArchiveFileSize(String, HttpServletRequest, HttpServletResponse)}.
     * Verify the api call returns status is OK.
     * 
     * @throws Exception
     */
    @Test
    public void testGetArchiveFileSize() throws Exception {

        final ArchiveSource archiveSource = new ArchiveSource();
        archiveSource.setEncryptionFormat("AES");

        commonMockMethod();
        when(archiveService.getArchiveFile(any(String.class), any(HttpServletResponse.class)))
                .thenReturn(archiveSource);

        final MockHttpServletResponse response = mockMvc
                .perform(get("/file/archive/id/size").session(new MockHttpSession())).andReturn().getResponse();
        assertThat(response.getStatus(), equalTo(OK.value()));
    }

    /**
     * Test
     * {@link FileController#getArchiveFileSize(String, HttpServletRequest, HttpServletResponse)}.
     * Verify the api call returns status is NOT_FOUND.
     * 
     * @throws Exception
     */
    @Test
    public void testGetArchiveFileSize_NullSource() throws Exception {

        commonMockMethod();
        when(archiveService.getArchiveFile(any(String.class), any(HttpServletResponse.class))).thenReturn(null);

        final MockHttpServletResponse response = mockMvc
                .perform(get("/file/archive/id/size").session(new MockHttpSession())).andReturn().getResponse();
        assertThat(response.getStatus(), equalTo(NOT_FOUND.value()));
    }

    /**
     * Common mock method.
     */
    private void commonMockMethod() {
        doNothing().when(resService).transfer(any(String.class), any(String.class), any(String.class),
                any(String.class), any(String.class), any(String.class), any(Long.class), any(Long.class), any(Long.class),
                any(String.class), any(String.class), any(HttpServletRequest.class), any(HttpServletResponse.class));
    }

}
