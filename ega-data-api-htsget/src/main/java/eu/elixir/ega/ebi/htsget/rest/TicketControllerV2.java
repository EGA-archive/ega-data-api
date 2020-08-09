package eu.elixir.ega.ebi.htsget.rest;

import eu.elixir.ega.ebi.commons.config.UnsupportedFormatException;
import eu.elixir.ega.ebi.htsget.dto.HtsgetContainer;
import eu.elixir.ega.ebi.htsget.dto.HtsgetErrorResponse;
import org.apache.commons.lang.NotImplementedException;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
//@EnableDiscoveryClient
@RequestMapping("/htsget")

public class TicketControllerV2 {
    public enum Field{
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

    @RequestMapping(value = "/reads/{id}", method = GET, produces = "application/json")
    public String getRead(@PathVariable String id,
                                    @RequestParam(defaultValue = "BAM") String format,
                                    @RequestParam(name = "class") Optional<String> requestClass,
                                    @RequestParam Optional<String> referenceName,
                                    @RequestParam Optional<Long> start,
                                    @RequestParam Optional<Long> end,
                                    @RequestParam Optional<List<Field>> fields,
                                    @RequestParam Optional<List<String>> tags,
                                    @RequestParam Optional<List<String>> notags
                                  ) throws Exception {
        if (format.equals("sushi")){
            throw new UnsupportedFormatException(format);
        }
        return "{\"htsget\": {} }";
    }

    @RequestMapping(value = "/variants/{id}", method = GET)
    public HtsgetResponse getVariant(@PathVariable String id,
                                     @RequestParam(defaultValue = "VCF") String format,
                                     @RequestParam(name = "class") Optional<String> requestClass,
                                     @RequestParam Optional<String> referenceName,
                                     @RequestParam Optional<Long> start,
                                     @RequestParam Optional<Long> end,
                                     @RequestParam Optional<List<Field>> fields,
                                     @RequestParam Optional<List<String>> tags,
                                     @RequestParam Optional<List<String>> notags) {
        throw new NotImplementedException();

    }

    @RequestMapping(value = "/files/{id}", method = GET)
    public HtsgetResponse getFile(@PathVariable String id,
                                  @RequestParam String format,
                                  @RequestParam(name = "class") Optional<String> requestClass,
                                  @RequestParam Optional<String> referenceName,
                                  @RequestParam Optional<Long> start,
                                  @RequestParam Optional<Long> end,
                                  @RequestParam Optional<List<Field>> fields,
                                  @RequestParam Optional<List<String>> tags,
                                  @RequestParam Optional<List<String>> notags){
        throw new NotImplementedException();

    }

    @ExceptionHandler
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ResponseBody
    public Object handleUnsupportedFormatException(UnsupportedFormatException exception) {
        HtsgetErrorResponse response = new HtsgetErrorResponse("UnsupportedFormat", exception.getMessage());
        return new HtsgetContainer(response);
    }

}
