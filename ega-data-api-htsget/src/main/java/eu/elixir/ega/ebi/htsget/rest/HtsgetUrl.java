package eu.elixir.ega.ebi.htsget.rest;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;

public class HtsgetUrl {
    private URL url;
    private HashMap<String, String> headers;
    private String urlClass;

    public HtsgetUrl(URI baseURI) throws MalformedURLException {
        this.url = baseURI.toURL();
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public HashMap<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(HashMap<String, String> headers) {
        this.headers = headers;
    }

    public String getUrlClass() {
        return urlClass;
    }

    public void setUrlClass(String urlClass) {
        this.urlClass = urlClass;
    }
}
