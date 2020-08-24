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
package eu.elixir.ega.ebi.downloader.service.internal;

import eu.elixir.ega.ebi.downloader.domain.entity.Dataset;
import eu.elixir.ega.ebi.downloader.domain.entity.File;
import eu.elixir.ega.ebi.downloader.domain.entity.FileDataset;
import eu.elixir.ega.ebi.downloader.domain.entity.FileIndexFile;
import eu.elixir.ega.ebi.downloader.domain.repository.DatasetRepository;
import eu.elixir.ega.ebi.downloader.domain.repository.FileDatasetRepository;
import eu.elixir.ega.ebi.downloader.domain.repository.FileIndexFileRepository;
import eu.elixir.ega.ebi.downloader.domain.repository.FileRepository;
import eu.elixir.ega.ebi.downloader.dto.DownloaderFile;
import eu.elixir.ega.ebi.downloader.service.FileService;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Optional;

/**
 * @author asenf
 */
@Profile("!LocalEGA")
@Service
@Slf4j
public class FileServiceImpl implements FileService {

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private FileDatasetRepository fileDatasetRepository;

    @Autowired
    private FileIndexFileRepository fileIndexFileRepository;
    
    @Autowired
    private DatasetRepository datasetRepository;

    @Override
    @Cacheable(cacheNames = "fileById")
    public Iterable<File> getFileByStableId(String fileIds) {
        return fileRepository.findByFileId(fileIds);
    }

    @Override
    @Cacheable(cacheNames = "datasetByFile")
    public Iterable<FileDataset> getFileDatasetByFileId(String fileID) {
        ArrayList<FileDataset> result = new ArrayList<>();
        Iterable<String> findByFileId = fileDatasetRepository.findCustom(fileID);
        if (findByFileId != null) {
            for (String aFindByFileId : findByFileId) {
                FileDataset fd = new FileDataset(fileID, aFindByFileId);
                log.info(" (--) " + fd.getFileId() + ", " + fd.getDatasetId());
                result.add(fd);
            }
        }
        return result;
        //return fileDatasetRepository.findByFileId(fileID);
    }

    @Override
    @Cacheable(cacheNames = "datasetFiles")
    public Iterable<DownloaderFile> getDatasetFiles(String datasetId) {
        // Get File IDs
        Iterable<FileDataset> fileIds = fileDatasetRepository.findByDatasetId(datasetId);
        Iterator<FileDataset> iter = fileIds.iterator();
        ArrayList<DownloaderFile> result = new ArrayList<>();

        // Get Files
        while (iter.hasNext()) {
            FileDataset next = iter.next();
            Iterable<File> files = fileRepository.findByFileId(next.getFileId());
            Iterator<File> iterator = files.iterator();
            while (iterator.hasNext()) {
                File file = iterator.next();
                result.add(new DownloaderFile(file.getFileId(),
                        next.getDatasetId(),
                        file.getDisplayFileName(),
                        file.getDisplayFilePath(),
                        file.getFileName(),
                        file.getFileSize(),
                        file.getUnencryptedChecksum(),
                        file.getUnencryptedChecksumType(),
                        file.getFileStatus()));
            }
        }

        return result;
    }

    @Override
    @Cacheable(cacheNames = "fileIndexFile")
    public Iterable<FileIndexFile> getFileIndexByFileId(String fileId) {
        return fileIndexFileRepository.findByFileId(fileId);
    }

    @Override
    public Optional<Dataset> getDataset(String datasetId) {
        return datasetRepository.findByDatasetId(datasetId);
    }

}
