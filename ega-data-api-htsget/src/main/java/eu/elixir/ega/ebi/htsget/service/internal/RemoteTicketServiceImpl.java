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
package eu.elixir.ega.ebi.htsget.service.internal;

import eu.elixir.ega.ebi.commons.exception.IndexNotFoundException;
import eu.elixir.ega.ebi.commons.exception.NotFoundException;
import eu.elixir.ega.ebi.commons.shared.dto.File;
import eu.elixir.ega.ebi.commons.shared.dto.FileIndexFile;
import eu.elixir.ega.ebi.commons.shared.dto.MyExternalConfig;
import eu.elixir.ega.ebi.commons.shared.service.FileInfoService;
import eu.elixir.ega.ebi.htsget.dto.HtsgetContainer;
import eu.elixir.ega.ebi.htsget.dto.HtsgetErrorResponse;
import eu.elixir.ega.ebi.htsget.dto.HtsgetHeader;
import eu.elixir.ega.ebi.htsget.dto.HtsgetResponse;
import eu.elixir.ega.ebi.htsget.dto.HtsgetUrl;
import eu.elixir.ega.ebi.htsget.service.TicketService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

import static eu.elixir.ega.ebi.commons.config.Constants.FILEDATABASE_SERVICE;
import static eu.elixir.ega.ebi.commons.config.Constants.RES_SERVICE;

/**
 * @author asenf
 */
@Service
@EnableDiscoveryClient
public class RemoteTicketServiceImpl implements TicketService {

    @Autowired
    private MyExternalConfig externalConfig;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private FileInfoService fileInfoService;

    @Override
    public Object getTicket(String fileId,
                            String format,
                            int referenceIndex,
                            String referenceName,
                            String referenceMD5,
                            String start,
                            String end,
                            List<String> fields,
                            List<String> tags,
                            List<String> notags,
                            HttpServletRequest request,
                            HttpServletResponse response) {
        // Get Auth Token
        String token = request.getHeader("Authorization");
        if (token == null || token.length() == 0)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.valueOf("application/vnd.ga4gh.htsget.v1.0+json; charset=utf-8"))
                    .body(new HtsgetContainer(new HtsgetErrorResponse("InvalidInput", "EGA requires oauth token")));

        // Ascertain Access Permissions for specified File ID
        File reqFile;
        try {
            reqFile = fileInfoService.getFileInfo(fileId); // request added for ELIXIR
        } catch (NotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.valueOf("application/vnd.ga4gh.htsget.v1.0+json; charset=utf-8"))
                    .body(new HtsgetContainer(new HtsgetErrorResponse("NotFound", "No such accession '" + fileId + "'")));
        }

        FileIndexFile fileIndexFile = getFileIndexFile(reqFile.getFileId());
        if (fileIndexFile == null || StringUtils.isEmpty(fileIndexFile.getIndexFileId())) {
            throw new IndexNotFoundException("IndexFileId not found for file", fileId);
        }

        boolean reference;
        reference = (referenceIndex > -1 || (referenceName != null && referenceName.length() > 0) || (referenceMD5 != null && referenceMD5.length() > 0));
        if (!reference && ((start != null && start.length() > 0) || (end != null && end.length() > 0))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.valueOf("application/vnd.ga4gh.htsget.v1.0+json; charset=utf-8"))
                    .body(new HtsgetContainer(new HtsgetErrorResponse("InvalidInput", "range specified without reference")));
        }

        if (reqFile != null) {
            HtsgetHeader authHeader = new HtsgetHeader(token);

            ArrayList<HtsgetUrl> urls = new ArrayList<>();

            // Header URL
//            String headerUrl = externalConfig.getEgaExternalUrl() + file_id + "/header?destinationFormat=plain";
//            urls.add(new HtsgetUrl("header", authHeader));

            // Data URL(s)
            String url = externalConfig.getEgaExternalUrl() + "byid/file?accession=" + fileId;
            if (format != null && format.length() > 0)
                url += "&format=" + format;
            if (start != null && start.length() > 0)
                url += "&start=" + start;
            if (end != null && end.length() > 0)
                url += "&end=" + end;
            if (referenceName != null && referenceName.length() > 0)
                url += "&chr=" + referenceName;
            if (fields != null && fields.size() > 0) {
                url += "&fields=";
                for (String field : fields)
                    url += field + ",";
                url = url.substring(0, url.length() - 1); // remove trailing comma
            }
            if (tags != null && tags.size() > 0) {
                url += "&tags=";
                for (String tag : tags)
                    url += tag + ",";
                url = url.substring(0, url.length() - 1); // remove trailing comma
            }
            if (notags != null && notags.size() > 0) {
                url += "&notags=";
                for (String notag : notags)
                    url += notag + ",";
                url = url.substring(0, url.length() - 1); // remove trailing comma
            }
            urls.add(new HtsgetUrl(url, authHeader));

            return ResponseEntity.status(HttpStatus.OK)
                    .contentType(MediaType.valueOf("application/vnd.ga4gh.htsget.v1.0+json; charset=utf-8"))
                    .body(new HtsgetContainer(new HtsgetResponse(format.toUpperCase(), urls.toArray(new HtsgetUrl[urls.size()]))));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.valueOf("application/vnd.ga4gh.htsget.v1.0+json; charset=utf-8"))
                .body(new HtsgetContainer(new HtsgetErrorResponse("UnAuthorized", "No authorization for accession '" + fileId + "'")));
    }

    @Override
    public Object getVariantTicket(String fileId,
                                   String format,
                                   int referenceIndex,
                                   String referenceName,
                                   String referenceMD5,
                                   String start,
                                   String end,
                                   List<String> fields,
                                   List<String> tags,
                                   List<String> notags,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {
        // Get Auth Token
        String token = request.getHeader("Authorization");
        if (token == null || token.length() == 0)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.valueOf("application/vnd.ga4gh.htsget.v1.0+json; charset=utf-8"))
                    .body(new HtsgetContainer(new HtsgetErrorResponse("InvalidInput", "EGA requires oauth token")));

        // Ascertain Access Permissions for specified File ID
        File reqFile;
        try {
            reqFile = fileInfoService.getFileInfo(fileId); // request added for ELIXIR
        } catch (NotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.valueOf("application/vnd.ga4gh.htsget.v1.0+json; charset=utf-8"))
                    .body(new HtsgetContainer(new HtsgetErrorResponse("NotFound", "No such accession '" + fileId + "'")));
        }

        FileIndexFile fileIndexFile = getFileIndexFile(reqFile.getFileId());
        if (fileIndexFile == null || StringUtils.isEmpty(fileIndexFile.getIndexFileId())) {
            throw new IndexNotFoundException("IndexFileId not found for file", fileId);
        }

        boolean reference;
        reference = (referenceIndex > -1 || (referenceName != null && referenceName.length() > 0) || (referenceMD5 != null && referenceMD5.length() > 0));
        if (!reference && ((start != null && start.length() > 0) || (end != null && end.length() > 0))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.valueOf("application/vnd.ga4gh.htsget.v1.0+json; charset=utf-8"))
                    .body(new HtsgetContainer(new HtsgetErrorResponse("InvalidInput", "range specified without reference")));
        }

        if (reqFile != null) {
            HtsgetHeader authHeader = new HtsgetHeader(token);

            ArrayList<HtsgetUrl> urls = new ArrayList<>();

            // Header URL
//            String headerUrl = externalConfig.getEgaExternalUrl() + file_id + "/header?destinationFormat=plain";
//            urls.add(new HtsgetUrl("header", authHeader));

            // Data URL(s)
            String url = externalConfig.getEgaExternalUrl() + "variant/byid/file?accession=" + fileId;
            if (format != null && format.length() > 0)
                url += "&format=" + format;
            if (start != null && start.length() > 0)
                url += "&start=" + start;
            if (end != null && end.length() > 0)
                url += "&end=" + end;
            if (referenceName != null && referenceName.length() > 0)
                url += "&chr=" + referenceName;
            if (fields != null && fields.size() > 0) {
                url += "&fields=";
                for (String field : fields)
                    url += field + ",";
                url = url.substring(0, url.length() - 1); // remove trailing comma
            }
            if (tags != null && tags.size() > 0) {
                url += "&tags=";
                for (String tag : tags)
                    url += tag + ",";
                url = url.substring(0, url.length() - 1); // remove trailing comma
            }
            if (notags != null && notags.size() > 0) {
                url += "&notags=";
                for (String notag : notags)
                    url += notag + ",";
                url = url.substring(0, url.length() - 1); // remove trailing comma
            }
            urls.add(new HtsgetUrl(url, authHeader));

            return ResponseEntity.status(HttpStatus.OK)
                    .contentType(MediaType.valueOf("application/vnd.ga4gh.htsget.v1.0+json; charset=utf-8"))
                    .body(new HtsgetContainer(new HtsgetResponse(format.toUpperCase(), urls.toArray(new HtsgetUrl[urls.size()]))));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.valueOf("application/vnd.ga4gh.htsget.v1.0+json; charset=utf-8"))
                .body(new HtsgetContainer(new HtsgetErrorResponse("UnAuthorized", "No authorization for accession '" + fileId + "'")));
    }

    @Cacheable(cacheNames = "indexFile")
    private FileIndexFile getFileIndexFile(String fileId) {
        FileIndexFile indexFile = null;
        ResponseEntity<FileIndexFile[]> forEntity = restTemplate.getForEntity(FILEDATABASE_SERVICE + "/file/{fileId}/index", FileIndexFile[].class, fileId);
        FileIndexFile[] body = forEntity.getBody();
        if (body != null && body.length >= 1) {
            indexFile = body[0];
        }
        return indexFile;
    }

    public String resUrl() {
        return RES_SERVICE;
    }

}
