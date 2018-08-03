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

import com.google.gson.Gson;
import eu.elixir.ega.ebi.downloader.domain.entity.File;
import eu.elixir.ega.ebi.downloader.domain.entity.FileDataset;
import eu.elixir.ega.ebi.downloader.domain.entity.FileIndexFile;
import eu.elixir.ega.ebi.downloader.dto.DownloaderFile;
import eu.elixir.ega.ebi.downloader.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.transaction.Transactional;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;

@Slf4j
@Profile("LocalEGA")
@Service
@Transactional
public class LocalEGAFileServiceImpl implements FileService {

    private String fileServiceURL;
    private RestTemplate restTemplate;

    @Override
    @Cacheable(cacheNames = "fileById")
    public Iterable<File> getFileByStableId(String fileIDs) {
        // TODO: bring that back after LocalEGA key server becomes able to register itself against Eureka
        // ResponseEntity<Resource> responseEntity =
        //        restTemplate.getForEntity(fileServiceURL + "/temp/file." + id, Resource.class);

        File file = null;
        try {
            HashMap response = new Gson().fromJson(IOUtils.toString(new URL(fileServiceURL + "/temp/file/" + fileIDs).openStream(), Charset.defaultCharset()), HashMap.class);
            String filePath = String.valueOf(response.get("filepath"));
            long fileSize = Math.round(Double.parseDouble(String.valueOf(response.get("filesize"))));
            String checksum = String.valueOf(response.get("checksum"));
            String checksumType = String.valueOf(response.get("algo"));
            file = new File(fileIDs, filePath, "{path}", "{displayName}", 
                            fileSize, checksum, checksumType, 
                            "{unencryptedMD5}", "{unencType}", "available", 
                            "{Crypt4GH Header}");
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return Collections.singleton(file);
    }

    @Override
    @Cacheable(cacheNames = "datasetByFile")
    public Iterable<FileDataset> getFileDatasetByFileId(String fileID) {
        return Collections.singleton(new FileDataset(fileID, "EGAD01"));
    }

    @Override
    @Cacheable(cacheNames = "datasetFiles")
    public Iterable<DownloaderFile> getDatasetFiles(String datasetId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    @Cacheable(cacheNames = "fileIndexFile")
    public Iterable<FileIndexFile> getFileIndexByFileId(String fileID) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Value("${localega.fileserver.url:http://localhost:8443}")
    public void setFileServiceURL(String fileServiceURL) {
        this.fileServiceURL = fileServiceURL;
    }

    @Autowired
    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

}
