package eu.elixir.ega.ebi.htsget.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.net.URI;
import java.util.HashMap;

public class HtsgetUrlV2 {

    private URI url;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private HashMap<String, String> headers;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String urlClass;

    public HtsgetUrlV2(URI baseURI) {
        this(baseURI, null);
    }

    public HtsgetUrlV2(URI baseURI, String urlClass) {
        this.url = baseURI;
        this.urlClass = urlClass;
    }

    public URI getUrl() {
        return url;
    }

    public HashMap<String, String> getHeaders() {
        return headers;
    }

    public String getUrlClass() {
        return urlClass;
    }

    public void setHeader(String name, String value) {
        if (this.headers == null) {
            this.headers = new HashMap<>();
        }
        this.headers.put(name, value);
    }
}
