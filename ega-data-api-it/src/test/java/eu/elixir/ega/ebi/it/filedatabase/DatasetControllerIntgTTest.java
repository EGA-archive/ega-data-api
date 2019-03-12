package eu.elixir.ega.ebi.it.filedatabase;

import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import com.jayway.restassured.response.Response;

/**
 * Class to do the integration test for DatasetController in ega-data-api-key.
 * 
 * @author amohan
 *
 */
public class DatasetControllerIntgTTest extends FileDatabaseBase {

    private final static String DATASET_CONTROLLER = "/datasets/";    
    
    /**
     * Verify the api call /datasets/{datasetId}/files and check status is {@link org.apache.http.HttpStatus#SC_OK}.
     * Also checks the response fileId & datasetId.
     */
    @Test
    public void testGetDatasetFiles() {
        final Response response = REQUEST.get(DATASET_CONTROLLER + datasetId + "/files");
        JSONArray jsonArray = new JSONArray(response.body().asString()); 
        JSONObject jsonObject = jsonArray.getJSONObject(0);
        
        response.then().assertThat().statusCode(SC_OK);
        assertThat(jsonArray.length(), equalTo(1));
        assertThat(jsonObject.getString("datasetId"), equalTo(datasetId));
        assertThat(jsonObject.getString("fileId"), equalTo(fileId));
    }
    
    /**
     * Verify the api call /datasets/{datasetId}/files and check status is {@link org.apache.http.HttpStatus#SC_OK}.
     * Also passing wrong Id 000 and expecting the response body to be empty array i.e, []
     */
    @Test
    public void testGetDatasetFilesForZeroId() {
        final Response response = REQUEST.get(DATASET_CONTROLLER +  ID_ZERO + "/files");
        JSONArray jsonArray = new JSONArray(response.body().asString()); 

        response.then().assertThat().statusCode(SC_OK);
        assertThat(jsonArray.length(), equalTo(0));
    }
  
}
