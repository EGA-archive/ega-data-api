package eu.elixir.ega.ebi.commons.config;


public class InvalidRangeException extends HtsgetException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new Invalid range exception with the given message.
     *
     * @param code the detail message.
     */
    public InvalidRangeException(String code) {
        super("Invalid Range : " + code);
    }

    @Override
    public int getStatusCode() {
        return 400;
    }

    @Override
    public String getHtsgetErrorCode() {
        return "InvalidRange";
    }
}