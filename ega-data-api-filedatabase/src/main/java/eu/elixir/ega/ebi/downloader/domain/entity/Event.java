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
package eu.elixir.ega.ebi.downloader.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.sql.Timestamp;

/**
 * @author asenf
 */
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "event")
public class Event implements Serializable {

    @Id
    @NotNull
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @NotNull
    @Size(max = 45)
    @Column(name = "client_ip", nullable = false, length = 45)
    private String clientIp;

    @NotNull
    @Size(max = 256)
    @Column(nullable = false, length = 256)
    private String event;

    @NotNull
    @Size(max = 256)
    @Column(name = "event_type", nullable = false, length = 256)
    private String eventType;

    @NotNull
    @Size(max = 256)
    @Column(name = "email", nullable = false, length = 256)
    private String email;

    @NotNull
    @Column(nullable = false)
    private Timestamp created;

    /*
     *
     */
    public String toString() {
        String line = "";

        line += "ID: " + eventId + "\n" +
                "Client IP: " + clientIp + "\n" +
                "Event: " + event + "\n" +
                "Event Type: " + eventType + "\n" +
                "Email: " + email + "\n" +
                "Created: " + created.toString();

        return line;
    }
}
