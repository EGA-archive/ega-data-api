package eu.elixir.ega.ebi.dataedge.service.internal;

import static eu.elixir.ega.ebi.shared.Constants.FILEDATABASE_SERVICE;
import static eu.elixir.ega.ebi.shared.Constants.RES_SERVICE;

import eu.elixir.ega.ebi.dataedge.config.GeneralStreamingException;
import eu.elixir.ega.ebi.dataedge.config.NotFoundException;
import eu.elixir.ega.ebi.dataedge.config.PermissionDeniedException;
import eu.elixir.ega.ebi.dataedge.config.VerifyMessageNew;
import eu.elixir.ega.ebi.dataedge.dto.EventEntry;
import eu.elixir.ega.ebi.dataedge.service.AuthenticationService;
import eu.elixir.ega.ebi.dataedge.service.DownloaderLogService;
import eu.elixir.ega.ebi.dataedge.service.PermissionsService;
import eu.elixir.ega.ebi.shared.dto.File;
import eu.elixir.ega.ebi.shared.dto.FileDataset;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.client.RestTemplate;

public class PermissionsServiceImpl implements PermissionsService {

  private AuthenticationService authenticationService;

  private DownloaderLogService downloaderLogService;

  private RestTemplate restTemplate;

  @Autowired
  public PermissionsServiceImpl(AuthenticationService authenticationService,
      DownloaderLogService downloaderLogService, RestTemplate restTemplate) {
    this.authenticationService = authenticationService;
    this.downloaderLogService = downloaderLogService;
    this.restTemplate = restTemplate;
  }

  @Override
  @Cacheable(cacheNames = "reqFile")
  public File getReqFile(String fileId, HttpServletRequest request) {

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
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null) {
          ipAddress = request.getRemoteAddr();
        }
        String user_email = authenticationService.getName(); // For Logging

        System.out.println("getReqFile Error 0: " + ex.toString());
        EventEntry eev = downloaderLogService.getEventEntry(ex.toString(), ipAddress, "file", user_email);
        downloaderLogService.logEvent(eev);

        throw new GeneralStreamingException(ex.toString(), 0);
      }
    }

    ResponseEntity<FileDataset[]> forEntityDataset = restTemplate
        .getForEntity(FILEDATABASE_SERVICE + "/file/{fileId}/datasets", FileDataset[].class,
            fileId);
    FileDataset[] bodyDataset = forEntityDataset.getBody();

    File reqFile = null;
    ResponseEntity<File[]> forEntity = restTemplate
        .getForEntity(FILEDATABASE_SERVICE + "/file/{fileId}", File[].class, fileId);
    File[] body = forEntity.getBody();
    if (body != null && bodyDataset != null) {
      for (FileDataset f : bodyDataset) {
        String datasetId = f.getDatasetId();
        if (permissions.contains(datasetId) && body.length >= 1) {
          reqFile = body[0];
          reqFile.setDatasetId(datasetId);
          break;
        }
      }

      if (reqFile != null) {
        // If there's no file size in the database, obtain it from RES
        if (reqFile.getFileSize() == 0) {
          ResponseEntity<Long> forSize = restTemplate
              .getForEntity(RES_SERVICE + "/file/archive/{fileId}/size", Long.class, fileId);
          reqFile.setFileSize(forSize.getBody());
        }
      } else { // 403 Unauthorized
        throw new PermissionDeniedException(HttpStatus.UNAUTHORIZED.toString());
      }
    } else { // 404 File Not Found
      throw new NotFoundException(fileId, "4");
    }
    return reqFile;
  }

}
