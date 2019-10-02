package eu.elixir.ega.ebi.shared.service.internal;

import eu.elixir.ega.ebi.shared.config.NotFoundException;
import eu.elixir.ega.ebi.shared.dto.File;
import eu.elixir.ega.ebi.shared.service.FileInfoService;
import eu.elixir.ega.ebi.shared.service.PermissionsService;

import static eu.elixir.ega.ebi.htsget.config.Constants.FILEDATABASE_SERVICE;
import static eu.elixir.ega.ebi.htsget.config.Constants.RES_SERVICE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class FileInfoServiceImpl implements FileInfoService{

  @Autowired
  PermissionsService permissionService;

  @Autowired
  private RestTemplate restTemplate;

  @Override
  @Cacheable(cacheNames = "reqFile", key="T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication() + #p0")
  public File getFileInfo(String fileId) {

    //check User has permissions for this file
    String datasetId = permissionService.getFilePermissionsEntity(fileId);

    File reqFile = null;
    ResponseEntity<File[]> forEntity = restTemplate
        .getForEntity(FILEDATABASE_SERVICE + "/file/{fileId}", File[].class, fileId);
    File[] body = forEntity.getBody();
    if (body != null) {
      reqFile = body[0];
      reqFile.setDatasetId(datasetId);
      // If there's no file size in the database, obtain it from RES
      if (reqFile.getFileSize() == 0) {
        ResponseEntity<Long> forSize = restTemplate
            .getForEntity(RES_SERVICE + "/file/archive/{fileId}/size", Long.class, fileId);
        reqFile.setFileSize(forSize.getBody());
      }
    } else { // 404 File Not Found
      throw new NotFoundException(fileId, "4");
    }
    return reqFile;
  }

}
