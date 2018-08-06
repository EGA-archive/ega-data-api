/*
 * Copyright 2016 ELIXIR EGA
 * Copyright 2016 Alexander Senf
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
package eu.elixir.ega.ebi.dataedge.rest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import eu.elixir.ega.ebi.dataedge.service.FileService;
import htsjdk.samtools.cram.ref.CRAMReferenceSource;

/**
 * Test class for {@link FileController}.
 * 
 * @author amohan
 */
@RunWith(SpringRunner.class)
@WebMvcTest(FileController.class)
@TestPropertySource(locations = "classpath:application-test.properties")
public final class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileService fileService;

    /**
     * Test
     * {@link FileController#getFile(String, String, String, String, long, long, String, HttpServletRequest, HttpServletResponse)}.
     * Verify the api call returns status is OK.
     * 
     * @throws Exception
     */
    @Test
    public void testGetFile() throws Exception {
        final MockHttpServletResponse response = mockMvc.perform(get("/files/1").session(new MockHttpSession()))
                .andReturn().getResponse();
        assertThat(response.getStatus(), equalTo(OK.value()));
    }

    /**
     * Test {@link FileController#getFileHead(String, String, HttpServletRequest, HttpServletResponse)}.
     * Verify the api call returns status is OK.
     * 
     * @throws Exception
     */
    @Test
    public void testGetFileHead() throws Exception {
        final MockHttpServletResponse response = mockMvc.perform(head("/files/1").session(new MockHttpSession()))
                .andReturn().getResponse();
        assertThat(response.getStatus(), equalTo(OK.value()));
    }

    /**
     * Test {@link FileController#getFileHeader(String, String, String)}. Verify the
     * api call returns status is OK.
     * 
     * @throws Exception
     */
    @Test
    public void testGetFileHeader() throws Exception {
        when(fileService.getFileHeader(any(Authentication.class), any(String.class), any(String.class),
                any(String.class), any(CRAMReferenceSource.class))).thenReturn(null);

        final MockHttpServletResponse response = mockMvc.perform(get("/files/1/header").session(new MockHttpSession()))
                .andReturn().getResponse();
        assertThat(response.getStatus(), equalTo(OK.value()));
    }

    /**
     * Test {@link FileController#getById_(HttpServletResponse)}. Verify the
     * api call returns status is OK.
     * 
     * @throws Exception
     */
    @Test
    public void testGetById_() throws Exception {
        final MockHttpServletResponse response = mockMvc
                .perform(options("/files/byid/type").session(new MockHttpSession())).andReturn().getResponse();
        assertThat(response.getStatus(), equalTo(OK.value()));
    }

    /**
     * Test {@link FileController#getById(String, String, String, String, long, long, java.util.List, java.util.List, java.util.List, Boolean, String, String, HttpServletRequest, HttpServletResponse)}. Verify the
     * api call returns status is UNAUTHORIZED.
     * 
     * @throws Exception
     */
    @Test
    public void testGetById() throws Exception {
        final MockHttpServletResponse response = mockMvc
                .perform(get("/files/byid/type").param("accession", "accession").session(new MockHttpSession()))
                .andReturn().getResponse();
        assertThat(response.getStatus(), equalTo(UNAUTHORIZED.value()));
    }

    /**
     * Test {@link FileController#getByVariantId_(HttpServletResponse)}. Verify the
     * api call returns status is UNAUTHORIZED.
     * 
     * @throws Exception
     */
    @Test
    public void testGetByVariantId_() throws Exception {
        final MockHttpServletResponse response = mockMvc
                .perform(options("/files/variant/byid/type").session(new MockHttpSession())).andReturn().getResponse();
        assertThat(response.getStatus(), equalTo(OK.value()));
    }

    /**
     * Test {@link FileController#getByVariantId(String, String, String, String, long, long, java.util.List, java.util.List, java.util.List, Boolean, String, String, HttpServletRequest, HttpServletResponse)}. Verify the
     * api call returns status is UNAUTHORIZED.
     * 
     * @throws Exception
     */
    @Test
    public void testGetByVariantId() throws Exception {
        final MockHttpServletResponse response = mockMvc
                .perform(get("/files/variant/byid/type").param("accession", "accession").session(new MockHttpSession()))
                .andReturn().getResponse();
        assertThat(response.getStatus(), equalTo(UNAUTHORIZED.value()));
    }

    /**
     * Test {@link FileController#getHeadById(String, String, HttpServletRequest, HttpServletResponse)}. Verify the
     * api call returns status is UNAUTHORIZED.
     * 
     * @throws Exception
     */
    @Test
    public void testGetHeadById() throws Exception {
        final MockHttpServletResponse response = mockMvc
                .perform(head("/files/byid/type").param("accession", "accession").session(new MockHttpSession()))
                .andReturn().getResponse();
        assertThat(response.getStatus(), equalTo(UNAUTHORIZED.value()));
    }

    /**
     * Common mock method.
     */
    @Before
    public void commonMockMethod() {
        doNothing().when(fileService).getFile(any(Authentication.class), any(String.class), any(String.class),
                any(String.class), any(String.class), any(Long.class), any(Long.class), any(HttpServletRequest.class),
                any(HttpServletResponse.class));
        doNothing().when(fileService).getFileHead(any(Authentication.class), any(String.class), any(String.class),
                any(HttpServletRequest.class), any(HttpServletResponse.class));

    }
}
