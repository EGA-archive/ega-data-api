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
package eu.elixir.ega.ebi.htsget.config;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author amohan
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class IndexNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Exception thrown when an index file is not found.
     *
     * @param error Error description.
     * @param id file ID of the index file.
     */
    public IndexNotFoundException(String error, String id) {
        super(error + ": " + id);
    }

}
