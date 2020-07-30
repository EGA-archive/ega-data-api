/*
 * Copyright 2017 ELIXIR EGA
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

/**
 * Configures a connection to an external EGA instance.
 *
 * @param egaExternalUrl      URL to the external EGA instance to connect to
 * @param cramFastaReferenceA
 * @param cramFastaReferenceB
 *
 * @author asenf
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class MyExternalConfig {

    private String egaExternalUrl;
    private String cramFastaReferenceA;
    private String cramFastaReferenceB;

}
