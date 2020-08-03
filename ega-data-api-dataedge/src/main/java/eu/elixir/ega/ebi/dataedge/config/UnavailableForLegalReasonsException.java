package eu.elixir.ega.ebi.dataedge.config;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS)
public class UnavailableForLegalReasonsException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public UnavailableForLegalReasonsException(String msg) {
        super(msg);
    }
}