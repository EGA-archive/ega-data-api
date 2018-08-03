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
@Table(name = "download_log")
public class DownloadLog implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @NotNull
    @Column(name = "download_log_id", nullable = false)
    private Long downloadLogId;

    @NotNull
    @Size(max = 45)
    @Column(name = "client_ip", nullable = false, length = 45)
    private String clientIp;

    @NotNull
    @Size(max = 45)
    @Column(nullable = false, length = 45)
    private String api;

    @NotNull
    @Size(max = 256)
    @Column(name = "email", nullable = false, length = 256)
    private String email;

    @NotNull
    @Size(max = 15)
    @Column(name = "file_id", nullable = false, length = 15)
    private String fileId;

    @NotNull
    @Column(name = "download_speed", nullable = false)
    private double downloadSpeed;

    @NotNull
    @Size(max = 256)
    @Column(name = "download_status", nullable = false, length = 256)
    private String downloadStatus;

    @NotNull
    @Size(max = 256)
    @Column(name = "encryption_type", nullable = false, length = 256)
    private String encryptionType;

    @NotNull
    @Column(name = "start_coordinate", nullable = false)
    private Long startCoordinate;

    @NotNull
    @Column(name = "end_coordinate", nullable = false)
    private Long endCoordinate;

    @NotNull
    @Column(name = "bytes", nullable = false)
    private Long bytes;

    @NotNull
    @Column(nullable = false)
    private Timestamp created;

    @NotNull
    @Size(max = 255)
    @Column(name = "token_source", nullable = false, length = 255)
    private String tokenSource;

    /*
     *
     */
    public String toString() {
        String line = "";

        line += "ID: " + downloadLogId + "\n" +
                "Client IP: " + clientIp + "\n" +
                "Server: " + api + "\n" +
                "Email: " + email + "\n" +
                "File ID: " + fileId + "\n" +
                "Download Speed: " + downloadSpeed + "\n" +
                "Download Status: " + downloadStatus + "\n" +
                "Encryption Type: " + encryptionType + "\n" +
                "Start Coordinate: " + startCoordinate + "\n" +
                "End Coordinate: " + endCoordinate + "\n" +
                "Bytes: " + bytes + "\n" +
                "Created: " + created.toString() + "\n" +
                "Token Source: " + tokenSource;

        return line;
    }

}
