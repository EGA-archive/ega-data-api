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

    /**
     * Writes a requested file, or part of file from the FileService to the supplied
     * response stream.
     *
     * @param fileId            ELIXIR id of the requested file.
     * @param destinationFormat Requested destination format, either 'plain', 'aes',
     *                          or file extension.
     * @param destinationKey    Encryption key that the result file will be
     *                          encrypted with.
     * @param destinationIV     Initialization Vector for for destination file, used
     *                          when requesting a partial AES encrypted file.
     * @param startCoordinate   Start coordinate when requesting a partial file.
     * @param endCoordinate     End coordinate when requesting a partial file.
     * @param request           Unused.
     * @param response          Response stream for the returned data.
     */
    void getFile(String fileId,
                 String destinationFormat,
                 String destinationKey,
                 String destinationIV,
                 long startCoordinate,
                 long endCoordinate,
                 HttpServletRequest request,
                 HttpServletResponse response);

    /**
     * Returns the http header for a file identified by fileId. This mainly includes
     * the content length, but also a random UUID for statistics.
     *
     * @param fileId            ELIXIR id of the requested file
     * @param destinationFormat Requested destination format.
     * @param request           Unused.
     * @param response          Response stream for the returned data.
     */
    void getFileHead(String fileId,
                     String destinationFormat,
                     HttpServletRequest request,
                     HttpServletResponse response);

    /**
     * Returns the SAM file header for a file identified by fileId.
     *
     * @param fileId ELIXIR id of the requested file.
     * @param destinationFormat Requested destination format.
     * @param destinationKey Encryption key that the result file will be
     *     encrypted with.
     * @param x optional CRAM reference source to be used with the
     *     SamReaderFactory.
     * @return The SAM file header for the file.
     */
    Object getFileHeader(String fileId,
                         String destinationFormat,
                         String destinationKey,
                         CRAMReferenceSource x);

    /**
     * Writes a requested file (or part of file), selected by accession, from the
     * FileService to the supplied response stream.
     *
     * @param fileId            Should be set to 'file'.
     * @param accession         Local accession ID of the requested file.
     * @param format            Requested file format. Either 'bam' or 'cram' (case
     *                          insensitive).
     * @param reference         FASTA reference name, required for selecting a
     *                          region with start and end.
     * @param start             Start coordinate when requesting a partial file.
     * @param end               End coordinate when requesting a partial file.
     * @param fields            Data fields to include in the output file.
     * @param tags              Data tags to include in the output file.
     * @param notags            Data tags to exclude from the output file.
     * @param header            Unused.
     * @param destinationFormat Requested destination format.
     * @param destinationKey    Unused.
     * @param request           Unused.
     * @param response          Response stream for the returned data.
     */
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

    /**
     * Writes a requested file (or part of file), selected by accession, from the
     * FileService to the supplied response stream.
     *
     * @param fileId            Should be set to 'file'.
     * @param accession         Local accession ID of the requested file.
     * @param format            Unused.
     * @param reference         FASTA reference name, required for selecting a
     *                          region with start and end.
     * @param start             Start coordinate when requesting a partial file.
     * @param end               End coordinate when requesting a partial file.
     * @param fields            Data fields to include in the output file.
     * @param tags              Data tags to include in the output file.
     * @param notags            Data tags to exclude from the output file.
     * @param header            Unused.
     * @param destinationFormat Requested destination format.
     * @param destinationKey    Unused.
     * @param request           Unused.
     * @param response          Response stream for the returned data.
     */
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

    /**
     * Writes the content length of a selected file to the reponse parameter, and
     * returns OK or UNAUTHORIZED wheather the file exists and can be accessible.
     *
     * @param fileId    should be "file".
     * @param accession accession id of the requested file.
     * @param request   Unused.
     * @param response  reponse object which will be modified with the content
     *                  length of the requested file head.
     * @return httpStatus OK if the file info was accessible, and the reponse was
     *         modified, otherwise UNAUTHORIZED.
     */
    ResponseEntity getHeadById(String fileId,
                               String accession,
                               HttpServletRequest request,
                               HttpServletResponse response);

}
