/*
 * Copyright 2017 ELIXIR EGA
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
package eu.elixir.ega.ebi.reencryptionmvc.service.internal;

import java.util.Optional;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import eu.elixir.ega.ebi.reencryptionmvc.service.ArchiveAdapterService;
import lombok.extern.slf4j.Slf4j;
import uk.ac.ebi.ega.fire.ingestion.service.IFireServiceNew;
import uk.ac.ebi.ega.fire.models.FireResponse;

/**
 * @author asenf
 */
@EnableDiscoveryClient
@Slf4j
public class ArchiveAdapterServiceImpl implements ArchiveAdapterService {

    private static final String PATH_OBJECTS = "objects/blob/path/";
    private String fireURL;
    private IFireServiceNew fireService;
    
    public ArchiveAdapterServiceImpl(IFireServiceNew fireService, String fireURL) {
        this.fireService = fireService;
        this.fireURL = fireURL;
    }

    @Override
    @Cacheable(cacheNames = "path")
    public String[] getPath(String path, String sessionId) {
        log.info(sessionId + "path=" + path);

        try {
            String[] result = new String[2];
            result[0] = "";
            result[1] = "";

            // Sending Request; 4 re-try attempts
            int reTryCount = 4;
            Optional<FireResponse> fireResponse = Optional.empty();
            do {
                try {
                    fireResponse = fireService.findFile(path);

                    if (fireResponse.isPresent()) {
                        FireResponse fire = fireResponse.get();
                        result[0] = fireURL + PATH_OBJECTS + path; 
                        result[1] = String.valueOf(fire.getObjectSize());
                    }

                } catch (Throwable th) {
                    log.error(sessionId + "FIRE error: " + th.getMessage(), th);
                }
                if (!fireResponse.isPresent()) {
                    Thread.sleep(500);
                }
            } while (!fireResponse.isPresent() && --reTryCount > 0);

            return result;
        } catch (Exception e) {
            log.error(sessionId + e.getMessage() + " Path = " + path, e);
        }

        return null;
    }

}
