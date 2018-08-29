/*
 * Copyright 2016 ELIXIR EBI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.elixir.ega.ebi.reencryptionmvc.service.internal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit4.SpringRunner;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Test class for {@link LocalEgaServiceImpl}.
 *
 * @author amohan
 */
@RunWith(SpringRunner.class)
public class LocalEgaServiceImplTest {

    @InjectMocks
    private LocalEgaServiceImpl localEgaServiceImpl;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    /**
     * Test method for
     * {@link LocalEgaServiceImpl#transfer(String, String, String, String, String, String, String, long, long, long, String, String, HttpServletRequest, HttpServletResponse)}.
     */
    @Test
    public void testTransfer() {
        try {
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            localEgaServiceImpl.transfer("aes256", "VTiGzT1YupWn993fvzBHWFJVaH6niANmBUyD+4r01ho=", "An3Ltvagx9nbK5lZi8XD9w==", "plain", null, null, getClass().getResource("/data.enc").getFile(), 42, 78, 0, null, "id", request, response);
            int status = response.getStatus();
            assertEquals(200, status);
            assertEquals("| Unencrypted | Uncompressed | MD5 |", response.getContentAsString());
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("Should not have thrown an exception");
        }
    }

}
