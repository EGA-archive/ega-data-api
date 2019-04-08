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
package eu.elixir.ega.ebi.shared.service.internal;

import static eu.elixir.ega.ebi.shared.Constants.FILEDATABASE_SERVICE;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import eu.elixir.ega.ebi.shared.config.GeneralStreamingException;
import eu.elixir.ega.ebi.shared.config.NotFoundException;
import eu.elixir.ega.ebi.shared.config.PermissionDeniedException;
import eu.elixir.ega.ebi.shared.config.VerifyMessageNew;
import eu.elixir.ega.ebi.shared.dto.EventEntry;
import eu.elixir.ega.ebi.shared.dto.FileDataset;
import eu.elixir.ega.ebi.shared.service.AuthenticationService;
import eu.elixir.ega.ebi.shared.service.DownloaderLogService;
import eu.elixir.ega.ebi.shared.service.PermissionsService;

@Service
public class PermissionsServiceImpl implements PermissionsService {

  private AuthenticationService authenticationService;

  private DownloaderLogService downloaderLogService;

  @Autowired
  private RestTemplate restTemplate;

  @Autowired
  private HttpServletRequest request;

  @Autowired
  public PermissionsServiceImpl(AuthenticationService authenticationService,
      DownloaderLogService downloaderLogService) {
    this.authenticationService = authenticationService;
    this.downloaderLogService = downloaderLogService;
  }

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
    } else if (request != null) { // ELIXIR User Case: Obtain Permmissions from X-Permissions Header
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
        //}
        //
        //try {
        //    List<String> permissions_ = (new VerifyMessage(request.getHeader("X-Permissions"))).getPermissions();
        //    if (permissions_ != null && permissions_.size() > 0) {
        //        for (String ds : permissions_) {
        //            if (ds != null) {
        //                permissions.add(ds);
        //            }
        //        }
        //    }
        //} catch (Exception ex) {

        System.out.println("getReqFile Error 0: " + ex.toString());
        EventEntry eev = downloaderLogService
            .createEventEntry(ex.toString(),  "file");
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
