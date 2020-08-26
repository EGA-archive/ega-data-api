package eu.elixir.ega.ebi.commons.config;

public abstract class HtsgetException extends RuntimeException {

    public HtsgetException(String message) {
        super(message);
    }

    public abstract int getStatusCode();

    public abstract String getHtsgetErrorCode();

}
