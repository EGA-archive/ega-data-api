package eu.elixir.ega.ebi.reencryptionmvc.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import org.apache.http.client.methods.HttpGet;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FireCommons {

    private final String fireUrl;
    private final String fireArchive;
    private final String fireKey;
    
    public FireCommons(String fireUrl, String fireArchive, String fireKey) {
        this.fireUrl = fireUrl;
        this.fireArchive = fireArchive;
        this.fireKey = fireKey;
    }

    public void addAuthenticationForFireRequest(String httpAuth, String url, HttpGet request) {
        if (httpAuth != null && httpAuth.length() > 0) { // Old: http Auth
            String encoding = java.util.Base64.getEncoder().encodeToString(httpAuth.getBytes());
            String auth = "Basic " + encoding;
            request.addHeader("Authorization", auth);
        } else if (!url.contains("X-Amz")) { // Not an S3 URL - Basic Auth embedded with URL
            try {
                URL url_ = new URL(url);
                if (url_.getUserInfo() != null) {
                    String encoding = java.util.Base64.getEncoder().encodeToString(url_.getUserInfo().getBytes());
                    String auth = "Basic " + encoding;
                    request.addHeader("Authorization", auth);
                }
            } catch (MalformedURLException ignored) {
            }
        }
    }

    public String getFireObjectUrl(String path) {
        return getFireSignedUrl(path.toLowerCase().startsWith("/fire/a/") ? path.substring(16) : path, "")[0];
    }
    
    public String[] getFireSignedUrl(String path, String sessionId) {
        if (path.equalsIgnoreCase("Virtual File"))
            return new String[] { "Virtual File" };

        try {
            String[] result = new String[4]; // [0] name [1] stable_id [2] size [3] rel path
            result[0] = "";
            result[1] = "";
            result[3] = path;
            String path_ = path;

            // Sending Request; 4 re-try attempts
            int reTryCount = 4;
            int responseCode = 0;
            HttpURLConnection connection = null;
            do {
                try {
                    connection = (HttpURLConnection) (new URL(fireUrl)).openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("X-FIRE-Archive", fireArchive);
                    connection.setRequestProperty("X-FIRE-Key", fireKey);
                    connection.setRequestProperty("X-FIRE-FilePath", path_);

                    // Reading Response - with Retries
                    responseCode = connection.getResponseCode();
                    // System.out.println("Response Code " + responseCode);
                } catch (Throwable th) {
                    log.error(sessionId + "FIRE error: " + th.getMessage(), th);
                }
                if (responseCode != 200) {
                    connection = null;
                    Thread.sleep(500);
                }
            } while (responseCode != 200 && --reTryCount > 0);

            // if Response OK
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;

                ArrayList<String[]> paths = new ArrayList<>();

                String object_get = "", // 1
                        object_head = "", // 2
                        object_md5 = "", // 3
                        object_length = "", // 4
                        object_url_expire = "", // 5
                        object_storage_class = ""; // 6
                while ((inputLine = in.readLine()) != null) {
                    if (inputLine.startsWith("OBJECT_GET"))
                        object_get = inputLine.substring(inputLine.indexOf("http://")).trim();
                    if (inputLine.startsWith("OBJECT_HEAD"))
                        object_head = inputLine.substring(inputLine.indexOf(" ") + 1).trim();
                    if (inputLine.startsWith("OBJECT_MD5"))
                        object_md5 = inputLine.substring(inputLine.indexOf(" ") + 1).trim();
                    if (inputLine.startsWith("OBJECT_LENGTH"))
                        object_length = inputLine.substring(inputLine.indexOf(" ") + 1).trim();
                    if (inputLine.startsWith("OBJECT_URL_EXPIRE"))
                        object_url_expire = inputLine.substring(inputLine.indexOf(" ") + 1).trim();
                    if (inputLine.startsWith("OBJECT_STORAGE_CLASS"))
                        object_storage_class = inputLine.substring(inputLine.indexOf(" ") + 1).trim();
                    if (inputLine.startsWith("END"))
                        paths.add(new String[] { object_get, object_length, object_storage_class });
                }
                in.close();

                if (paths.size() > 0) {
                    for (String[] e : paths) {
                        if (!e[0].toLowerCase().contains("/ota/")) { // filter out tape archive
                            result[0] = e[0]; // GET Url
                            result[1] = e[1]; // Length
                            result[2] = e[2]; // Storage CLass
                            break; // Pick first non-tape entry
                        }
                    }
                }
            }

            return result;
        } catch (Exception e) {
            log.error(sessionId + e.getMessage() + " Path = " + path, e);
        }
        return null;
    }
}
