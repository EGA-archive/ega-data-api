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

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import eu.elixir.ega.ebi.reencryptionmvc.dto.MyFireConfig;
import eu.elixir.ega.ebi.reencryptionmvc.service.ArchiveAdapterService;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * @author asenf
 */
@Service
@EnableDiscoveryClient
@Slf4j
public class ArchiveAdapterServiceImpl implements ArchiveAdapterService {

    @Autowired
    private MyFireConfig myFireConfig;

    @Override
    @Cacheable(cacheNames = "path")
    @HystrixCommand
    public String[] getPath(String path) {
        log.info("path=" + path);
        if (path.equalsIgnoreCase("Virtual File")) return new String[]{"Virtual File"};

        try {
            String[] result = new String[4]; // [0] name [1] stable_id [2] size [3] rel path
            result[0] = "";
            result[1] = "";
            result[3] = path;

            // Sending Request; 4 re-try attempts
            int reTryCount = 4;
            int responseCode = 0;
            HttpURLConnection connection = null;
            do {
                try {
                    connection = (HttpURLConnection) (new URL(myFireConfig.getFireUrl())).openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("X-FIRE-Archive", myFireConfig.getFireArchive());
                    connection.setRequestProperty("X-FIRE-Key", myFireConfig.getFireKey());
                    connection.setRequestProperty("X-FIRE-FilePath", path);

                    // Reading Response - with Retries
                    responseCode = connection.getResponseCode();
                    log.info("Response Code " + responseCode);
                } catch (Throwable th) {
                    log.error("FIRE error: " + th.getMessage(), th);
                }
                if (responseCode != 200) {
                    connection = null;
                    Thread.sleep(500);
                }
            } while (responseCode != 200 && --reTryCount > 0);

            // if Response OK
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                ArrayList<String[]> paths = new ArrayList<>();

                String object_get = "",             // 1
                        object_head = "",            // 2
                        object_md5 = "",             // 3
                        object_length = "",          // 4
                        object_url_expire = "",      // 5
                        object_storage_class = "";   // 6
                while ((inputLine = in.readLine()) != null) {
                    if (inputLine.startsWith("OBJECT_GET"))
                        object_get = inputLine.substring(inputLine.indexOf("http://")).trim();
                    if (inputLine.startsWith("OBJECT_HEAD"))
                        object_head = inputLine.substring(inputLine.indexOf(" ") + 1).trim();
                    if (inputLine.startsWith("OBJECT_MD5"))
                        object_md5 = inputLine.substring(inputLine.indexOf(" ") + 1).trim();
                    if (inputLine.startsWith("OBJECT_LENGTH"))
                        object_length = inputLine.substring(inputLine.indexOf(" ") + 1).trim();
                    if (inputLine.startsWith("OBJECT_URL_EXPIRE"))
                        object_url_expire = inputLine.substring(inputLine.indexOf(" ") + 1).trim();
                    if (inputLine.startsWith("OBJECT_STORAGE_CLASS"))
                        object_storage_class = inputLine.substring(inputLine.indexOf(" ") + 1).trim();
                    if (inputLine.startsWith("END"))
                        paths.add(new String[]{object_get, object_length, object_storage_class});
                }
                in.close();

                if (paths.size() > 0) {
                    for (int i = 0; i < paths.size(); i++) {
                        String[] e = paths.get(i);
                        if (!e[0].toLowerCase().contains("/ota/")) { // filter out tape archive
                            result[0] = e[0];   // GET Url
                            result[1] = e[1];   // Length
                            result[2] = e[2];   // Storage CLass
                            break;              // Pick first non-tape entry
                        }
                    }
                }
            }

            return result;
        } catch (Exception e) {
            log.error(e.getMessage() + " Path = " + path, e);
        }

        return null;
    }

}
