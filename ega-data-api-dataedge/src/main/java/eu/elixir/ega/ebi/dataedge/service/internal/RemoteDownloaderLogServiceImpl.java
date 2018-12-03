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
package eu.elixir.ega.ebi.dataedge.service.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.elixir.ega.ebi.dataedge.dto.DownloadEntry;
import eu.elixir.ega.ebi.dataedge.dto.EventEntry;
import eu.elixir.ega.ebi.dataedge.service.DownloaderLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;

import static eu.elixir.ega.ebi.shared.Constants.FILEDATABASE_SERVICE;

//import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

/**
 * @author asenf
 */
@Service
@EnableDiscoveryClient
public class RemoteDownloaderLogServiceImpl extends
    AbstractDownloaderLogService implements DownloaderLogService {

    @Autowired
    private AsyncRestTemplate restTemplate;

    @Autowired
    private RestTemplate syncRestTemplate;

    @Override
    //@HystrixCommand
    public void logDownload(DownloadEntry downloadEntry) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Jackson ObjectMapper to convert requestBody to JSON
        String json = null;
        try {
            json = new ObjectMapper().writeValueAsString(downloadEntry);
        } catch (JsonProcessingException ignored) {
        }

        HttpEntity<String> entity = new HttpEntity<>(json, headers);
        //HttpEntity entity = new HttpEntity("parameters", headers);

        ListenableFuture<ResponseEntity<String>> futureEntity;
        futureEntity = restTemplate.postForEntity(FILEDATABASE_SERVICE + "/log/download/", entity, String.class);

        futureEntity
                .addCallback(new ListenableFutureCallback<ResponseEntity>() {
                    @Override
                    public void onSuccess(ResponseEntity result) {
                        System.out.println(result.getBody());
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        System.out.println("LOG FAILURE: " + t.toString());
                    }
                });

        // Old Synchronous Call
        //syncRestTemplate.postForObject(SERVICE_URL + "/log/download/", downloadEntry, Void.class);
        //restTemplate.ppostForEntity(SERVICE_URL + "/log/download/", downloadEntry, Void.class);

    }

    @Override
    //@HystrixCommand
    public void logEvent(EventEntry eventEntry) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Jackson ObjectMapper to convert requestBody to JSON
        String json = null;
        try {
            json = new ObjectMapper().writeValueAsString(eventEntry);
        } catch (JsonProcessingException ignored) {
        }

        HttpEntity<String> entity = new HttpEntity<>(json, headers);
        //HttpEntity entity = new HttpEntity("parameters", headers);

        ListenableFuture<ResponseEntity<String>> futureEntity;
        futureEntity = restTemplate.postForEntity(FILEDATABASE_SERVICE + "/log/event/", entity, String.class);

        futureEntity
                .addCallback(new ListenableFutureCallback<ResponseEntity>() {
                    @Override
                    public void onSuccess(ResponseEntity result) {
                        System.out.println(result.getBody());
                    }

                    @Override
                    public void onFailure(Throwable t) {
                    }
                });

        // Old Synchronous Call
        //restTemplate.postForObject(SERVICE_URL + "/log/event/", eventEntry, Void.class);
    }

}
