package eu.elixir.ega.ebi.htsget.rest;

import eu.elixir.ega.ebi.commons.config.HtsgetException;
import eu.elixir.ega.ebi.commons.config.InvalidAuthenticationException;
import eu.elixir.ega.ebi.commons.config.UnsupportedFormatException;
import eu.elixir.ega.ebi.commons.exception.NotFoundException;
import eu.elixir.ega.ebi.commons.exception.PermissionDeniedException;
import eu.elixir.ega.ebi.htsget.dto.HtsgetErrorResponse;
import eu.elixir.ega.ebi.htsget.dto.HtsgetResponseV2;
import eu.elixir.ega.ebi.htsget.dto.HtsgetUrlV2;
import eu.elixir.ega.ebi.htsget.service.TicketServiceV2;
import org.apache.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
//@EnableDiscoveryClient
@RequestMapping("/htsget")
@CrossOrigin(maxAge = 30 * 24 * 60 * 60)
public class TicketControllerV2 {

    @Autowired
    private TicketServiceV2 service;

    @RequestMapping(value = "/version", method = GET)
    @ResponseBody
    public String getVersion() {
        return "v1.0.0";
    }

    @RequestMapping(value = "/reads/{id}", method = GET, produces = "application/vnd.ga4gh.htsget.v1.0.0+json")
    public HtsgetResponseV2 getRead(@PathVariable String id,
                                    @RequestParam(defaultValue = "BAM") String format,
                                    @RequestParam(name = "class") Optional<String> requestClass,
                                    @RequestParam Optional<String> referenceName,
                                    @RequestParam Optional<Long> start,
                                    @RequestParam Optional<Long> end,
                                    @RequestParam Optional<List<TicketServiceV2.Field>> fields,
                                    @RequestParam Optional<List<String>> tags,
                                    @RequestParam Optional<List<String>> notags,
                                    @RequestHeader String authorization
    ) throws IOException, URISyntaxException {
        HtsgetResponseV2 response = service.getRead(id, format, requestClass, referenceName, start, end, fields, tags, notags);
        for (HtsgetUrlV2 url : response.getUrls()) {
            url.setHeader(HttpHeaders.AUTHORIZATION, authorization);
        }
        return response;
    }

    @RequestMapping(value = "/variants/{id}", method = GET)
    public HtsgetResponseV2 getVariant(@PathVariable String id,
                                       @RequestParam(defaultValue = "VCF") String format,
                                       @RequestParam(name = "class") Optional<String> requestClass,
                                       @RequestParam Optional<String> referenceName,
                                       @RequestParam Optional<Long> start,
                                       @RequestParam Optional<Long> end,
                                       @RequestParam Optional<List<TicketServiceV2.Field>> fields,
                                       @RequestParam Optional<List<String>> tags,
                                       @RequestParam Optional<List<String>> notags,
                                       @RequestHeader String authorization) throws IOException, URISyntaxException {
        HtsgetResponseV2 response = service.getVariant(id, format, requestClass, referenceName, start, end, fields, tags, notags);
        for (HtsgetUrlV2 url : response.getUrls()) {
            url.setHeader(HttpHeaders.AUTHORIZATION, authorization);
        }
        return response;
    }

    @RequestMapping(value = "/files/{id}", method = GET)
    public HtsgetResponseV2 getFile(@PathVariable String id,
                                    @RequestParam String format,
                                    @RequestParam(name = "class") Optional<String> requestClass,
                                    @RequestParam Optional<String> referenceName,
                                    @RequestParam Optional<Long> start,
                                    @RequestParam Optional<Long> end,
                                    @RequestParam Optional<List<TicketServiceV2.Field>> fields,
                                    @RequestParam Optional<List<String>> tags,
                                    @RequestParam Optional<List<String>> notags,
                                    @RequestHeader String authorization) throws IOException, URISyntaxException {
        HtsgetResponseV2 response;
        if (format.equalsIgnoreCase("BAM") || format.equalsIgnoreCase("CRAM")) {
            response = service.getRead(id, format, requestClass, referenceName, start, end, fields, tags, notags);
        } else if (format.equalsIgnoreCase("VCF") || format.equalsIgnoreCase("BCF")) {
            response = service.getVariant(id, format, requestClass, referenceName, start, end, fields, tags, notags);
        } else {
            throw new UnsupportedFormatException(format);
        }

        for (HtsgetUrlV2 url : response.getUrls()) {
            url.setHeader(HttpHeaders.AUTHORIZATION, authorization);
        }
        return response;
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

    @ExceptionHandler
    public ResponseEntity<HtsgetErrorResponse> handleServletRequestBindingException(ServletRequestBindingException exception) {
        return ResponseEntity
                .status(401)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new HtsgetErrorResponse("InvalidAuthentication", "Request missing Authorization header"));
    }
}
