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
package eu.elixir.ega.ebi.commons.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Data transfer object for inserting event entries in the file database.
 *
 * @param downloadLogId Database id of the log entry
 * @param clientIp IP of the client that triggered the event
 * @param event event description text
 * @param eventType event type
 * @param email email of the user that triggered the event
 * @param created timestamp of the event
 *
 * @author asenf
 */
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class EventEntry implements Serializable {

    private String eventId;
    private String clientIp;
    private String event;
    private String eventType;
    private String email;
    private Timestamp created;

    /**
     * @return The formatted event entry string.
     */
    @Override
    public String toString() {
        return "EventEntry [eventId=" + eventId +
                ", clientIp=" + clientIp +
                ", event=" + event +
                ", eventType=" + eventType +
                ", email=" + email +
                ", created=" + created + "]";
    }

}
