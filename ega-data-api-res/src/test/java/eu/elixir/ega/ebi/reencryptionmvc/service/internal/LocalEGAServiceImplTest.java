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

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Test class for {@link LocalEGAServiceImpl}.
 *
 * @author amohan
 */
@RunWith(SpringRunner.class)
public class LocalEGAServiceImplTest {

    @InjectMocks
    private LocalEGAServiceImpl localEgaServiceImpl;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    /**
     * Test method for
     * {@link LocalEGAServiceImpl#transfer(String, String, String, String, String, String, String, long, long, long, String, String, HttpServletRequest, HttpServletResponse)}.
     */
   /* @Test
    public void testTransfer() {
        try {
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            localEgaServiceImpl.transfer("aes256", "ef8485457cd460eeb49f70783fb768199bd71679c600e898b010be30665f45a2", "980f5689552c96b3c81d775d0bd9a817", "plain", null, null, getClass().getResource("/data.enc").getFile(), 6, 11, 0, null, "id", request, response);
            int status = response.getStatus();
            assertEquals(200, status);
            assertEquals("test2", response.getContentAsString());
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("Should not have thrown an exception");
        }
    }*/

    @Test
    public void blabla() {

            String string = "JkhY/Hq5EhcmPzYIBrUGOQ==";


            // Get bytes from string

            byte[] byteArray = Base64.decodeBase64(string.getBytes());


            // Print the decoded array

            System.out.println(Arrays.toString(byteArray));


            // Print the decoded string 

            String decodedString = new String(byteArray);

            System.out.println(string + " = " + decodedString);
        System.out.println(Hex.encodeHex(byteArray));
        }
}
