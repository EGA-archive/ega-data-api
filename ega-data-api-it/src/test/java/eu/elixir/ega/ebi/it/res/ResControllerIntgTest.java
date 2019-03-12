package eu.elixir.ega.ebi.it.res;

import static eu.elixir.ega.ebi.it.common.Common.getMd5DigestFromResponse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.jayway.restassured.response.Response;

/**
 * Class to do the integration test for FileController in ega-data-api-key.
 * 
 * @author amohan
 *
 */
public class ResControllerIntgTest extends ResBase {

    private final static String FILE_PATH = "/file";
    private final static String FILE_ARCHIVE_PATH = "/file/archive/";

    /**
     * Verify the api call /file/archive/{id} and check status is
     * {@link org.apache.http.HttpStatus#SC_OK}. Also checks the response body
     * should not be null.
     * 
     * @throws Exception
     */
    @Test
    public void testGetArchiveFile() throws Exception {
        final Response response = REQUEST.get(FILE_ARCHIVE_PATH + fileId + "?destinationFormat=plain");
        assertTrue(getMd5DigestFromResponse(response).equalsIgnoreCase(unencryptedChecksum));
    }

    /**
     * Verify the api call /file and compares the response body md5.
     * 
     * @throws Exception
     */
    @Test
    public void testGetFile() throws Exception {
        final Response response = REQUEST.get(FILE_PATH +"?sourceKey="+sourceKey+"&sourceIV="+sourceIV+"&filePath="+filePath+"&destinationFormat=plain");
        assertTrue(getMd5DigestFromResponse(response).equalsIgnoreCase(unencryptedChecksum));
    }
}
