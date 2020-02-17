package eu.elixir.ega.ebi.reencryptionmvc.util;

import java.util.Optional;

import org.apache.http.client.methods.HttpGet;

import lombok.extern.slf4j.Slf4j;
import uk.ac.ebi.ega.fire.service.IFireService;
import uk.ac.ebi.ega.fire.models.FireResponse;

@Slf4j
public class FireCommons {

    private static final String PATH_OBJECTS = "objects/blob/path/";
    private final String fireUrl;
    private final String base64EncodedCredentials;
    private final IFireService fireService;
    
    public FireCommons(String fireUrl, String base64EncodedCredentials, IFireService fireService) {
        this.fireUrl = fireUrl;
        this.base64EncodedCredentials = base64EncodedCredentials;
        this.fireService = fireService;
    }

    public void addAuthenticationForFireRequest(String httpAuth, String url, HttpGet request) {
        if (httpAuth != null && httpAuth.length() > 0) { // Old: http Auth
            String encoding = java.util.Base64.getEncoder().encodeToString(httpAuth.getBytes());
            String auth = "Basic " + encoding;
            request.addHeader("Authorization", auth);
        } else if (!url.contains("X-Amz")) { // Not an S3 URL - Basic Auth embedded with URL
            request.addHeader("Authorization", "Basic " + base64EncodedCredentials);
        }
    }

    public String getFireObjectUrl(String path) {
        return getFireSignedUrl(path.toLowerCase().startsWith("/fire/a/") ? path.substring(16) : path, "")[0];
    }
    
    public String[] getFireSignedUrl(String path, String sessionId) {
        if (path.equalsIgnoreCase("Virtual File"))
            return new String[] { "Virtual File" };
        
        log.info(sessionId + "path=" + path);

        try {
            String[] result = new String[2];
            result[0] = "";
            result[1] = "";

            // Sending Request; 4 re-try attempts
            int reTryCount = 4;
            Optional<FireResponse> fireResponse = Optional.empty();
            do {
                try {
                    fireResponse = fireService.findFile(path);

                    if (fireResponse.isPresent()) {
                        FireResponse fire = fireResponse.get();
                        result[0] = fireUrl + PATH_OBJECTS + path; 
                        result[1] = String.valueOf(fire.getObjectSize());
                    }

                } catch (Throwable th) {
                    log.error(sessionId + "FIRE error: " + th.getMessage(), th);
                }
                if (!fireResponse.isPresent()) {
                    Thread.sleep(500);
                }
            } while (!fireResponse.isPresent() && --reTryCount > 0);

            return result;
        } catch (Exception e) {
            log.error(sessionId + e.getMessage() + " FIRE error path = " + path, e);
        }
        return null;
    }
}
