package eu.elixir.ega.ebi.it.filedatabase;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.specification.RequestSpecification;

public class FileDatabaseBase {

    final static String ID_ZERO = "000";
    RequestSpecification REQUEST;
    String datasetId;
    String fileId;
    String indexId;

    public FileDatabaseBase() {

        if (System.getProperty("file.url") == null) {
            throw new IllegalArgumentException("fileservice host url is null. Pls check configuration in pom.xml.");
        } else if (System.getProperty("indexId") == null) {
            throw new IllegalArgumentException("IndexId value is null. Pls check configuration in pom.xml.");
        } else if (System.getProperty("datasetId") == null) {
            throw new IllegalArgumentException("DatasetId value is null. Pls check configuration in pom.xml.");
        } else if (System.getProperty("fileId") == null) {
            throw new IllegalArgumentException("FileId value is null. Pls check configuration in pom.xml.");
        } else if (System.getProperty("file.port") == null) {
            throw new IllegalArgumentException("Port value is null. Pls check configuration in pom.xml.");
        }
        
        RestAssured.baseURI = System.getProperty("file.url");
        RestAssured.port = Integer.parseInt(System.getProperty("file.port"));
        datasetId = System.getProperty("datasetId");
        fileId = System.getProperty("fileId");
        indexId = System.getProperty("indexId");

        REQUEST = RestAssured.given().contentType(ContentType.JSON);
    }

}
