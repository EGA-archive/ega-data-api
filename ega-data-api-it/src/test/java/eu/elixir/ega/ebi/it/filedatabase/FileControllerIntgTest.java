package eu.elixir.ega.ebi.it.filedatabase;

import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import com.jayway.restassured.response.Response;

/**
 * Class to do the integration test for FileController in ega-data-api-key.
 * 
 * @author amohan
 *
 */
public class FileControllerIntgTest extends FileDatabaseBase {

    private final static String FILE_CONTROLLER = "/file/";    
    
    /**
     * Verify the api call /file/{fileId} and check status is {@link org.apache.http.HttpStatus#SC_OK}.
     * Also checks the response fileId.
     */
    @Test
    public void testGetFile() {
        final Response response = REQUEST.get(FILE_CONTROLLER + fileId);
        JSONArray jsonArray = new JSONArray(response.body().asString()); 
        JSONObject jsonObject = jsonArray.getJSONObject(0);
        
        response.then().assertThat().statusCode(SC_OK);
        assertThat(jsonArray.length(), equalTo(1));
        assertThat(jsonObject.getString("fileId"), equalTo(fileId));
    }
    
    /**
     * Verify the api call /file/{fileId} and check status is {@link org.apache.http.HttpStatus#SC_OK}.
     * Also passing wrong Id 000 and expecting the response body to be empty array i.e, []
     */
    @Test
    public void testGetFileForZeroId() {
        final Response response = REQUEST.get(FILE_CONTROLLER + ID_ZERO);
        response.then().assertThat().statusCode(SC_NOT_FOUND);
    }
    
    
    /**
     * Verify the api call /file/{fileId}/datasets and check status is {@link org.apache.http.HttpStatus#SC_OK}.
     * Also checks the response fileId & datasetId.
     */
    @Test
    public void testGetFileDataset() {
        final Response response = REQUEST.get(FILE_CONTROLLER + fileId + "/datasets");
        JSONArray jsonArray = new JSONArray(response.body().asString()); 
        JSONObject jsonObject = jsonArray.getJSONObject(0);
        
        response.then().assertThat().statusCode(SC_OK);
        assertThat(jsonArray.length(), equalTo(1));
        assertThat(jsonObject.getString("fileId"), equalTo(fileId));
        assertThat(jsonObject.getString("datasetId"), equalTo(datasetId));
    }
    
    /**
     * Verify the api call /file/{fileId}/datasets and check status is {@link org.apache.http.HttpStatus#SC_OK}.
     * Also passing wrong Id 000 and expecting the response body to be empty array i.e, []
     */
    @Test
    public void testGetFileDatasetForZeroId() {
        final Response response = REQUEST.get(FILE_CONTROLLER + ID_ZERO + "/datasets");
        JSONArray jsonArray = new JSONArray(response.body().asString()); 
        
        response.then().assertThat().statusCode(SC_OK);
        assertThat(jsonArray.length(), equalTo(0));
    }
    
}
