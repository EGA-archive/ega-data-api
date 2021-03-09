package eu.elixir.ega.ebi.commons.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Shared constants for the dataedge service
 */
@Component
public class Constants {

    public static String FILEDATABASE_SERVICE;

    public static String KEYS_SERVICE;

    public static String DATA_SERVICE;

    @Value("${ega.internal.filedatabase.url}")
    public void setFileDatabaseService(String filedburl) {
        FILEDATABASE_SERVICE = filedburl;
    }

    @Value("${ega.internal.key.url}")
    public void setKeyService(String keyurl) {
        KEYS_SERVICE = keyurl;
    }

    @Value("${ega.internal.data.url}")
    public void setDataService(final String dataServiceURL) {
        DATA_SERVICE = dataServiceURL;
    }
}
