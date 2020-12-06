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

import eu.elixir.ega.ebi.dataedge.exception.EgaFileNotFoundException;
import eu.elixir.ega.ebi.dataedge.exception.FileNotAvailableException;
import eu.elixir.ega.ebi.dataedge.exception.RangesNotSatisfiableException;
import eu.elixir.ega.ebi.dataedge.exception.UnretrievableFileException;

import java.io.InputStream;

public interface NuFileService {

    /**
     * Retrieve the plain file size for a specific EGA FileId.
     *
     * @param fileId The ID of the file
     * @return The plain file size (i.e. the size when it is unencrypted)
     * @throws EgaFileNotFoundException   the given file ID could not be found
     * @throws UnretrievableFileException the file is encrypted in an unsupported format
     * @throws FileNotAvailableException  the file exists but is not available
     */
    long getPlainFileSize(String fileId)
            throws EgaFileNotFoundException,
            UnretrievableFileException,
            FileNotAvailableException;

    /**
     * Retrieve a specific byte range for a specific EGA FileId.
     * The range is [start, end] inclusive.
     * The byte range will be returned as plain (unencrypted) data.
     *
     * @param fileId The ID of the file
     * @param start  The position of the first byte to return
     * @param end    The position of the last byte to return
     * @return A stream for reading the unencrypted bytes
     * @throws EgaFileNotFoundException      the given file ID could not be found
     * @throws UnretrievableFileException    the file is encrypted in an unsupported format
     * @throws FileNotAvailableException     the file exists but is not available
     * @throws RangesNotSatisfiableException the specified byte range could not be satisfied
     *                                       (e.g. past the end of the file)
     */
    InputStream getSpecificByteRange(String fileId, long start, long end)
            throws EgaFileNotFoundException,
            UnretrievableFileException,
            FileNotAvailableException,
            RangesNotSatisfiableException;

}
