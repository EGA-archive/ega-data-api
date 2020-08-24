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
package eu.elixir.ega.ebi.dataedge.rest;


import eu.elixir.ega.ebi.commons.config.VerifyMessage;
import eu.elixir.ega.ebi.commons.exception.NotFoundException;
import eu.elixir.ega.ebi.commons.exception.PermissionDeniedException;
import eu.elixir.ega.ebi.commons.shared.config.VerifyMessageNew;
import eu.elixir.ega.ebi.commons.shared.dto.Dataset;
import eu.elixir.ega.ebi.commons.shared.dto.File;
import eu.elixir.ega.ebi.dataedge.service.FileMetaService;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Strings;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

/**
 * @author asenf
 */
@RestController
@EnableDiscoveryClient
@Slf4j
@RequestMapping("/metadata")
public class MetadataController {

    @Autowired
    private FileMetaService fileService;

    /**
     * Returns the list of datasets that are authorized for the requesting user.
     *
     * @param request An http request
     * @return List of datasets authorized for this user
     */
    @RequestMapping(value = "/datasets", method = GET)
    public @ResponseBody
    Iterable<String> list(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // EGA AAI: Permissions Provided by EGA AAI
        ArrayList<String> result = new ArrayList<>();
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        if (authorities != null && authorities.size() > 0) {
            Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
            while (iterator.hasNext()) {
                GrantedAuthority next = iterator.next();
                result.add(next.getAuthority());
            }
        } else { // ELIXIR User Case: Obtain Permissions from X-Permissions Header
            try {
                List<String> permissions = (new VerifyMessageNew(request.getHeader("X-Permissions"))).getPermissions();
                if (permissions != null && permissions.size() > 0) {
                    for (String ds : permissions) {
                        if (ds != null && ds.length() > 0) result.add(ds);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return result; // List of datasets authorized for this user
    }

    /**
     * Returns a list of files for the given dataset if the current user has
     * permission to view them.
     *
     * @param datasetId Stable ID of the target dataset.
     * @param request Request holding
     * @return The list of files for this dataset id, or an empty list if the
     *     user doesn't have permission to the dataset.
     */
    @RequestMapping(value = "/datasets/{datasetId}/files", method = GET)
    public @ResponseBody
    Iterable<File> getDatasetFiles(@PathVariable String datasetId,
                                   HttpServletRequest request) {
        String sessionId = Strings.isNullOrEmpty(request.getHeader("Session-Id")) ? ""
                : request.getHeader("Session-Id") + " ";
        
        
        fileService.getDataset(datasetId, sessionId);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Validate Dataset Access
        boolean permission = false;

        // EGA AAI: Permissions Provided by EGA AAI
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        if (authorities != null && authorities.size() > 0) {
            Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
            while (iterator.hasNext()) {
                GrantedAuthority next = iterator.next();
                if (datasetId.equalsIgnoreCase(next.getAuthority())) {
                    permission = true;
                    break;
                }
            }
        } else { // ELIXIR User Case: Obtain Permmissions from X-Permissions Header
            try {
                List<String> permissions = (new VerifyMessage(request.getHeader("X-Permissions"))).getPermissions();
                if (permissions != null && permissions.size() > 0) {
                    for (String ds : permissions) {
                        if (datasetId.equalsIgnoreCase(ds)) {
                            permission = true;
                            break;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        
        if(!permission) {
            String message = sessionId.concat("Forbidden dataset ").concat(datasetId);
            log.error(message);
            throw new PermissionDeniedException(message);
        }

        return fileService.getDatasetFiles(datasetId);
    }

    /**
     * Returns a file from the configured file service given a file stable ID.
     *
     * @param fileId Stable ID for the file to retrieve.
     * @return The requested file.
     */
    @RequestMapping(value = "/files/{fileId}", method = GET)
    @ResponseBody
    public File getFile(@PathVariable String fileId,  HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // I don't know the dataset ID yet - pass on auth object to implementation for access control
        String sessionId = Strings.isNullOrEmpty(request.getHeader("Session-Id")) ? ""
                : request.getHeader("Session-Id") + " ";
        return fileService.getFile(auth, fileId, sessionId);
    }

}
