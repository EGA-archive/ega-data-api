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
package eu.elixir.ega.ebi.downloader.rest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.elixir.ega.ebi.downloader.domain.entity.DownloadLog;
import eu.elixir.ega.ebi.downloader.domain.entity.Event;
import eu.elixir.ega.ebi.downloader.service.LogService;

/**
 * Test class for {@link LogController}.
 * 
 * @author anand
 */
@RunWith(SpringRunner.class)
@WebMvcTest(LogController.class)
@TestPropertySource(locations = "classpath:application-test.properties")
public class LogControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private LogService logService;

	/**
	 * Test {@link LogController#putEvent(Event)}. Verify the api call returns
	 * status is NO_CONTENT.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPutEvent() throws Exception {
		Event input = new Event();
		input.setEvent("event");
		input.setEventId(Long.MAX_VALUE);

		String inputJson = mapToJson(input);
		RequestBuilder requestBuilder = MockMvcRequestBuilders.post("/log/event").accept(APPLICATION_JSON)
				.content(inputJson).contentType(APPLICATION_JSON);

		when(logService.logEvent(any(Event.class))).thenReturn(input);
		final MockHttpServletResponse response = mockMvc.perform(requestBuilder).andReturn().getResponse();
		assertThat(response.getStatus(), equalTo(NO_CONTENT.value()));
	}

	/**
	 * Test {@link LogController#putDownload(DownloadLog)}. Verify the api call
	 * returns status is NO_CONTENT.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPutDownload() throws Exception {
		DownloadLog input = new DownloadLog();
		input.setEmail("email");

		String inputJson = mapToJson(input);
		RequestBuilder requestBuilder = MockMvcRequestBuilders.post("/log/download").accept(APPLICATION_JSON)
				.content(inputJson).contentType(APPLICATION_JSON);

		when(logService.logDownload(any(DownloadLog.class))).thenReturn(input);
		final MockHttpServletResponse response = mockMvc.perform(requestBuilder).andReturn().getResponse();
		assertThat(response.getStatus(), equalTo(NO_CONTENT.value()));
	}

	/**
	 * Maps an object into JSON string. Uses a Jackson ObjectMapper.
	 * 
	 * @param object
	 * @return {@link String}
	 * @throws JsonProcessingException
	 */
	private String mapToJson(Object object) throws JsonProcessingException {
		ObjectMapper objectMapper = new ObjectMapper();
		return objectMapper.writeValueAsString(object);
	}
}
