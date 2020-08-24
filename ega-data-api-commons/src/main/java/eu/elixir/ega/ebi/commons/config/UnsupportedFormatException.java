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
package eu.elixir.ega.ebi.commons.config;

public class UnsupportedFormatException extends HtsgetException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new unsupported format exception with the given message.
     *
     * @param code the detail message.
     */
    public UnsupportedFormatException(String code) {
        super("Unsupported Format : " + code);
    }

    @Override
    public int getStatusCode() {
        return 400;
    }

    @Override
    public String getHtsgetErrorCode() {
        return "UnsupportedFormat";
    }

}
