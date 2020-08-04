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
package eu.elixir.ega.ebi.commons.shared.service;

import eu.elixir.ega.ebi.commons.shared.dto.DownloadEntry;
import eu.elixir.ega.ebi.commons.shared.dto.EventEntry;

/**
 * @author asenf
 */
public interface DownloaderLogService {

    /**
     * Writes a {@link DownloadEntry} string to the log.
     *
     * @param downloadEntry a DownloadEntry to write to the log
     */
    void logDownload(DownloadEntry downloadEntry);

    /**
     * Writes an {@link EventEntry} string to the log.
     *
     * @param eventEntry an EventEntry to write to the log
     */
    void logEvent(EventEntry eventEntry);

    /**
     * Creates a new {@link EventEntry} with the description given in {@code t},
     * client IP set from the current request, email set to the current login email,
     * type set to 'Error' and created set to the current timestamp.
     *
     * @param t Event description text.
     * @param ticket unused.
     * @return An 'Error' type {@link EventEntry}.
     */
    EventEntry createEventEntry(String t, String ticket);

    /**
     * Creates a new {@link DownloadEntry} with the supplied values, email set to
     * the current login user, created set to the current timestamp and token
     * source set to 'EGA'.
     *
     * @param success {@code true} or {@code false} wheather the download succeeded.
     * @param speed Download speed for the download.
     * @param fileId Stable ID of the downloaded file.
     * @param server Server where the data was downloaded from.
     * @param encryptionType The encryption type for the downloaded data.
     * @param startCoordinate Start coordinate of the downloaded data sequence.
     * @param endCoordinate End coordinate of the downloaded data sequence.
     * @param bytes Number of downloaded bytes.
     * @return A {@link DownloadEntry} with the supplied values.
     */
    DownloadEntry createDownloadEntry(boolean success, double speed, String fileId,
                                      String server,
                                      String encryptionType,
                                      long startCoordinate,
                                      long endCoordinate,
                                      long bytes);

}
