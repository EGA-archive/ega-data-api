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
package eu.elixir.ega.ebi.htsget.rest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;

import eu.elixir.ega.ebi.htsget.rest.TicketController;
import eu.elixir.ega.ebi.htsget.service.TicketService;
import java.util.List;
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

/**
 * Test class for {@link TicketController}.
 * 
 * @author amohan
 */
@RunWith(SpringRunner.class)
@WebMvcTest(TicketController.class)
@TestPropertySource(locations = "classpath:application-test.properties")
public class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TicketService ticketService;

    /**
     * Test {@link TicketController#getTicket(HttpServletResponse)}. Verify the API
     * call returns status is OK.
     * 
     * @throws Exception
     */
    @Test
    public void testGetTicket() throws Exception {
        final MockHttpServletResponse response = mockMvc
                .perform(options("/tickets/files/fileId").session(new MockHttpSession())).andReturn().getResponse();
        assertThat(response.getStatus(), equalTo(OK.value()));
    }

    /**
     * Test
     * {@link TicketController#getTicket(String, String, Integer, String, String, String, String, List, List, List, HttpServletRequest, HttpServletResponse)}.
     * Verify the API call returns status is OK.
     * 
     * @throws Exception
     */
    @Test
    public void testGetTicket2() throws Exception {
        final MockHttpServletResponse response = mockMvc
                .perform(get("/tickets/files/fileId").session(new MockHttpSession())).andReturn().getResponse();
        assertThat(response.getStatus(), equalTo(OK.value()));
    }
    
    /**
     * Test
     * {@link TicketController#getVariant(HttpServletResponse)}.
     * Verify the API call returns status is OK.
     * 
     * @throws Exception
     */
    @Test
    public void getVariant() throws Exception {
        final MockHttpServletResponse response = mockMvc
                .perform(options("/tickets/variants/fileId").session(new MockHttpSession())).andReturn().getResponse();
        assertThat(response.getStatus(), equalTo(OK.value()));
    }

    /**
     * Test
     * {@link TicketController#getVariantTicket(String, String, Integer, String, String, String, String, List, List, List, HttpServletRequest, HttpServletResponse)}.
     * Verify the API call returns status is OK.
     * 
     * @throws Exception
     */
    @Test
    public void testGetVariantTicket() throws Exception {
        final MockHttpServletResponse response = mockMvc
                .perform(get("/tickets/variants/fileId").session(new MockHttpSession())).andReturn().getResponse();
        assertThat(response.getStatus(), equalTo(OK.value()));
    }
    
    /**
     * Common mock method.
     */
    @SuppressWarnings("unchecked")
    @Before
    public void commonMockMethod() {
        when(ticketService.getVariantTicket(any(String.class), any(String.class),
                any(Integer.class), any(String.class), any(String.class), any(String.class), any(String.class),
                any(List.class), any(List.class), any(List.class), any(HttpServletRequest.class),
                any(HttpServletResponse.class))).thenReturn(null);
    }

}
