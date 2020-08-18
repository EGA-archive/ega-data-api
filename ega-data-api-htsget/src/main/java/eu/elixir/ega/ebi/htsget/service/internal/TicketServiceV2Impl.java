package eu.elixir.ega.ebi.htsget.service.internal;

import eu.elixir.ega.ebi.commons.config.HtsgetException;
import eu.elixir.ega.ebi.commons.config.InvalidInputException;
import eu.elixir.ega.ebi.commons.config.UnsupportedFormatException;
import eu.elixir.ega.ebi.commons.shared.config.IndexNotFoundException;
import eu.elixir.ega.ebi.commons.shared.config.NotFoundException;
import eu.elixir.ega.ebi.commons.shared.config.PermissionDeniedException;
import eu.elixir.ega.ebi.commons.shared.dto.File;
import eu.elixir.ega.ebi.commons.shared.dto.FileIndexFile;
import eu.elixir.ega.ebi.commons.shared.dto.MyExternalConfig;
import eu.elixir.ega.ebi.commons.shared.service.FileInfoService;
import eu.elixir.ega.ebi.htsget.dto.HtsgetResponseV2;
import eu.elixir.ega.ebi.htsget.dto.HtsgetUrlV2;
import eu.elixir.ega.ebi.htsget.formats.DataProvider;
import eu.elixir.ega.ebi.htsget.formats.DataProviderFactory;
import eu.elixir.ega.ebi.htsget.service.TicketServiceV2;
import htsjdk.samtools.seekablestream.SeekableStream;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

public class TicketServiceV2Impl implements TicketServiceV2 {

    public static final int MAX_BYTES_PER_DATA_BLOCK = 1024 * 1024 * 1024;

    private final FileInfoService fileInfoService;

    private final MyExternalConfig externalConfig;

    private final ResClient resClient;

    private final DataProviderFactory dataProviderFactory;


    public TicketServiceV2Impl(FileInfoService fileInfoService, MyExternalConfig externalConfig, ResClient resClient, DataProviderFactory dataProviderFactory) {
        this.fileInfoService = fileInfoService;
        this.externalConfig = externalConfig;
        this.resClient = resClient;
        this.dataProviderFactory = dataProviderFactory;
    }

    public HtsgetResponseV2 getFile(String id,
                                    String format,
                                    Optional<String> requestClass,
                                    Optional<String> referenceName,
                                    Optional<Long> start,
                                    Optional<Long> end,
                                    Optional<List<Field>> fields,
                                    Optional<List<String>> tags,
                                    Optional<List<String>> notags)
            throws HtsgetException, NotFoundException, PermissionDeniedException, IOException, URISyntaxException {

        boolean onlyHeader = false;
        if (requestClass.isPresent()) {
            if (!requestClass.get().equalsIgnoreCase("Header"))
                throw new InvalidInputException("Invalid class parameter");
            onlyHeader = true;
            if (referenceName.isPresent() ||
                    start.isPresent() ||
                    end.isPresent() ||
                    fields.isPresent() ||
                    tags.isPresent() ||
                    notags.isPresent())
                throw new InvalidInputException("Other parameters not allow for Header request");
        }

        if (start.isPresent()) {
            if (!referenceName.isPresent())
                throw new InvalidInputException("Must specify reference name if using start parameter");

            if (referenceName.get().equals("*"))
                throw new InvalidInputException("Cannot use start with unplaced unmapped reads");
        }

        // Ascertain Access Permissions for specified File ID
        File reqFile = fileInfoService.getFileInfo(id);

        DataProvider reader = dataProviderFactory.getProviderForFormat(format);
        if (!reader.supportsFileType(reqFile.getFileName()))
            throw new UnsupportedFormatException("Conversion not supported");

        // Start Building the URL for the file on DataEdge
        URI baseURI = UriComponentsBuilder
                .fromUriString(externalConfig.getEgaExternalUrl() + id)
                .queryParam("destinationFormat", "plain")
                .build().toUri();

        HtsgetResponseV2 result = new HtsgetResponseV2(format);

        if (!onlyHeader && !referenceName.isPresent()) {
            // user is requesting the entire file
            result.addUrl(new HtsgetUrlV2(baseURI));
            if (reqFile.getUnencryptedChecksumType() != null && reqFile.getUnencryptedChecksumType().equalsIgnoreCase("md5"))
                result.setMd5(reqFile.getUnencryptedChecksum());

        } else {

            try (SeekableStream dataStream = resClient.getStreamForFile(id)) {
                reader.readHeader(dataStream);
                result.addUrl(new HtsgetUrlV2(reader.getHeaderAsDataUri(), "header"));

                if (!onlyHeader) {

                    if (referenceName.get().equals("*")) {
                        // TODO
                        throw new NotImplementedException("Unplaced unmapped reads not implemented yet");
                    }

                    // Download the indexFile as we are going to look at it
                    FileIndexFile fileIndexFile = fileInfoService.getFileIndexFile(reqFile.getFileId());
                    if (fileIndexFile == null || StringUtils.isEmpty(fileIndexFile.getIndexFileId())) {
                        throw new IndexNotFoundException("IndexFileId not found for file", id);
                    }

                    try (SeekableStream indexStream = resClient.getStreamForFile(fileIndexFile.getIndexFileId())) {
                        reader.addContentUris(referenceName.get(), start.orElse(0L), end.orElse(Long.MAX_VALUE), baseURI, result, dataStream, indexStream);
                    }

                    result.addUrl(new HtsgetUrlV2(reader.getFooterAsDataUri(), "body"));
                }
                splitLargeDataBlocks(result);

            }
        }

        return result;
    }

    private void splitLargeDataBlocks(HtsgetResponseV2 result) {
        // HtsGet spec recommends that data blocks should not exceed 1GB so split Urls that are too big
        for (int i = 0; i < result.getUrls().size(); ++i) {
            HtsgetUrlV2 url = result.getUrls().get(i);
            if (url.getHeaders() == null)
                continue;
            String rangeHeader = url.getHeaders().getOrDefault(HttpHeaders.RANGE, null);
            if (rangeHeader == null)
                continue;

            List<HttpRange> ranges = HttpRange.parseRanges(rangeHeader);
            if (ranges.size() > 1)
                throw new RuntimeException("Multiple ranges on one request not supported");
            HttpRange range = ranges.get(0);
            long rangeStart = range.getRangeStart(Long.MAX_VALUE);
            long rangeEnd = range.getRangeEnd(Long.MAX_VALUE);

            while (rangeEnd - rangeStart + 1 > MAX_BYTES_PER_DATA_BLOCK) {
                HttpRange subRange = HttpRange.createByteRange(rangeStart, rangeStart + MAX_BYTES_PER_DATA_BLOCK - 1);
                HtsgetUrlV2 splitUrl = new HtsgetUrlV2(result.getUrls().get(i).getUrl(), result.getUrls().get(i).getUrlClass());
                splitUrl.setHeader(HttpHeaders.RANGE, "bytes=" + subRange.toString());
                result.getUrls().add(i, splitUrl);
                rangeStart += MAX_BYTES_PER_DATA_BLOCK;
                ++i;
            }

            url.setHeader(HttpHeaders.RANGE, "bytes=" + HttpRange.createByteRange(rangeStart, rangeEnd).toString());
        }
    }

    @Override
    public HtsgetResponseV2 getRead(String id,
                                    String format,
                                    Optional<String> requestClass,
                                    Optional<String> referenceName,
                                    Optional<Long> start,
                                    Optional<Long> end,
                                    Optional<List<Field>> fields,
                                    Optional<List<String>> tags,
                                    Optional<List<String>> notags)
            throws HtsgetException, NotFoundException, PermissionDeniedException, IOException, URISyntaxException {

        if (!(format.equalsIgnoreCase("bam") || format.equalsIgnoreCase("cram")))
            throw new UnsupportedFormatException(format);

        return getFile(id, format, requestClass, referenceName, start, end, fields, tags, notags);
    }

    @Override
    public HtsgetResponseV2 getVariant(String id,
                                       String format,
                                       Optional<String> requestClass,
                                       Optional<String> referenceName,
                                       Optional<Long> start,
                                       Optional<Long> end,
                                       Optional<List<Field>> fields,
                                       Optional<List<String>> tags,
                                       Optional<List<String>> notags)
            throws HtsgetException, NotFoundException, PermissionDeniedException, IOException, URISyntaxException {

        if (!(format.equalsIgnoreCase("vcf") || format.equalsIgnoreCase("bcf")))
            throw new UnsupportedFormatException(format);

        return getFile(id, format, requestClass, referenceName, start, end, fields, tags, notags);
    }
}
