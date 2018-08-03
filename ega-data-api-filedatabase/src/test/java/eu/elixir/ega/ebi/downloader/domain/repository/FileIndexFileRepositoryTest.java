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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import eu.elixir.ega.ebi.downloader.domain.entity.FileIndexFile;

/**
 * Test class for {@link FileIndexFileRepository}.
 * 
 * @author anand
 */
@RunWith(SpringRunner.class)
@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
public class FileIndexFileRepositoryTest {

	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private FileIndexFileRepository fileIndexFileRepository;
	private FileIndexFile fileIndexFile;
	private Iterable<FileIndexFile> fileDatasetOutput;

	/**
	 * Test {@link FileIndexFileRepository#findByFileId(String)}. Verify the
	 * {@link FileIndexFile} retrieved from db.
	 */
	@Test
	public void testFindByFileId() {
		givenIndexFile();
		whenFindByFileIdRequested();
		thenVerifyFileDataset();
	}

	private void givenIndexFile() {
		fileIndexFile = new FileIndexFile("f", "if");
		System.out.println(entityManager.persist(fileIndexFile));
	}

	private void whenFindByFileIdRequested() {
		fileDatasetOutput = fileIndexFileRepository.findByFileId(fileIndexFile.getFileId());
	}

	private void thenVerifyFileDataset() {
		assertThat(fileDatasetOutput.iterator().next(), equalTo(fileIndexFile));
	}
}
