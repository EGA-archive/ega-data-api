package eu.elixir.ega.ebi.commons.config;

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
        return 401;
    }

    @Override
    public String getHtsgetErrorCode() {
        return "InvalidAuthentication";
    }
}

