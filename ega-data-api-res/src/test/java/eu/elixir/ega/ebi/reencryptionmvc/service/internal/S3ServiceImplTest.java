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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class S3ServiceImplTest {

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }
    
    /**
     * Test method for {@link S3ServiceImpl#S3ServiceImpl(String, String, String, String)}.
     * @throws MalformedURLException 
     * @throws URISyntaxException 
     */
    @Test
    public void testGetS3ObjectUrl() throws MalformedURLException, URISyntaxException {
        final S3ServiceImpl s3ServiceImpl = new S3ServiceImpl("awsAccessKeyId", "awsSecretAccessKey", "awsEndpointUrl", "awsRegion");
        List<NameValuePair> params = URLEncodedUtils.parse(new URI(s3ServiceImpl.getS3ObjectUrl("s3://test/bucket")), Charset.forName("UTF-8"));
        for(NameValuePair param :params ) {
            if(param.getName().equals("X-Amz-Expires")) {
                assertThat(Long.valueOf(param.getValue()), equalTo(86399L));
            }
        }
    }
    
}
