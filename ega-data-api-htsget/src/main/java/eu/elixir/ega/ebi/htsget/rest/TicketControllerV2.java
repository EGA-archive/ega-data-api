package eu.elixir.ega.ebi.htsget.rest;

import eu.elixir.ega.ebi.commons.config.*;
import eu.elixir.ega.ebi.commons.shared.config.NotFoundException;
import eu.elixir.ega.ebi.commons.shared.config.PermissionDeniedException;
import eu.elixir.ega.ebi.htsget.dto.HtsgetErrorResponse;
import eu.elixir.ega.ebi.htsget.service.TicketServiceV2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Optional;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
//@EnableDiscoveryClient
@RequestMapping("/htsget")
@CrossOrigin(maxAge = 30*24*60*60)
public class TicketControllerV2 {

    @Autowired
    private TicketServiceV2 service;

    @RequestMapping(value = "/reads/{id}", method = GET, produces="application/vnd.ga4gh.htsget.v1.0.0+json")
    public HtsgetResponse getRead(@PathVariable String id,
                                  @RequestParam(defaultValue = "BAM") String format,
                                  @RequestParam(name = "class") Optional<String> requestClass,
                                  @RequestParam Optional<String> referenceName,
                                  @RequestParam Optional<Long> start,
                                  @RequestParam Optional<Long> end,
                                  @RequestParam Optional<List<TicketServiceV2.Field>> fields,
                                  @RequestParam Optional<List<String>> tags,
                                  @RequestParam Optional<List<String>> notags
                                  ) throws MalformedURLException {
        return service.getRead(id, format, requestClass, referenceName, start, end, fields, tags, notags);
    }

    @RequestMapping(value = "/variants/{id}", method = GET)
    public HtsgetResponse getVariant(@PathVariable String id,
                                     @RequestParam(defaultValue = "VCF") String format,
                                     @RequestParam(name = "class") Optional<String> requestClass,
                                     @RequestParam Optional<String> referenceName,
                                     @RequestParam Optional<Long> start,
                                     @RequestParam Optional<Long> end,
                                     @RequestParam Optional<List<TicketServiceV2.Field>> fields,
                                     @RequestParam Optional<List<String>> tags,
                                     @RequestParam Optional<List<String>> notags) {
        return service.getVariant(id, format, requestClass, referenceName, start, end, fields, tags, notags);
    }

    @RequestMapping(value = "/files/{id}", method = GET)
    public HtsgetResponse getFile(@PathVariable String id,
                                  @RequestParam String format,
                                  @RequestParam(name = "class") Optional<String> requestClass,
                                  @RequestParam Optional<String> referenceName,
                                  @RequestParam Optional<Long> start,
                                  @RequestParam Optional<Long> end,
                                  @RequestParam Optional<List<TicketServiceV2.Field>> fields,
                                  @RequestParam Optional<List<String>> tags,
                                  @RequestParam Optional<List<String>> notags) throws MalformedURLException {
        if (format.equalsIgnoreCase("BAM") || format.equalsIgnoreCase("CRAM")){
            return service.getRead(id, format, requestClass, referenceName, start, end, fields, tags, notags);
        }

        if (format.equalsIgnoreCase("VCF") || format.equalsIgnoreCase("BCF")){
            return service.getVariant(id, format, requestClass, referenceName, start, end, fields, tags, notags);
        }

        throw new UnsupportedFormatException(format);
    }

    @ExceptionHandler
    public ResponseEntity<HtsgetErrorResponse> handleHtsgetException(HtsgetException exception) {
        return ResponseEntity
                .status(exception.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new HtsgetErrorResponse(exception.getHtsgetErrorCode(), exception.getMessage()));
    }

    @ExceptionHandler
    public ResponseEntity<HtsgetErrorResponse> handlePermissionDeniedException(PermissionDeniedException exception) {
        return ResponseEntity
                .status(403)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new HtsgetErrorResponse("PermissionDenied", exception.getMessage()));
    }

    @ExceptionHandler
    public ResponseEntity<HtsgetErrorResponse> handleNotFoundException(NotFoundException exception) {
        return ResponseEntity
                .status(404)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new HtsgetErrorResponse("NotFound", exception.getMessage()));
    }

    @ExceptionHandler
    public ResponseEntity<HtsgetErrorResponse> handleInvalidAuthenticationException(InvalidAuthenticationException exception) {
        return ResponseEntity
                .status(401)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new HtsgetErrorResponse("InvalidAuthentication", exception.getMessage()));
    }

}
