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
package eu.elixir.ega.ebi.shared.service.internal;

import static eu.elixir.ega.ebi.dataedge.config.Constants.FILEDATABASE_SERVICE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.elixir.ega.ebi.shared.dto.DownloadEntry;
import eu.elixir.ega.ebi.shared.dto.EventEntry;
import eu.elixir.ega.ebi.shared.service.DownloaderLogService;

/**
 * @author asenf
 */
@Service
@EnableDiscoveryClient
public class RemoteDownloaderLogServiceImpl extends
    AbstractDownloaderLogService implements DownloaderLogService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Converts a download entry to JSON and sends a POST request to the file
     * database service to store the event.
     *
     * @param downloadEntry The download entry to log.
     */
    @Override
    @Async
    public void logDownload(DownloadEntry downloadEntry) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Jackson ObjectMapper to convert requestBody to JSON
        String json = null;
        try {
            json = objectMapper.writeValueAsString(downloadEntry);
        } catch (JsonProcessingException jsonProcessingException) {
            throw new RuntimeException(jsonProcessingException);
        }

        restTemplate.postForEntity(FILEDATABASE_SERVICE + "/log/download/", new HttpEntity<>(json, headers), String.class);
    }

    /**
     * Converts an event entry to JSON and sends a POST request to the file
     * database service to store the event.
     *
     * @param eventEntry The event to log.
     */
    @Override
    @Async
    public void logEvent(EventEntry eventEntry) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Jackson ObjectMapper to convert requestBody to JSON
        String json = null;
        try {
            json = objectMapper.writeValueAsString(eventEntry);
        } catch (JsonProcessingException jsonProcessingException) {
            throw new RuntimeException(jsonProcessingException);
        }

        restTemplate.postForEntity(FILEDATABASE_SERVICE + "/log/event/", new HttpEntity<>(json, headers), String.class);
    }

}
