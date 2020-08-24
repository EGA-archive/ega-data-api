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
package eu.elixir.ega.ebi.commons.shared.service.internal;


import eu.elixir.ega.ebi.commons.exception.GeneralStreamingException;
import eu.elixir.ega.ebi.commons.exception.NotFoundException;
import eu.elixir.ega.ebi.commons.exception.PermissionDeniedException;
import eu.elixir.ega.ebi.commons.shared.config.VerifyMessageNew;
import eu.elixir.ega.ebi.commons.shared.dto.EventEntry;
import eu.elixir.ega.ebi.commons.shared.dto.FileDataset;
import eu.elixir.ega.ebi.commons.shared.service.AuthenticationService;
import eu.elixir.ega.ebi.commons.shared.service.DownloaderLogService;
import eu.elixir.ega.ebi.commons.shared.service.PermissionsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static eu.elixir.ega.ebi.commons.config.Constants.FILEDATABASE_SERVICE;


@Service
@Slf4j
public class PermissionsServiceImpl implements PermissionsService {

    private AuthenticationService authenticationService;

    private DownloaderLogService downloaderLogService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private HttpServletRequest request;

    /**
     * Initializes the permission service with an authentication service and a
     * download log service.
     *
     * @param authenticationService Authentication service to use for permissions
     * @param downloaderLogService Downloader log service to use for logging
     */
    @Autowired
    public PermissionsServiceImpl(AuthenticationService authenticationService,
                                  DownloaderLogService downloaderLogService) {
        this.authenticationService = authenticationService;
        this.downloaderLogService = downloaderLogService;
    }

    /**
     * Returns the dataset that gives permissions for a provided stable ID.
     *
     * @param stableId The stable ID to get permissions for.
     *
     * @return The ID of the dataset that gives permissions for the stable ID.
     */
    @Override
    public String getFilePermissionsEntity(String stableId) {

        //Obtains DatasetId and FileId mapping from AAI Token Introspection result
        Map<String, List<String>> datasetFileMapping = authenticationService.getDatasetFileMapping();
        if (datasetFileMapping != null) {
            String datasetId = datasetFileMapping.entrySet().parallelStream()
                    .filter(e -> e.getValue().contains(stableId)).map(Map.Entry::getKey).findFirst()
                    .orElse(null);
            if (datasetId != null) {
                return datasetId;
            }
        }

        // Obtain all Authorised Datasets (Provided by EGA AAI)
        HashSet<String> permissions = new HashSet<>();
        Collection<? extends GrantedAuthority> authorities = authenticationService.getAuthorities();
        if (authorities != null && authorities.size() > 0) {
            Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
            while (iterator.hasNext()) {
                GrantedAuthority next = iterator.next();
                permissions.add(next.getAuthority());
            }
        } else if (request != null) { // ELIXIR User Case: Obtain Permissions from X-Permissions Header
            //String permissions = request.getHeader("X-Permissions");
            try {
                List<String> permissions_ = (new VerifyMessageNew(request.getHeader("X-Permissions")))
                        .getPermissions();
                if (permissions_ != null && permissions_.size() > 0) {
                    //StringTokenizer t = new StringTokenizer(permissions, ",");
                    //while (t!=null && t.hasMoreTokens()) {
                    for (String ds : permissions_) {
                        //String ds = t.nextToken();
                        if (ds != null && ds.length() > 0) {
                            permissions.add(ds);
                        }
                    }
                }
            } catch (Exception ex) {

                log.error("getReqFile Error 0: " + ex.toString());
                EventEntry eev = downloaderLogService
                        .createEventEntry(ex.toString(), "file");
                downloaderLogService.logEvent(eev);

                throw new GeneralStreamingException(ex.toString(), 0);
            }
        }

        ResponseEntity<FileDataset[]> forEntityDataset = restTemplate
                .getForEntity(FILEDATABASE_SERVICE + "/file/{fileId}/datasets", FileDataset[].class,
                        stableId);
        FileDataset[] bodyDataset = forEntityDataset.getBody();
        if (bodyDataset != null) {
            for (FileDataset f : bodyDataset) {
                String datasetId = f.getDatasetId();
                if (permissions.contains(datasetId)) {
                    return datasetId;
                }
            }
        } else { // 404 File Not Found
            throw new NotFoundException(stableId, "4");
        }
        throw new PermissionDeniedException(HttpStatus.UNAUTHORIZED.toString());
    }
}
