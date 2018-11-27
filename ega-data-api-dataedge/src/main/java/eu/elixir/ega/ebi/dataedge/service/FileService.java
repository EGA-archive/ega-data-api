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
package eu.elixir.ega.ebi.dataedge.service;

import htsjdk.samtools.cram.ref.CRAMReferenceSource;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @author asenf
 */
public interface FileService {

    void getFile(String fileId,
                 String destinationFormat,
                 String destinationKey,
                 String destinationIV,
                 long startCoordinate,
                 long endCoordinate,
                 HttpServletRequest request,
                 HttpServletResponse response);

    void getFileHead(String fileId,
                     String destinationFormat,
                     HttpServletRequest request,
                     HttpServletResponse response);

    Object getFileHeader(String fileId,
                         String destinationFormat,
                         String destinationKey,
                         CRAMReferenceSource x);

    void getById(String fileId,
                 String accession,
                 String format,
                 String reference,
                 long start,
                 long end,
                 List<String> fields,
                 List<String> tags,
                 List<String> notags,
                 boolean header,
                 String destinationFormat,
                 String destinationKey,
                 HttpServletRequest request,
                 HttpServletResponse response);

    void getVCFById(String fileId,
                    String accession,
                    String format,
                    String reference,
                    long start,
                    long end,
                    List<String> fields,
                    List<String> tags,
                    List<String> notags,
                    boolean header,
                    String destinationFormat,
                    String destinationKey,
                    HttpServletRequest request,
                    HttpServletResponse response);

    ResponseEntity getHeadById(String fileId,
                               String accession,
                               HttpServletRequest request,
                               HttpServletResponse response);

}
