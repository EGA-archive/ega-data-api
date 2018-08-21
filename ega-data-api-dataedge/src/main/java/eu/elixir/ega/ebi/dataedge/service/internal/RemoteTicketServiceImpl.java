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
package eu.elixir.ega.ebi.dataedge.service.internal;

import eu.elixir.ega.ebi.dataedge.config.NotFoundException;
import eu.elixir.ega.ebi.dataedge.config.VerifyMessageNew;
import eu.elixir.ega.ebi.dataedge.dto.*;
import eu.elixir.ega.ebi.dataedge.service.TicketService;
import htsjdk.samtools.BAMFileSpan;
import htsjdk.samtools.BAMIndex;
import htsjdk.samtools.QueryInterval;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

//import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

/**
 * @author asenf
 */
@Service
@Transactional
@EnableDiscoveryClient
public class RemoteTicketServiceImpl implements TicketService {

    public static final String SERVICE_URL = "http://FILEDATABASE2";
    public static final String RES_URL = "http://RES2";

    @Autowired
    private MyExternalConfig externalConfig;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * Use the index to determine the chunk boundaries for the required intervals.
     *
     * @param intervals the intervals to restrict reads to
     * @param fileIndex the BAM index to use
     * @return file pointer pairs corresponding to chunk boundaries
     */
    public static BAMFileSpan getFileSpan(QueryInterval[] intervals, BAMIndex fileIndex) {
        final BAMFileSpan[] inputSpans = new BAMFileSpan[intervals.length];
        for (int i = 0; i < intervals.length; ++i) {
            final QueryInterval interval = intervals[i];
            final BAMFileSpan span = fileIndex.getSpanOverlapping(interval.referenceIndex, interval.start, interval.end);
            inputSpans[i] = span;
        }
        final BAMFileSpan span;
        if (inputSpans.length > 0) {
            span = BAMFileSpan.merge(inputSpans);
        } else {
            span = null;
        }
        return span;
    }

    @Override
    //@HystrixCommand
    public Object getTicket(Authentication auth,
                            String fileId,
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
            reqFile = getReqFile(fileId, auth, request); // request added for ELIXIR
        } catch (NotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.valueOf("application/vnd.ga4gh.htsget.v1.0+json; charset=utf-8"))
                    .body(new HtsgetContainer(new HtsgetErrorResponse("NotFound", "No such accession '" + fileId + "'")));
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
    //@HystrixCommand
    public Object getVariantTicket(Authentication auth,
                                   String fileId,
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
            reqFile = getReqFile(fileId, auth, request); // request added for ELIXIR
        } catch (NotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.valueOf("application/vnd.ga4gh.htsget.v1.0+json; charset=utf-8"))
                    .body(new HtsgetContainer(new HtsgetErrorResponse("NotFound", "No such accession '" + fileId + "'")));
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

    // *************************************************************************
    //@HystrixCommand
    @Cacheable(cacheNames = "reqFile")
    private File getReqFile(String fileId, Authentication auth, HttpServletRequest request)
            throws NotFoundException {

        // Obtain all Authorised Datasets (Provided by EGA AAI)
        HashSet<String> permissions = new HashSet<>();
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        if (authorities != null && authorities.size() > 0) {
            Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
            while (iterator.hasNext()) {
                GrantedAuthority next = iterator.next();
                permissions.add(next.getAuthority());
            }
        } else if (request != null) { // ELIXIR User Case: Obtain Permmissions from X-Permissions Header
            try {
                List<String> permissions_ = (new VerifyMessageNew(request.getHeader("X-Permissions"))).getPermissions();
                if (permissions_ != null && permissions_.size() > 0) {
                    //StringTokenizer t = new StringTokenizer(permissions, ",");
                    //while (t!=null && t.hasMoreTokens()) {
                    for (String ds : permissions_) {
                        //String ds = t.nextToken();
                        if (ds != null && ds.length() > 0) permissions.add(ds);
                    }
                }
            } catch (Exception ex) {
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
            }
        }

        ResponseEntity<FileDataset[]> forEntityDataset = restTemplate.getForEntity(SERVICE_URL + "/file/{fileId}/datasets", FileDataset[].class, fileId);
        FileDataset[] bodyDataset = forEntityDataset.getBody();

        File reqFile = null;
        ResponseEntity<File[]> forEntity = restTemplate.getForEntity(SERVICE_URL + "/file/{fileId}", File[].class, fileId);
        File[] body = forEntity.getBody();
        if ((body != null && body.length > 0) && bodyDataset != null) {
            for (FileDataset f : bodyDataset) {
                String datasetId = f.getDatasetId();
                if (permissions.contains(datasetId) && body.length >= 1) {
                    reqFile = body[0];
                    reqFile.setDatasetId(datasetId);
                    break;
                }
            }
        } else { // 404 File Not Found
            throw new NotFoundException(fileId, "File not found.");
        }

        return reqFile;
    }

    //@HystrixCommand
    @Cacheable(cacheNames = "indexFile")
    private FileIndexFile getFileIndexFile(String fileId) {
        FileIndexFile indexFile = null;
        ResponseEntity<FileIndexFile[]> forEntity = restTemplate.getForEntity(SERVICE_URL + "/file/{fileId}/index", FileIndexFile[].class, fileId);
        FileIndexFile[] body = forEntity.getBody();
        if (body != null && body.length >= 1) {
            indexFile = body[0];
        }
        return indexFile;
    }

    //@HystrixCommand
    public String resUrl() {
        return RES_URL;
    }

}
