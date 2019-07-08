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

import eu.elixir.ega.ebi.shared.dto.File;

/**
 * @author amohan
 */
public interface FileLengthService {

    /**
     * Calculates the content length of a file or part of a file.
     *
     * @param reqFile file whose content is requested
     * @param destinationFormat file format of reqFile
     * @param startCoordinate start coordinate of the requested file part.
     * @param endCoordinate end coordinate of the requested file part, or -1 if
     *     the entire file is used.
     * @return length of the requested content
     */
    long getContentLength(File reqFile, String destinationFormat, long startCoordinate, long endCoordinate);

}
