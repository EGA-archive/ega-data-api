package eu.elixir.ega.ebi.commons.config;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(HttpStatus.CONFLICT)
public class InvalidAuthenticationException extends HtsgetException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new unsupported format exception with the given message.
     *
     * @param code the detail message.
     */
    public InvalidAuthenticationException(String code) {
        super("Invalid Authentication : " + code);
    }

    @Override
    public int getStatusCode() {
        return 400;
    }

    @Override
    public String getHtsgetErrorCode() {
        return "InvalidAuthentication";
    }
}

