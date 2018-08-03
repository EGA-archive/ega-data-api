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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

/**
 * @author asenf
 */
@RestController
@RequestMapping("/file")
public class FileController {

    @Autowired
    private FileService fileService;

    @RequestMapping(value = "/{file_id}", method = GET)
    @ResponseBody
    public Iterable<File> get(@PathVariable String file_id) {
        return fileService.getFileByStableId(file_id);
    }

    @RequestMapping(value = "/{file_id}/datasets", method = GET)
    @ResponseBody
    public Iterable<FileDataset> getDatasets(@PathVariable String file_id) {
        return fileService.getFileDatasetByFileId(file_id);
    }

    @RequestMapping(value = "/{file_id}/index", method = GET)
    @ResponseBody
    public Iterable<FileIndexFile> getIndex(@PathVariable String file_id) {
        return fileService.getFileIndexByFileId(file_id);
    }
}
