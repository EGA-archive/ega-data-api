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
package eu.elixir.ega.ebi.downloader.domain.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import eu.elixir.ega.ebi.downloader.domain.entity.FileDataset;

/**
 * Test class for {@link FileDatasetRepository}.
 * 
 * @author anand
 */
@RunWith(SpringRunner.class)
@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
public class FileDatasetRepositoryTest {
	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private FileDatasetRepository datasetRepository;
	private FileDataset fileDataset;
	private Iterable<FileDataset> fileDatasetOutput;

	/**
	 * Test {@link FileDatasetRepository#findByFileId(String)}. Verify the
	 * {@link FileDataset} retrieved from db.
	 */
	@Test
	public void testFindByFileId() {
		givenDataset();
		whenFindByFileIdRequested();
		thenVerifyFileDataset();
	}

	@Test
	@Ignore
	public void testFindByDatasetId() {
		givenDataset();
		whenFindByDatasetIdRequested();
		thenVerifyFileDataset();
	}

	private void givenDataset() {
		fileDataset = new FileDataset("f", "d");
		entityManager.persist(fileDataset);
	}

	private void whenFindByFileIdRequested() {
		fileDatasetOutput = datasetRepository.findByFileId(fileDataset.getFileId());
	}

	private void whenFindByDatasetIdRequested() {
		fileDatasetOutput = datasetRepository.findByDatasetId(fileDataset.getDatasetId());
	}

	private void thenVerifyFileDataset() {
		assertThat(fileDatasetOutput.iterator().next(), equalTo(fileDataset));
	}

}
