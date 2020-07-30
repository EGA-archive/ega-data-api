package eu.elixir.ega.ebi.commons.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Shared constants for the dataedge service
 */
@Component
public class Constants {

    public static String FILEDATABASE_SERVICE;

    public static String RES_SERVICE;

    public static String KEYS_SERVICE;

    @Value("${ega.internal.filedatabase.url}")
    public void setFileDatabaseService(String filedburl) {
        FILEDATABASE_SERVICE = filedburl;
    }
    
    @Value("${ega.internal.res.url}")
    public void setKeyService(String resurl) {
        RES_SERVICE = resurl;
    }
    
    @Value("${ega.internal.key.url}")
    public void setResService(String keyurl) {
        KEYS_SERVICE = keyurl;
    } 

}
