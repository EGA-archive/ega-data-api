package eu.elixir.ega.ebi.commons.shared.util;

import java.net.URISyntaxException;
import java.util.Optional;

import eu.elixir.ega.ebi.commons.exception.ServerErrorException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.cache.annotation.Cacheable;

import lombok.extern.slf4j.Slf4j;
import uk.ac.ebi.ega.fire.service.IFireService;
import uk.ac.ebi.ega.fire.exceptions.FireServiceException;
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
        return getFireSignedUrl(path.toLowerCase().startsWith("/fire/a/") ? path.substring(16) : path, "").getFileURL();
    }

    @Cacheable(cacheNames = "fireSignedUrl", key = "#root.methodName + #path")
    public FireObject getFireSignedUrl(String path, String sessionId) {
        // Sending Request; 4 re-try attempts
        int reTryCount = 4;
        Optional<FireResponse> fireResponse = Optional.empty();
        do {
            try {
                fireResponse = fireService.findFile(path);

                if (fireResponse.isPresent()) {
                    FireResponse fire = fireResponse.get();
                    final String encodedFirePath;
                    try {
                        encodedFirePath = new URIBuilder()
                                .setPath(path)
                                .build()
                                .getRawPath();
                    } catch (URISyntaxException e) {
                        throw new FireServiceException("Session id: " + sessionId +" Unable to build encoded fire path.", e);
                    }
                    log.info(sessionId + " path=" + encodedFirePath);

                    return new FireObject(fireUrl + PATH_OBJECTS + encodedFirePath, fire.getObjectSize());
                }

            } catch (Throwable th) {
                log.error(sessionId + "FIRE error: " + th.getMessage(), th);
            }
            if (!fireResponse.isPresent()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    log.error(e.getMessage(), e);
                }
            }
        } while (!fireResponse.isPresent() && --reTryCount > 0);

        throw new ServerErrorException("Session id: " + sessionId + " can't not retrieve FireFileURL");
    }


    public String getBase64EncodedCredentials() {
        return base64EncodedCredentials;
    }

}
