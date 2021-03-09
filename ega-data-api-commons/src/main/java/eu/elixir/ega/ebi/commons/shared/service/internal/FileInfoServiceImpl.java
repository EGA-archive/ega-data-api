package eu.elixir.ega.ebi.commons.shared.service.internal;

import eu.elixir.ega.ebi.commons.exception.NotFoundException;
import eu.elixir.ega.ebi.commons.shared.dto.File;
import eu.elixir.ega.ebi.commons.shared.dto.FileIndexFile;
import eu.elixir.ega.ebi.commons.shared.service.FileInfoService;
import eu.elixir.ega.ebi.commons.shared.service.PermissionsService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import static eu.elixir.ega.ebi.commons.config.Constants.FILEDATABASE_SERVICE;

@Service
public class FileInfoServiceImpl implements FileInfoService {

    private PermissionsService permissionService;
    private RestTemplate restTemplate;

    public FileInfoServiceImpl(PermissionsService permissionService, RestTemplate restTemplate) {
        this.permissionService = permissionService;
        this.restTemplate = restTemplate;
    }

    @Override
    @Cacheable(cacheNames = "reqFile", key = "T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication() + #p0")
    public File getFileInfo(String fileId, String sessionId) {

        //check User has permissions for this file
        String datasetId = permissionService.getFilePermissionsEntity(fileId);

        File reqFile = null;
        ResponseEntity<File[]> forEntity = restTemplate
                .getForEntity(FILEDATABASE_SERVICE + "/file/{fileId}", File[].class, fileId);
        File[] body = forEntity.getBody();
        if (body != null && body.length > 0) {
            reqFile = body[0];
            reqFile.setDatasetId(datasetId);
        } else { // 404 File Not Found
            throw new NotFoundException(fileId, "4");
        }
        return reqFile;
    }

    @Cacheable(cacheNames = "indexFile")
    public FileIndexFile getFileIndexFile(String fileId) {
        FileIndexFile indexFile = null;
        ResponseEntity<FileIndexFile[]> forEntity = restTemplate.getForEntity(FILEDATABASE_SERVICE + "/file/{fileId}/index", FileIndexFile[].class, fileId);
        FileIndexFile[] body = forEntity.getBody();
        if (body != null && body.length >= 1) {
            indexFile = body[0];
        }
        return indexFile;
    }
    
}
