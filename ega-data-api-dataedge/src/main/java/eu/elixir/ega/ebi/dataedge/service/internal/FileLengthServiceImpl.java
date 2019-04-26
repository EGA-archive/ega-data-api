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

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import eu.elixir.ega.ebi.dataedge.service.FileLengthService;
import eu.elixir.ega.ebi.shared.dto.File;

/**
 * @author amohan
 */
@Service
@Primary
@Profile("!LocalEGA")
public class FileLengthServiceImpl implements FileLengthService {

    @Override
    public long getContentLength(File reqFile, String destinationFormat, long startCoordinate, long endCoordinate) {
        long length = 0;

        // EncryptionFormat
        // If destinationFormat is encrypted then we add extra 16 byte to store the IV.
        int prefix = 16;
        if (destinationFormat.equalsIgnoreCase("plain"))
            prefix = 0;

        // Range specified?
        // The AES encrypted data has a 16 byte random IV at the start, so to
        // get the correct (raw) file length we need to remove this 16 byte
        // prefix.
        if (startCoordinate > 0 || endCoordinate > 0) {
            length = endCoordinate - startCoordinate;
        } else {
            length = reqFile.getFileSize() - 16;
        }

        return (length + prefix);
    }

}
