package eu.elixir.ega.ebi.dataedge.service.internal;

import static eu.elixir.ega.ebi.shared.Constants.FILEDATABASE_SERVICE;

import eu.elixir.ega.ebi.dataedge.config.GeneralStreamingException;
import eu.elixir.ega.ebi.dataedge.config.NotFoundException;
import eu.elixir.ega.ebi.dataedge.config.PermissionDeniedException;
import eu.elixir.ega.ebi.dataedge.config.VerifyMessageNew;
import eu.elixir.ega.ebi.dataedge.dto.EventEntry;
import eu.elixir.ega.ebi.dataedge.service.AuthenticationService;
import eu.elixir.ega.ebi.dataedge.service.DownloaderLogService;
import eu.elixir.ega.ebi.dataedge.service.PermissionsService;
import eu.elixir.ega.ebi.shared.dto.FileDataset;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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
    boolean userHasPermissions = false;
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

  @Override
  public void checkFilePermissions(String stableId) {
    checkFilePermissions(stableId);
  }
}
