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
package eu.elixir.ega.ebi.dataedge.service;

import eu.elixir.ega.ebi.commons.shared.dto.Dataset;
import eu.elixir.ega.ebi.commons.shared.dto.File;

import java.util.List;

import org.springframework.security.core.Authentication;

/**
 * @author asenf
 */
public interface FileMetaService {

    /**
     * Returns a {@link File} descriptor for the requested file, if the correct
     * permissions are available in @{code auth}.
     *
     * @param auth   An authentication token for the file.
     * @param fileId The stable ID of the file to request.
     * @param sessionId 
     * @return The requested file descriptor, otherwise an empty {@link File}
     *         object.
     */
    File getFile(Authentication auth, String fileId, String sessionId);

    /**
     * Returns the list of files for a given dataset from the file database service.
     *
     * @param datasetId Stable ID of the dataset to request
     * @return List of files for the given dataset
     */
    Iterable<File> getDatasetFiles(String datasetId);

    Dataset getDataset(String datasetId, String sessionId);
}
