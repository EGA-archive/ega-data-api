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
package eu.elixir.ega.ebi.dataedge.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;

/**
 * Request ticket used to request a file.
 *
 * @author asenf
 */
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class RequestTicket {

    private String email;
    private String downloadTicket;
    private String clientIp;
    private String fileId;
    private String encryptionKey;
    private String encryptionType;
    private String ticketStatus;
    private String label;
    private Timestamp created;
    private long startCoordinate;
    private long endCoordinate;

    /**
     * @return The formatted request ticket string.
     */
    public String toString() {
        return email + ":" + downloadTicket + ":" + clientIp + ":" + fileId +
               ":" + encryptionKey + ":" + encryptionType + ":" + ticketStatus +
               ":" + label + ":" + startCoordinate + ":" + endCoordinate;
    }

}
