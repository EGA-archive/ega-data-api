package eu.elixir.ega.ebi.it.dataedge;

import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Test;

import com.jayway.restassured.response.Response;

/**
 * Class to do the integration test for MetadataController in
 * ega-data-api-dataedge.
 * 
 * @author amohan
 *
 */
public class MetadataControllerIntgTest extends DataedgeBase {

    private final static String METADATA_PATH = "/metadata";
    private final static String METADATA_DATASET_PATH = METADATA_PATH + "/datasets/";

    /**
     * Verify the api call /metadata/datasets and check status is
     * {@link org.apache.http.HttpStatus#SC_OK}. Also compares response body
     * datasetId.
     */
    @Test
    public void testGetList() {
        final Response response = REQUEST.get(METADATA_DATASET_PATH);

        response.then().assertThat().statusCode(SC_OK);
        final List<String> data = response.body().jsonPath().getList("");
        assertThat(data.get(0), equalTo(datasetId));
    }
   
    /**
     * Verify the api call /metadata/datasets/{id}/files and check status is
     * {@link org.apache.http.HttpStatus#SC_OK}. Also compares response body
     * datasetId.
     */
    @Test
    public void testGetDatasetFiles() {
        REQUEST.get(METADATA_DATASET_PATH + datasetId + "/files").then().assertThat().statusCode(SC_OK).and()
                .body("datasetId", everyItem(equalTo(datasetId)));
    }
    
    /**
     * Verify the api call /metadata/datasets/{id}/files and check status is
     * {@link org.apache.http.HttpStatus#SC_OK}. Also compares response body is empty.
     */
    @Test
    public void testGetDatasetFilesForZeroDatasetId() {
        final Response response = REQUEST_ZERO_DATASETID.get(METADATA_DATASET_PATH + datasetId + "/files");
        response.then().assertThat().statusCode(SC_FORBIDDEN);
    }

    /**
     * Verify the api call /metadata/files/{fileId} and check status is
     * {@link org.apache.http.HttpStatus#SC_OK}. Also compares response body
     * datasetId & fileId.
     */
    @Test
    public void testGetFile() {
        REQUEST.get(METADATA_PATH + "/files/" + fileId).then().assertThat().statusCode(SC_OK).and()
                .body("fileId", equalTo(fileId)).body("datasetId", equalTo(datasetId));

    }
    
    /**
     * Verify the api call /metadata/files/{fileId} and check status is
     * {@link org.apache.http.HttpStatus#SC_OK}. Also compares response body
     * datasetId & fileId should be null.
     */
    @Test
    public void testGetFileForZeroDatasetId() {
        REQUEST_ZERO_DATASETID.get(METADATA_PATH + "/files/" + fileId).then().assertThat().statusCode(SC_OK).and()
                .body("fileId", equalTo(null)).body("datasetId", equalTo(null));

    }
}
