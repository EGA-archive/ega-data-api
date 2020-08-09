package eu.elixir.ega.ebi.htsget.rest;

import java.net.URL;
import java.util.HashMap;

public class HtsgetResponse {
    private String format;
    private HtsgetUrl[] urls;
    private String md5;
    public class HtsgetUrl {
        private URL url;
        private HashMap<String, String> headers;
        private String urlClass;


    }
}
