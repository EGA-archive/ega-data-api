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
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import eu.elixir.ega.ebi.downloader.domain.entity.File;
import eu.elixir.ega.ebi.downloader.domain.entity.FileDataset;
import eu.elixir.ega.ebi.downloader.domain.entity.FileIndexFile;
import eu.elixir.ega.ebi.downloader.service.FileService;

/**
 * Test class for {@link FileController}.
 * 
 * @author anand
 */
@RunWith(SpringRunner.class)
@WebMvcTest(FileController.class)
@TestPropertySource(locations = "classpath:application-test.properties")
public class FileControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private FileService fileService;

	/**
	 * Test {@link FileController#get(String)}. Verify the api call returns
	 * status is OK.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGet() throws Exception {
		final List<File> files = new ArrayList<>();
		final File file = new File();
		file.setFileId("fileId");

		when(fileService.getFileByStableId(any(String.class))).thenReturn(files);
		final MockHttpServletResponse response = mockMvc.perform(get("/file/file_id").accept(APPLICATION_JSON))
				.andReturn().getResponse();
		assertThat(response.getStatus(), equalTo(OK.value()));
	}

	/**
	 * Test {@link FileController#getDatasets(String)}. Verify the api call
	 * returns status is OK.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetDatasets() throws Exception {
		final List<FileDataset> fileDatasets = new ArrayList<>();
		final FileDataset fileDataset = new FileDataset();
		fileDataset.setFileId("fileId");

		when(fileService.getFileDatasetByFileId(any(String.class))).thenReturn(fileDatasets);
		final MockHttpServletResponse response = mockMvc
				.perform(get("/file/file_id/datasets").accept(APPLICATION_JSON)).andReturn().getResponse();
		assertThat(response.getStatus(), equalTo(OK.value()));
	}

	/**
	 * Test {@link FileController#getIndex(String)}. Verify the api call returns
	 * status is OK.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetIndex() throws Exception {
		final List<FileIndexFile> fileIndexFiles = new ArrayList<>();
		final FileIndexFile fileIndexFile = new FileIndexFile();
		fileIndexFile.setFileId("fileId");

		when(fileService.getFileIndexByFileId(any(String.class))).thenReturn(fileIndexFiles);
		final MockHttpServletResponse response = mockMvc.perform(get("/file/file_id/index").accept(APPLICATION_JSON))
				.andReturn().getResponse();
		assertThat(response.getStatus(), equalTo(OK.value()));
	}
}
