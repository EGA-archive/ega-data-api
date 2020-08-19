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

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import eu.elixir.ega.ebi.downloader.domain.entity.Dataset;
import eu.elixir.ega.ebi.downloader.dto.DownloaderFile;
import eu.elixir.ega.ebi.downloader.service.FileService;

/**
 * @author asenf
 */
@RestController
@RequestMapping("/datasets")
public class DatasetController {

    @Autowired
    private FileService fileService;

    @RequestMapping(value = "/{datasetId}", method = GET)
    @ResponseBody
    public ResponseEntity<?> getDataset(@PathVariable String datasetId) {
        Iterable<Dataset> dataset = fileService.getDataset(datasetId);
        if (!dataset.iterator().hasNext()) {
            return ResponseEntity.status(NOT_FOUND).build();
        }
        return ResponseEntity.ok(dataset);
    }

    @RequestMapping(value = "/{datasetId}/files", method = GET)
    @ResponseBody
    public Iterable<DownloaderFile> getDatasetFiles(@PathVariable String datasetId) {
        return fileService.getDatasetFiles(datasetId);
    }

}
