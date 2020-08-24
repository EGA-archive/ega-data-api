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
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;

import eu.elixir.ega.ebi.commons.exception.NotFoundException;
import eu.elixir.ega.ebi.commons.exception.PermissionDeniedException;
import eu.elixir.ega.ebi.commons.shared.dto.Dataset;
import eu.elixir.ega.ebi.commons.shared.dto.File;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import eu.elixir.ega.ebi.dataedge.service.FileMetaService;

/**
 * Test class for {@link MetadataController}.
 * 
 * @author amohan
 */
@RunWith(SpringRunner.class)
@WebMvcTest(MetadataController.class)
@TestPropertySource(locations = "classpath:application-test.properties")
public class MetadataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileMetaService fileService;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Before
    public void setupMock() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        Collection authorities = new ArrayList<GrantedAuthority>();
        authorities.add(new SimpleGrantedAuthority("DATASET1"));
        authorities.add(new SimpleGrantedAuthority("DATASET2"));
        when(authentication.getAuthorities()).thenReturn(authorities);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(fileService.getDataset(anyString(), anyString())).thenReturn(new Dataset());
    }

    /**
     * Test {@link MetadataController#list(HttpServletRequest)}. Verify the API call
     * returns status is OK and dataset authorities should also be present in response.
     * 
     * @throws Exception
     */
    @Test
    public void testList() throws Exception {
        final MockHttpServletResponse response = mockMvc
                .perform(get("/metadata/datasets").session(new MockHttpSession())).andReturn().getResponse();
        assertThat(response.getStatus(), equalTo(OK.value()));
        assertTrue(response.getContentAsString().contains("DATASET1"));
        assertTrue(response.getContentAsString().contains("DATASET2"));
    }
    
    /**
     * Test {@link MetadataController#getDatasetFiles(String, HttpServletRequest)}. Verify the API call
     * returns status is OK and checking fileId.
     * 
     * @throws Exception
     */
    @Test
    public void testGetDatasetFiles() throws Exception {
        final File f1 = new File();
        f1.setFileId("fileId");
        when(fileService.getDatasetFiles("DATASET1")).thenReturn(Arrays.asList(f1));
        
        final MockHttpServletResponse response = mockMvc
                .perform(get("/metadata/datasets/DATASET1/files").session(new MockHttpSession())).andReturn().getResponse();
        assertThat(response.getStatus(), equalTo(OK.value()));
        assertTrue(response.getContentAsString().contains(f1.getFileId()));
    }
    
    @Test
    public void getDatasetFiles_WhenDatasetDoesNotExistInUserAuthorisedDatasets_ThenThrowsPermissionDeniedException()
            throws Exception {
        mockMvc.perform(get("/metadata/datasets/DATASET3/files").session(new MockHttpSession()))
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof PermissionDeniedException));
    }
    
    /**
     * Test {@link MetadataController#getFile(String)}. Verify the API call
     * returns status is OK and checking fileId.
     * 
     * @throws Exception
     */
    @Test
    public void testGetFile() throws Exception {
        final File f1 = new File();
        f1.setFileId("fileId");
        when(fileService.getFile(any(Authentication.class), any(String.class), any(String.class))).thenReturn(f1);
        
        final MockHttpServletResponse response = mockMvc
                .perform(get("/metadata/files/fileId").session(new MockHttpSession())).andReturn().getResponse();
        assertThat(response.getStatus(), equalTo(OK.value()));
        assertTrue(response.getContentAsString().contains(f1.getFileId()));
    }

}
