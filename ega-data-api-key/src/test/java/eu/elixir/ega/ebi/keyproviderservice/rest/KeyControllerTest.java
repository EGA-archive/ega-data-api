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
package eu.elixir.ega.ebi.keyproviderservice.rest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import eu.elixir.ega.ebi.keyproviderservice.service.KeyService;

/**
 * Test class for {@link KeyController}.
 * 
 * @author amohan
 */
@RunWith(SpringRunner.class)
@WebMvcTest(KeyController.class)
@TestPropertySource(locations = "classpath:application-test.properties")
public final class KeyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KeyService keyService;

    /**
     * Test {@link KeyController#getFileKey(String)}. Verify the api call returns
     * status is OK.
     * 
     * @throws Exception
     */
    @Test
    public void testGetFileKey() throws Exception {
        when(keyService.getFileKey("file_id")).thenReturn("filekey");
        final MockHttpServletResponse response = mockMvc.perform(get("/keys/filekeys/file_id").accept(APPLICATION_JSON))
                .andReturn().getResponse();
        assertThat(response.getStatus(), equalTo(OK.value()));
        assertThat(response.getContentAsString(), equalTo("filekey"));
    }

    /**
     * Test {@link KeyController#getPublicKeyFromPrivate(String)}. Verify the api
     * call returns status is OK.
     * 
     * @throws Exception
     */
    @Test
    public void testGetPublicKeyFromPrivate() throws Exception {
        when(keyService.getPublicKeyFromPrivate("keyId")).thenReturn(null);
        final MockHttpServletResponse response = mockMvc
                .perform(get("/keys/retrieve/keyId/public").accept(APPLICATION_JSON)).andReturn().getResponse();
        assertThat(response.getStatus(), equalTo(OK.value()));
    }

    /**
     * Test {@link KeyController#getPublicKey(String, String)}. Verify the api call
     * returns status is OK.
     * 
     * @throws Exception
     */
    @Test
    public void testGetPublicKey() throws Exception {
        when(keyService.getPublicKey("keyId", "keyType")).thenReturn(null);
        final MockHttpServletResponse response = mockMvc
                .perform(get("/keys/retrieve/keyId/public/keyType").accept(APPLICATION_JSON)).andReturn().getResponse();
        assertThat(response.getStatus(), equalTo(OK.value()));
    }

    /**
     * Test {@link KeyController#getPrivateKey(String)}. Verify the api call returns
     * status is OK.
     * 
     * @throws Exception
     */
    @Test
    public void testGetPrivateKey() throws Exception {
        when(keyService.getPrivateKey("keyId")).thenReturn(null);
        final MockHttpServletResponse response = mockMvc
                .perform(get("/keys/retrieve/keyId/private/object").accept(APPLICATION_JSON)).andReturn().getResponse();
        assertThat(response.getStatus(), equalTo(OK.value()));
    }

    /**
     * Test {@link KeyController#getPrivateKeyPath(String)}. Verify the api call
     * returns status is OK.
     * 
     * @throws Exception
     */
    @Test
    public void testGetPrivateKeyPath() throws Exception {
        when(keyService.getPrivateKeyPath("keyId")).thenReturn(null);
        final MockHttpServletResponse response = mockMvc
                .perform(get("/keys/retrieve/keyId/private/path").accept(APPLICATION_JSON)).andReturn().getResponse();
        assertThat(response.getStatus(), equalTo(OK.value()));
    }

    /**
     * Test {@link KeyController#getPrivateKeyString(String)}. Verify the api call
     * returns status is OK.
     * 
     * @throws Exception
     */
    @Test
    public void testGetPrivateKeyString() throws Exception {
        when(keyService.getPrivateKeyString("keyId")).thenReturn(null);
        final MockHttpServletResponse response = mockMvc
                .perform(get("/keys/retrieve/keyId/private/key").accept(APPLICATION_JSON)).andReturn().getResponse();
        assertThat(response.getStatus(), equalTo(OK.value()));
    }

    /**
     * Test {@link KeyController#getPublicKey(String)}. Verify the api call returns
     * status is OK.
     * 
     * @throws Exception
     */
    @Test
    public void testGetSetPublicKey() throws Exception {
        when(keyService.getKeyIDs("keyType")).thenReturn(null);
        final MockHttpServletResponse response = mockMvc
                .perform(get("/keys/retrieve/keyType/ids").accept(APPLICATION_JSON)).andReturn().getResponse();
        assertThat(response.getStatus(), equalTo(OK.value()));
    }

}
