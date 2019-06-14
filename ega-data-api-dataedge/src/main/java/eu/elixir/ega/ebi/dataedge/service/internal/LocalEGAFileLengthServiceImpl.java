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
package eu.elixir.ega.ebi.dataedge.service.internal;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import eu.elixir.ega.ebi.dataedge.service.FileLengthService;
import eu.elixir.ega.ebi.shared.dto.File;

/**
 * @author amohan
 */
@Service
@Profile("LocalEGA")
public class LocalEGAFileLengthServiceImpl  implements FileLengthService {

    /**
     * Calculates the content length of a file or part of a file.
     *
     * @param reqFile           file whose content is requested
     * @param destinationFormat (unused) file format of reqFile
     * @param startCoordinate   start coordinate of the requested file part.
     * @param endCoordinate     end coordinate of the requested file part, or -1 if
     *                          the entire file is used.
     * @return length of the requested content
     */
    @Override
    public long getContentLength(File reqFile, String destinationFormat, long startCoordinate, long endCoordinate) {
        long length = 0;

        if (startCoordinate > 0 || endCoordinate > 0) {
            length = endCoordinate - startCoordinate;
        } else {
            // Removing 32 bytes from the actual file size because when the file is stored in S3,
            // the sha256sum is added at the beginning so the length would be -32
            length = reqFile.getFileSize() - 32;
        }

        return length;
    }

}
