/*
 *
 * Copyright 2020 EMBL - European Bioinformatics Institute
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
 *
 */
package eu.elixir.ega.ebi.reencryptionmvc.exception;

public class RangesNotSatisfiableException extends Exception {
    private final String fileId;
    private final long rangeStart;
    private final long rangeEnd;

    public RangesNotSatisfiableException(String fileId, long rangeStart, long rangeEnd) {
        this.fileId = fileId;
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
    }

    public String getFileId() {
        return fileId;
    }

    public long getRangeStart() {
        return rangeStart;
    }

    public long getRangeEnd() {
        return rangeEnd;
    }
}
