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
package eu.elixir.ega.ebi.downloader.rest;

import eu.elixir.ega.ebi.downloader.domain.entity.File;
import eu.elixir.ega.ebi.downloader.domain.entity.FileDataset;
import eu.elixir.ega.ebi.downloader.domain.entity.FileIndexFile;
import eu.elixir.ega.ebi.downloader.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

/**
 * @author asenf
 */
@RestController
@RequestMapping("/file")
public class FileController {

    @Autowired
    private FileService fileService;

    @RequestMapping(value = "/{fileId}", method = GET)
    @ResponseBody
    public ResponseEntity<?> get(@PathVariable String fileId) {
        Iterable<File> file = fileService.getFileByStableId(fileId);
        while (!file.iterator().hasNext()) {
            return ResponseEntity.status(NOT_FOUND).build();
        }
        return ResponseEntity.ok(file);
    }

    @RequestMapping(value = "/{fileId}/datasets", method = GET)
    @ResponseBody
    public Iterable<FileDataset> getDatasets(@PathVariable String fileId) {
        return fileService.getFileDatasetByFileId(fileId);
    }

    @RequestMapping(value = "/{fileId}/index", method = GET)
    @ResponseBody
    public Iterable<FileIndexFile> getIndex(@PathVariable String fileId) {
        return fileService.getFileIndexByFileId(fileId);
    }

}
