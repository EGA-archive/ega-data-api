package eu.elixir.ega.ebi.htsget.service;

import eu.elixir.ega.ebi.commons.config.HtsgetException;
import eu.elixir.ega.ebi.commons.shared.config.NotFoundException;
import eu.elixir.ega.ebi.commons.shared.config.PermissionDeniedException;
import eu.elixir.ega.ebi.htsget.rest.HtsgetResponse;
import org.omg.CosNaming.NamingContextPackage.NotFound;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Optional;

public interface TicketServiceV2 {

    HtsgetResponse getRead (String id,
                           String format,
                           Optional<String> requestClass,
                           Optional<String> referenceName,
                           Optional<Long> start,
                           Optional<Long> end,
                           Optional<List<Field>> fields,
                           Optional<List<String>> tags,
                           Optional<List<String>> notags)
            throws HtsgetException, NotFoundException, PermissionDeniedException, MalformedURLException;

    HtsgetResponse getVariant(String id,
                              String format,
                              Optional<String> requestClass,
                              Optional<String> referenceName,
                              Optional<Long> start,
                              Optional<Long> end,
                              Optional<List<Field>> fields,
                              Optional<List<String>> tags,
                              Optional<List<String>> notags)
            throws HtsgetException, NotFoundException, PermissionDeniedException;;

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
