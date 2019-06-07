package eu.elixir.ega.ebi.it.key;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.specification.RequestSpecification;

public class KeyBase {
    RequestSpecification REQUEST;
    String fileId;
    String keyId;

    public KeyBase() {

        if (System.getProperty("key.url") == null) {
            throw new IllegalArgumentException("Key service host url is null. Pls check configuration in pom.xml.");
        } else if (System.getProperty("fileId") == null) {
            throw new IllegalArgumentException("FileId value is null. Pls check configuration in pom.xml.");
        } else if (System.getProperty("keyId") == null) {
            throw new IllegalArgumentException("KeyId value is null. Pls check configuration in pom.xml.");
        } else if (System.getProperty("key.port") == null) {
            throw new IllegalArgumentException("Port value is null. Pls check configuration in pom.xml.");
        }

        RestAssured.baseURI = System.getProperty("key.url");
        RestAssured.port = Integer.parseInt(System.getProperty("key.port"));
        fileId = System.getProperty("fileId");
        keyId = System.getProperty("keyId");
        RestAssured.useRelaxedHTTPSValidation();

        REQUEST = RestAssured.given().contentType(ContentType.JSON);
    }

}
