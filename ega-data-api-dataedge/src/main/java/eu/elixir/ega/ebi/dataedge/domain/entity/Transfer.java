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
package eu.elixir.ega.ebi.dataedge.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
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
@Table(name = "transfer_log")
public class Transfer implements Serializable {

    @Id
    @NotNull
    @Size(max = 36)
    @Column(name = "transfer_uuid", nullable = false, length = 36)
    private String uuid;

    @Column(name = "timestamp")
    private Timestamp timestamp;

    @Size(max = 32)
    @Column(name = "md5_1", length = 32)
    private String md5_1;

    @Size(max = 32)
    @Column(name = "md5_2", length = 32)
    private String md5_2;

    @Column(name = "read")
    private long read;

    @Column(name = "sent")
    private long sent;

    @Size(max = 128)
    @Column(name = "service", length = 128)
    private String service;

}
