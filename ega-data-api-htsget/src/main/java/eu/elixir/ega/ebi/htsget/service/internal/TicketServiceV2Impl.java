package eu.elixir.ega.ebi.htsget.service.internal;

import eu.elixir.ega.ebi.commons.config.HtsgetException;
import eu.elixir.ega.ebi.commons.config.InvalidInputException;
import eu.elixir.ega.ebi.commons.shared.config.IndexNotFoundException;
import eu.elixir.ega.ebi.commons.shared.config.NotFoundException;
import eu.elixir.ega.ebi.commons.shared.config.PermissionDeniedException;
import eu.elixir.ega.ebi.commons.shared.dto.File;
import eu.elixir.ega.ebi.commons.shared.dto.FileIndexFile;
import eu.elixir.ega.ebi.commons.shared.dto.MyExternalConfig;
import eu.elixir.ega.ebi.commons.shared.service.FileInfoService;
import eu.elixir.ega.ebi.htsget.rest.HtsgetResponse;
import eu.elixir.ega.ebi.htsget.rest.HtsgetUrl;
import eu.elixir.ega.ebi.htsget.service.TicketServiceV2;
import htsjdk.samtools.*;
import htsjdk.samtools.seekablestream.SeekableHTTPStream;
import org.apache.commons.lang.StringUtils;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.web.util.UriComponentsBuilder;
import htsjdk.samtools.seekablestream.SeekableStream;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class TicketServiceV2Impl implements TicketServiceV2 {

    private FileInfoService fileInfoService;

    private MyExternalConfig externalConfig;

    private LoadBalancerClient loadBalancer;


    public TicketServiceV2Impl(FileInfoService fileInfoService, MyExternalConfig externalConfig, LoadBalancerClient loadBalancer) {
        this.fileInfoService = fileInfoService;
        this.externalConfig = externalConfig;
        this.loadBalancer = loadBalancer;
    }

    @Override
    public HtsgetResponse getRead(String id,
                                  String format,
                                  Optional<String> requestClass,
                                  Optional<String> referenceName,
                                  Optional<Long> start,
                                  Optional<Long> end,
                                  Optional<List<Field>> fields,
                                  Optional<List<String>> tags,
                                  Optional<List<String>> notags)
            throws HtsgetException, NotFoundException, PermissionDeniedException, MalformedURLException {

        if(requestClass.isPresent()) {
            if(!requestClass.get().equalsIgnoreCase("Header")) throw new InvalidInputException("Invalid class parameter");
            if(referenceName.isPresent() ||
                    start.isPresent() ||
                    end.isPresent() ||
                    fields.isPresent() ||
                    tags.isPresent() ||
                    notags.isPresent())
                throw new InvalidInputException("Other parameters not allow for Header request");

        }

        if (start.isPresent()) {
            if (!referenceName.isPresent()) {
                throw new InvalidInputException("Must specify reference name if using start parameter");
            }

            if (referenceName.get().equals("*")) throw new InvalidInputException("Cannot use start with unplaced unmapped reads");
        }

        // Ascertain Access Permissions for specified File ID
        File reqFile = fileInfoService.getFileInfo(id);

        // Start Building the URl for the file on DataEdge
        URI baseURI = UriComponentsBuilder
                .fromUriString(externalConfig.getEgaExternalUrl() + id)
                .queryParam("destinationFormat", "plain")
                .build().toUri();

        HtsgetResponse result = new HtsgetResponse();
        ArrayList<HtsgetUrl> urls = new ArrayList<HtsgetUrl>();
        if (requestClass.isPresent() || referenceName.isPresent()) {
            urls.add(new HtsgetUrl(UriComponentsBuilder
                    .fromUri(baseURI)
                    .pathSegment("header")
                    .build().toUri()));
        }
        else {
            urls.add(new HtsgetUrl(baseURI));
        }

        if (referenceName.isPresent() && !requestClass.isPresent()) {
            // Download the header to get the sequence dictionary
            SamReaderFactory readerFactory = SamReaderFactory.makeDefault();
            SeekableStream headerStream = new SeekableHTTPStream(UriComponentsBuilder
                    .fromUri(baseURI)
                    .pathSegment("header")
                    .build()
                    .toUri()
                    .toURL());

            SamInputResource headerResource = SamInputResource.of(headerStream);
            SAMFileHeader header = readerFactory.open(headerResource).getFileHeader();
            SAMSequenceDictionary sequenceDictionary = header.getSequenceDictionary();

            // Download the fileIndex if we are going to look at it
            FileIndexFile fileIndexFile = fileInfoService.getFileIndexFile(reqFile.getFileId());
            if (fileIndexFile == null || StringUtils.isEmpty(fileIndexFile.getIndexFileId())) {
                throw new IndexNotFoundException("IndexFileId not found for file", id);
            }
            // Download the index file from RES
            URL resIndexURL = new URL(resURL() + "/file/archive/" + fileIndexFile.getIndexFileId());
            SeekableStream indexStream = new SeekableHTTPStream(resIndexURL);

            // If this is a BAM file, make a DiskBasedBAMFileIndex
            DiskBasedBAMFileIndex index = new DiskBasedBAMFileIndex(indexStream,
                    sequenceDictionary);

            // Get spans overlapping to get the byte range
            int sequenceIndex = sequenceDictionary.getSequenceIndex(referenceName.get());
            BAMFileSpan span = index.getSpanOverlapping(sequenceIndex,
                    start.get().intValue(),
                    end.get().intValue());

            // Build the URl for DataEdge but one thing
            for (Chunk chunk : span.getChunks()) {
                long byteStart = chunk.getChunkStart();
                long byteEnd = chunk.getChunkEnd();

                byteStart = (byteStart >> 16)&0xffffffffffffL;
                byteEnd = (byteEnd >> 16)&0xffffffffffffL;

                HtsgetUrl chunkURL = new HtsgetUrl(baseURI);
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Range", String.format("bytes=%d-%d", byteStart, byteEnd));
                chunkURL.setHeaders(headers);
                urls.add(chunkURL);
            }
        }

        result.setFormat(format);
        result.setUrls(urls.toArray(new HtsgetUrl[0]));

        return result;
    }

    @Override
    public HtsgetResponse getVariant(String id,
                                     String format,
                                     Optional<String> requestClass,
                                     Optional<String> referenceName,
                                     Optional<Long> start, Optional<Long> end,
                                     Optional<List<Field>> fields,
                                     Optional<List<String>> tags,
                                     Optional<List<String>> notags)
            throws HtsgetException, NotFoundException, PermissionDeniedException {
        return null;
    }

    private String resURL() {
        return loadBalancer.choose("RES2").getUri().toString();
    }
}
