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
package eu.elixir.ega.ebi.downloader.service.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;

import eu.elixir.ega.ebi.downloader.domain.entity.File;
import eu.elixir.ega.ebi.downloader.domain.entity.FileDataset;
import eu.elixir.ega.ebi.downloader.domain.entity.FileIndexFile;
import eu.elixir.ega.ebi.downloader.domain.repository.FileDatasetRepository;
import eu.elixir.ega.ebi.downloader.domain.repository.FileIndexFileRepository;
import eu.elixir.ega.ebi.downloader.domain.repository.FileRepository;
import java.util.Iterator;

/**
 * Test class for {@link FileServiceImpl}.
 * 
 * @author anand
 */
@RunWith(SpringRunner.class)
public class FileServiceImplTest {

	@Autowired
	private FileServiceImpl fileServiceImpl;

	@MockBean
	private FileRepository fileRepository;

	@MockBean
	private FileDatasetRepository fileDatasetRepository;

	@MockBean
	private FileIndexFileRepository fileIndexFileRepository;

	@Before
	public void setup() {
		when(fileRepository.findByFileId(any(String.class))).thenReturn(getFile());
		when(fileDatasetRepository.findByFileId(any(String.class))).thenReturn(getFileDataset());
		when(fileDatasetRepository.findByDatasetId(any(String.class))).thenReturn(getFileDataset());
		when(fileIndexFileRepository.findByFileId(any(String.class))).thenReturn(getFileIndexFile());
	}

	/**
	 * Test class for {@link FileServiceImpl#getFileByStableId(String)}. Verify
	 * fileId retrieved from db mock call.
	 */
	@Test
	public void testGetFileByStableId() {
		assertThat(fileServiceImpl.getFileByStableId("fileId").iterator().next().getFileId(), equalTo(getFile()
				.iterator().next().getFileId()));
	}

	/**
	 * Test class for {@link FileServiceImpl#getFileDatasetByFileId(String)}.
	 * Verify fileId retrieved from db mock call.
	 */
	@Test
	public void testGetFileDatasetByFileId() {
            // TODO: Update Test for FindCustom method in FileDatasetRepository called from FileServiceImpl
	//	assertThat(fileServiceImpl.getFileDatasetByFileId("fileId").iterator().next().getFileId(),
	//			equalTo(getFileDataset().iterator().next().getFileId()));
	}

	/**
	 * Test class for {@link FileServiceImpl#getDatasetFiles(String)}. Verify
	 * fileId retrieved from db mock call.
	 */
	@Test
	public void testGetDatasetFiles() {
		assertThat(fileServiceImpl.getDatasetFiles("datasetId").iterator().next().getFileId(), equalTo(getFileDataset()
				.iterator().next().getFileId()));
	}

	/**
	 * Test class for {@link FileServiceImpl#getFileIndexByFileId(String)}.
	 * Verify fileId retrieved from db mock call.
	 */
	@Test
	public void testGetFileIndexByFileId() {
		assertThat(fileServiceImpl.getFileIndexByFileId("fileId").iterator().next().getFileId(),
				equalTo(getFileIndexFile().iterator().next().getFileId()));
	}

	private Iterable<File> getFile() {
		final File file = new File();
		file.setFileId("fileId");
		return Arrays.asList(file);
	}

	private Iterable<FileDataset> getFileDataset() {
		return Arrays.asList(new FileDataset("fileId", "datasetId"));
	}

	private Iterable<FileIndexFile> getFileIndexFile() {
		return Arrays.asList(new FileIndexFile("fileId", "indexFileId"));
	}

	@TestConfiguration
	static class FileServiceImplTestContextConfiguration {
		@Bean
		public FileServiceImpl fileService() {
			return new FileServiceImpl();
		}
	}
}
