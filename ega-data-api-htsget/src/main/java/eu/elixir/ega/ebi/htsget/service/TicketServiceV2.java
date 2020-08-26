package eu.elixir.ega.ebi.htsget.service;

import eu.elixir.ega.ebi.commons.config.HtsgetException;
import eu.elixir.ega.ebi.commons.exception.NotFoundException;
import eu.elixir.ega.ebi.commons.exception.PermissionDeniedException;
import eu.elixir.ega.ebi.htsget.dto.HtsgetResponseV2;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

public interface TicketServiceV2 {

    HtsgetResponseV2 getRead(String id,
                             String format,
                             Optional<String> requestClass,
                             Optional<String> referenceName,
                             Optional<Long> start,
                             Optional<Long> end,
                             Optional<List<Field>> fields,
                             Optional<List<String>> tags,
                             Optional<List<String>> notags)
            throws HtsgetException, NotFoundException, PermissionDeniedException, IOException, URISyntaxException;

    HtsgetResponseV2 getVariant(String id,
                                String format,
                                Optional<String> requestClass,
                                Optional<String> referenceName,
                                Optional<Long> start,
                                Optional<Long> end,
                                Optional<List<Field>> fields,
                                Optional<List<String>> tags,
                                Optional<List<String>> notags)
            throws HtsgetException, NotFoundException, PermissionDeniedException, IOException, URISyntaxException;

    enum Field {
        QNAME,
        FLAG,
        RNAME,
        POS,
        MAPQ,
        CIGAR,
        RNEXT,
        PNEXT,
        TLEN,
        SEQ,
        QUAL
    }
}
