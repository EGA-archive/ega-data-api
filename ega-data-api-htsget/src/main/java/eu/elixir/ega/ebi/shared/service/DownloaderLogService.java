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
package eu.elixir.ega.ebi.shared.service;

import eu.elixir.ega.ebi.dataedge.dto.DownloadEntry;
import eu.elixir.ega.ebi.dataedge.dto.EventEntry;

/**
 * @author asenf
 */
public interface DownloaderLogService {

    void logDownload(DownloadEntry downloadEntry);

    void logEvent(EventEntry eventEntry);

    EventEntry createEventEntry(String t, String ticket);

    DownloadEntry createDownloadEntry(boolean success, double speed, String fileId,
        String server,
        String encryptionType,
        long startCoordinate,
        long endCoordinate,
        long bytes);

}
