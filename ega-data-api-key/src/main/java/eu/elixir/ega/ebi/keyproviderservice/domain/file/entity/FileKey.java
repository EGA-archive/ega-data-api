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
package eu.elixir.ega.ebi.keyproviderservice.domain.file.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author amohan
 */
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "file_key")
public class FileKey implements Serializable {

    @Id
    @Size(max = 128)
    @Column(name = "file_id", insertable = false, updatable = false, length = 128)
    private String fileId;

    @Column(name = "encryption_key_id", insertable = false, updatable = false)
    private long encryptionKeyId;

    @Size(max = 256)
    @Column(name = "encryption_algorithm", insertable = false, updatable = false, length = 256)
    private String encryptionAlgorithm;

    @Override
    public String toString() {
        return "FileKey [fileId=" + fileId + ", encryptionKeyId=" + encryptionKeyId + ", encryptionAlgorithm="
                + encryptionAlgorithm + "]";
    }
    
}