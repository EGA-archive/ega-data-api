/*
 *
 * Copyright 2020 EMBL - European Bioinformatics Institute
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
 *
 */
package eu.elixir.ega.ebi.dataedge.utils;

import okhttp3.OkHttpClient;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.mockserver.mock.action.ExpectationResponseCallback;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.verify.VerificationTimes;
import org.springframework.http.HttpRange;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import static org.mockserver.model.HttpRequest.request;

public class SimpleSeekableStreamTest {
    @Rule
    public MockServerRule mockServerRule = new MockServerRule(this, 1337);

    private MockServerClient mockServerClient;
    public static final Long RESOURCE_SIZE = 10000L;

    private final OkHttpClient client = new OkHttpClient();

    @Before
    public void setupMockResource() {
        mockServerClient.reset();
        mockServerClient.when(request("/test-data")).respond(new ExpectationResponseCallback() {
            @Override
            public HttpResponse handle(HttpRequest httpRequest) {
                if (httpRequest.getMethod().equals("HEAD")) {
                    return HttpResponse.response().withHeader(HttpHeaders.CONTENT_LENGTH, RESOURCE_SIZE.toString());
                }

                String rangeHeader = httpRequest.getFirstHeader(HttpHeaders.RANGE);
                List<HttpRange> ranges = HttpRange.parseRanges(rangeHeader);
                Assert.assertEquals(1, ranges.size());

                HttpRange range = ranges.get(0);
                long rangeStart = range.getRangeStart(RESOURCE_SIZE);
                long rangeEnd = range.getRangeEnd(RESOURCE_SIZE);
                byte[] body = new byte[(int) (rangeEnd - rangeStart + 1)];
                for (int i = 0; i < body.length; i++) {
                    body[i] = (byte) ((i + rangeStart) % 0xff);
                }

                HttpResponse response = HttpResponse.response().withBody(body);
                return response;
            }
        });
    }

    @Test
    public void downloadHttpUrlInChunks() throws URISyntaxException, IOException {
        // Arrange
        URL uri = new URL("http://localhost:" + mockServerRule.getPort() + "/test-data");

        // Act
        SimpleSeekableStream stream = new SimpleSeekableStream(uri, client, 1234);
        byte[] wholeFile = IOUtils.toByteArray(stream);

        // Assert
        Assert.assertEquals(RESOURCE_SIZE.longValue(), stream.length());
        Assert.assertEquals(RESOURCE_SIZE.longValue(), stream.position());
        Assert.assertTrue(stream.eof());
        Assert.assertEquals(RESOURCE_SIZE.longValue(), wholeFile.length);

        for (int i = 0; i < wholeFile.length; i++) {
            Assert.assertEquals(i % 0xff, ((int) wholeFile[i]) & 0xff);
        }

        mockServerClient.verify(request().withPath("/test-data").withMethod("GET"), VerificationTimes.atLeast(2));
    }

    @Test
    public void canReadSingleByte() throws IOException {
        // Arrange
        URL uri = new URL("http://localhost:" + mockServerRule.getPort() + "/test-data");

        // Act
        SimpleSeekableStream stream = new SimpleSeekableStream(uri, client, 100);

        // Assert
        Assert.assertEquals(0, stream.read());
        Assert.assertEquals(1, stream.read());
        Assert.assertEquals(2, stream.read());
    }

    @Test
    public void seekReadsDataFromExpectedPosition() throws IOException {
        // Arrange
        URL uri = new URL("http://localhost:" + mockServerRule.getPort() + "/test-data");

        // Act
        SimpleSeekableStream stream = new SimpleSeekableStream(uri, client, 100);
        stream.seek(90);

        byte[] buffer = new byte[20];
        stream.read(buffer, 0, 20);

        // Assert
        for (int i = 0; i < 20; ++i) {
            Assert.assertEquals(i + 90, buffer[i]);
        }
    }

    @Test(expected = IOException.class)
    public void seekToInvalidPositionThrowsException() throws IOException {
        // Arrange
        URL uri = new URL("http://localhost:" + mockServerRule.getPort() + "/test-data");

        // Act
        SimpleSeekableStream stream = new SimpleSeekableStream(uri, client, 1234);
        stream.seek(stream.length() + 100);
    }

    @Test
    public void getSourceReturnsURL() throws IOException {
        // Arrange
        URL uri = new URL("http://localhost:" + mockServerRule.getPort() + "/test-data");

        // Act
        SimpleSeekableStream stream = new SimpleSeekableStream(uri, client, 1234);

        // Assert
        Assert.assertEquals(uri.toString(), stream.getSource());
    }
}
