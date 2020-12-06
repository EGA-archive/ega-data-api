/*
 *
 * Copyright 2020 EMBL - European Bioinformatics Institute
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
 *
 */
package eu.elixir.ega.ebi.dataedge.service;

import eu.elixir.ega.ebi.commons.shared.dto.File;
import eu.elixir.ega.ebi.dataedge.exception.EgaFileNotFoundException;
import eu.elixir.ega.ebi.dataedge.exception.FileNotAvailableException;

public interface FileDatabaseClientService {

    /**
     * Get metadata about a file from the file database
     *
     * @param egaFileId the ID of the file to get
     * @return Metadata about the file
     * @throws EgaFileNotFoundException  The file was not found in the database
     * @throws FileNotAvailableException The file was found but is not available
     */
    File getById(String egaFileId) throws EgaFileNotFoundException, FileNotAvailableException;
}
