package eu.elixir.ega.ebi.it.dataedge;

import static eu.elixir.ega.ebi.it.common.Common.getMd5DigestFromResponse;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.Ignore;

import com.jayway.restassured.response.Response;

/**
 * Class to do the integration test for FileController in ega-data-api-dataedge.
 * 
 * @author amohan
 *
 */
public class DataedgeIntgTest extends DataedgeBase {

    private final static String FILE_PATH = "/files/";
    private final static String FILE_ID_ZERO = "000";

    /**
     * Verify the api call /files/{fileId} and check status is
     * {@link org.apache.http.HttpStatus#SC_OK}. Also compares the response body md5.
     * 
     * @throws Exception
     */
    @Test
    @Ignore // disabled because it does not access fire properly
    public void testGetFile() throws Exception {
        final Response response = REQUEST.get(FILE_PATH + fileId + "?destinationFormat=plain");
        response.then().assertThat().statusCode(SC_OK);
        assertTrue(getMd5DigestFromResponse(response).equalsIgnoreCase(unencryptedChecksum));
    }
    
    /**
     * Verify the api call /files/{fileId} passing wrong keyId 000 and expecting
     * the response body status code to be
     * {@link org.apache.http.HttpStatus#SC_FORBIDDEN}.
     * 
     * @throws Exception
     */
    @Test
    public void testGetFileForZeroFileId() throws Exception {
        final Response response = REQUEST.get(FILE_PATH + FILE_ID_ZERO + "?destinationFormat=plain");
        
        response.then().assertThat().statusCode(SC_FORBIDDEN);

        final JSONObject jsonObject = new JSONObject(response.body().asString());
        final int status = jsonObject.getInt("status");
        assertThat(status, equalTo(SC_FORBIDDEN));
    }

    /**
     * Verify the api call /files/{fileId} and check status is
     * {@link org.apache.http.HttpStatus#SC_OK}. Also checks the response header X-Content-Length
     * should be greater than zero.
     * 
     * @throws Exception
     */
    @Test
    public void testGetFileHead() throws Exception {
        final Response response = REQUEST.head(FILE_PATH + fileId + "?destinationFormat=plain");
        response.then().assertThat().statusCode(SC_OK);
        assertTrue(Integer.valueOf(response.getHeader("X-Content-Length")) > 0);
    }

    /**
     * Verify the api call /files/{fileId} and check status is
     * {@link org.apache.http.HttpStatus#SC_OK}. Also checks the response header X-Content-Length
     * should be greater than zero.
     * 
     * @throws Exception
     */
    @Test
    public void testGetFileHeadForZeroFileId() throws Exception {
        final Response response = REQUEST.head(FILE_PATH + FILE_ID_ZERO + "?destinationFormat=plain");
        response.then().assertThat().statusCode(SC_FORBIDDEN);
    }

}
