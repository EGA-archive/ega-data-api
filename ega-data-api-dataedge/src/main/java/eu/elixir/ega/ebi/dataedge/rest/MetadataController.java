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


import eu.elixir.ega.ebi.dataedge.config.VerifyMessage;
import eu.elixir.ega.ebi.dataedge.config.VerifyMessageNew;
import eu.elixir.ega.ebi.dataedge.dto.File;
import eu.elixir.ega.ebi.dataedge.service.FileMetaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

/**
 * @author asenf
 */
@RestController
@EnableDiscoveryClient
@RequestMapping("/metadata")
public class MetadataController {

    @Autowired
    private FileMetaService fileService;

    @RequestMapping(value = "/datasets", method = GET)
    public @ResponseBody
    Iterable<String> list(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, String[]> parameters = request.getParameterMap();

        // EGA AAI: Permissions Provided by EGA AAI
        ArrayList<String> result = new ArrayList<>();
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        if (authorities != null && authorities.size() > 0) {
            Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
            while (iterator.hasNext()) {
                GrantedAuthority next = iterator.next();
                result.add(next.getAuthority());
            }
        } else { // ELIXIR User Case: Obtain Permmissions from X-Permissions Header
            //String permissions = request.getHeader("X-Permissions");
            try {
                List<String> permissions = (new VerifyMessageNew(request.getHeader("X-Permissions"))).getPermissions();
                if (permissions != null && permissions.size() > 0) {
                    //StringTokenizer t = new StringTokenizer(permissions, ",");
                    //while (t!=null && t.hasMoreTokens()) {
                    for (String ds : permissions) {
                        //String ds = t.nextToken();
                        if (ds != null && ds.length() > 0) result.add(ds);
                    }
                }
            } catch (Exception ex) {
            }
        }
        return result; // List of datasets authorized for this user
    }

    @RequestMapping(value = "/datasets/{dataset_id}/files", method = GET)
    public @ResponseBody
    Iterable<File> getDatasetFiles(@PathVariable String dataset_id,
                                   HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        //Map<String, String[]> parameters = request.getParameterMap();

        // Validate Dataset Access
        boolean permission = false;

        // EGA AAI: Permissions Provided by EGA AAI
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        if (authorities != null && authorities.size() > 0) {
            Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
            while (iterator.hasNext()) {
                GrantedAuthority next = iterator.next();
                if (dataset_id.equalsIgnoreCase(next.getAuthority())) {
                    permission = true;
                    break;
                }
            }
        } else { // ELIXIR User Case: Obtain Permmissions from X-Permissions Header
            //String permissions = request.getHeader("X-Permissions");
            try {
                List<String> permissions = (new VerifyMessage(request.getHeader("X-Permissions"))).getPermissions();
                if (permissions != null && permissions.size() > 0) {
                    //StringTokenizer t = new StringTokenizer(permissions, ",");
                    //while (t!=null && t.hasMoreTokens()) {
                    for (String ds : permissions) {
                        //String ds = t.nextToken();
                        if (ds != null && dataset_id.equalsIgnoreCase(ds)) {
                            permission = true;
                            break;
                        }
                    }
                }
            } catch (Exception ex) {
            }
        }

        return permission ? (fileService.getDatasetFiles(dataset_id)) : (new ArrayList<>());
    }

    @RequestMapping(value = "/files/{file_id}", method = GET)
    @ResponseBody
    public File getFile(@PathVariable String file_id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // I don't know the dataset ID yet - pass on auth object to implementation for access control
        return fileService.getFile(auth, file_id);
    }

}
