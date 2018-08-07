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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * @author asenf
 */
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Entity
public class File implements Serializable {

    @Id
    @Size(max = 128)
    @Column(name = "file_id", insertable = false, updatable = false, length = 128)
    private String fileId;

    @Size(max = 256)
    @Column(name = "file_name", insertable = false, updatable = false, length = 256)
    private String fileName;

    @Size(max = 256)
    @Column(name = "file_path", insertable = false, updatable = false, length = 256)
    private String filePath;

    @Size(max = 128)
    @Column(name = "display_file_name", insertable = false, updatable = false, length = 128)
    private String displayFileName;

    @Column(name = "file_size", insertable = false, updatable = false)
    private long fileSize;

    @Size(max = 128)
    @Column(insertable = false, updatable = false, length = 128)
    private String checksum;

    @Size(max = 12)
    @Column(name = "checksum_type", insertable = false, updatable = false, length = 12)
    private String checksumType;

    @Size(max = 128)
    @Column(name = "unencrypted_checksum", insertable = false, updatable = false, length = 128)
    private String unencryptedChecksum;

    @Size(max = 12)
    @Column(name = "unencrypted_checksum_type", insertable = false, updatable = false, length = 12)
    private String unencryptedChecksumType;

    @Size(max = 13)
    @Column(name = "file_status", insertable = false, updatable = false, length = 13)
    private String fileStatus;

    @Column(insertable = false, updatable = false)
    private String header;
    
    /*
     *
     */

    public String toString() {
        String line = "";

        line += "File ID: " + fileId + "\n" +
                "File Name: " + fileName + "\n" +
                "File Size: " + fileSize + "\n" +
                "Checksum: " + checksum + "\n" +
                "Checksum Type: " + checksumType + "\n" +
                "Status: " + fileStatus;

        return line;
    }
}
