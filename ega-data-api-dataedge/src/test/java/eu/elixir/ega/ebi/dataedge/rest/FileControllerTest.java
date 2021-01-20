/*
 * Copyright 2016 ELIXIR EGA
 * Copyright 2016 Alexander Senf
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
package eu.elixir.ega.ebi.dataedge.rest;

import eu.elixir.ega.ebi.commons.exception.PermissionDeniedException;
import eu.elixir.ega.ebi.commons.shared.service.AuthenticationService;
import eu.elixir.ega.ebi.commons.shared.service.PermissionsService;
import eu.elixir.ega.ebi.dataedge.exception.EgaFileNotFoundException;
import eu.elixir.ega.ebi.dataedge.exception.FileNotAvailableException;
import eu.elixir.ega.ebi.dataedge.exception.RangesNotSatisfiableException;
import eu.elixir.ega.ebi.dataedge.exception.UnretrievableFileException;
import eu.elixir.ega.ebi.dataedge.service.NuFileService;
import org.apache.http.HttpHeaders;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import javax.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.PARTIAL_CONTENT;
import static org.springframework.http.HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;

@RunWith(SpringRunner.class)
@WebMvcTest(FileController.class)
@TestPropertySource(locations = "classpath:application-test.properties")
public final class FileControllerTest {

    public static final String TEST_FILE_ID = "EGAF0001";
    public static final String NON_EXISTING_FILE_ID = "EGAF-DOES-NOT-EXIST";
    public static final String NOT_AVAILABLE_FILE_ID = "EGAF-NOT-AVAILABLE";
    public static final String NOT_RETRIEVABLE_FILE_ID = "EGAF-NOT-RETRIEVABLE";

    public static final int TEST_FILE_LENGTH = 100;
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RequestMappingHandlerAdapter requestMappingHandlerAdapter;

    @MockBean
    private AuthenticationService authenticationService;

    @MockBean
    private PermissionsService permissionsService;

    @MockBean
    private NuFileService nuFileService;
    private byte[] testFileContent;
    private ExecutorService executor;

    private void setupFileThrowingException(String fileID, Throwable exception) throws Exception {
        when(nuFileService.getPlainFileSize(eq(fileID)))
                .thenThrow(exception);
        when(nuFileService.getSpecificByteRange(eq(fileID), any(Long.class), any(Long.class)))
                .thenThrow(exception);
    }

    @Before
    public void setUp() throws Exception {
        testFileContent = new byte[TEST_FILE_LENGTH];
        for (int i = 0; i < testFileContent.length; i++) {
            testFileContent[i] = (byte) i;
        }
        when(nuFileService.getPlainFileSize(eq(TEST_FILE_ID)))
                .thenReturn((long) testFileContent.length);
        when(nuFileService.getSpecificByteRange(eq(TEST_FILE_ID), any(Long.class), any(Long.class)))
                .then(invocationOnMock -> {
                    int startByte = invocationOnMock.getArgumentAt(1, Long.class).intValue();
                    int endByte = invocationOnMock.getArgumentAt(2, Long.class).intValue();
                    if (startByte < 0 || endByte >= testFileContent.length || startByte > endByte)
                        throw new RangesNotSatisfiableException(TEST_FILE_ID, startByte, endByte);
                    return new ByteArrayInputStream(testFileContent, startByte, endByte - startByte + 1);
                });

        setupFileThrowingException(NON_EXISTING_FILE_ID, new EgaFileNotFoundException(NON_EXISTING_FILE_ID));
        setupFileThrowingException(NOT_AVAILABLE_FILE_ID, new FileNotAvailableException(NOT_AVAILABLE_FILE_ID));
        setupFileThrowingException(NOT_RETRIEVABLE_FILE_ID, new UnretrievableFileException(NOT_RETRIEVABLE_FILE_ID));

        // Use a dedicated task executor that we can wait to finish
        executor = Executors.newSingleThreadExecutor();
        requestMappingHandlerAdapter.setTaskExecutor(new TaskExecutorAdapter(executor));
    }

    @After
    public void waitForStreamingCompleted() throws InterruptedException {
        // Make sure the streaming result is totally finished
        if (!executor.isShutdown())
            executor.shutdown();
        if (!executor.isTerminated())
            executor.awaitTermination(10, TimeUnit.SECONDS);
    }

    @Test
    public void getWholeFile_ReturnsWholeFile_WithFullContentLengthAndOKStatus() throws Exception {
        // Arrange: Authenticate the user
        setupAuthentication(true);

        // Act: Do the request
        final MockHttpServletResponse response = mockMvc.perform(get("/files/" + TEST_FILE_ID).session(new MockHttpSession()))
                .andReturn().getResponse();

        waitForStreamingCompleted();

        // Assert: Check that the response is OK and has the right size and content
        assertThat(response.getStatus(), equalTo(OK.value()));
        assertThat(response.getContentLengthLong(), equalTo((long) testFileContent.length));
        assertThat(response.getContentType(), equalTo(MediaType.APPLICATION_OCTET_STREAM));
        assertThat(response.getContentAsByteArray(), equalTo(testFileContent));
    }

    @Test
    public void getPartialFile_ReturnsPartialFile_WithPartialContentLengthAndStatus() throws Exception {
        // Arrange: Authenticate the user
        setupAuthentication(true);

        final int startByte = 37;
        final int endByte = 73;

        // Act: Do the request for only a part of the file
        final MockHttpServletResponse response = mockMvc.perform(get("/files/" + TEST_FILE_ID)
                .session(new MockHttpSession())
                .header(HttpHeaders.RANGE, String.format("bytes=%d-%d", startByte, endByte)))
                .andReturn().getResponse();

        waitForStreamingCompleted();

        // Assert: Check that the response is OK and has the right size and content
        assertThat(response.getStatus(), equalTo(PARTIAL_CONTENT.value()));
        assertThat(response.getContentLengthLong(), equalTo((long) (endByte - startByte + 1)));
        assertThat(response.getContentType(), equalTo(MediaType.APPLICATION_OCTET_STREAM));
        assertThat(response.getContentAsByteArray(), equalTo(Arrays.copyOfRange(testFileContent, startByte, endByte + 1)));
    }

    @Test
    public void testGetFileHead() throws Exception {

        // Arrange: Set up the file to return a valid size
        final long fileSize = 1234L;
        when(nuFileService.getPlainFileSize(any(String.class)))
                .thenReturn(fileSize);

        // Act: Do the request
        final MockHttpServletResponse response = mockMvc.perform(head("/files/" + TEST_FILE_ID).session(new MockHttpSession()))
                .andReturn().getResponse();

        // Assert: Check that the response is OK and has the right size and content type
        assertThat(response.getStatus(), equalTo(OK.value()));
        assertThat(response.getContentLengthLong(), equalTo(fileSize));
        assertThat(response.getContentType(), equalTo(MediaType.APPLICATION_OCTET_STREAM));
    }

    @Test
    public void whenNotAuthenticated_ReturnsHttpUnauthorized() throws Exception {
        // Arrange: Set up the user as not authenticated
        setupAuthentication(false);

        // Act: Do the request
        final MockHttpServletResponse response = mockMvc.perform(get("/files/" + TEST_FILE_ID).session(new MockHttpSession()))
                .andReturn().getResponse();

        // Assert: Check that the response is UNAUTHORIZED
        assertThat(response.getStatus(), equalTo(UNAUTHORIZED.value()));
    }

    @Test
    public void whenNotAuthorized_ReturnsHttpForbidden() throws Exception {
        // Arrange: Set up the user as authenticated, but no permission
        setupAuthentication(true);
        when(permissionsService.getFilePermissionsEntity(TEST_FILE_ID)).thenThrow(new PermissionDeniedException(null));

        // Act: Do the request
        final MockHttpServletResponse response = mockMvc.perform(get("/files/" + TEST_FILE_ID).session(new MockHttpSession()))
                .andReturn().getResponse();

        // Assert: Check that the response is UNAUTHORIZED
        assertThat(response.getStatus(), equalTo(FORBIDDEN.value()));
    }

    @Test
    public void whenFileNotFound_ReturnsHttpNotFound() throws Exception {
        // Arrange: Set up the user as not authenticated
        setupAuthentication(true);

        // Act: Do the request
        final MockHttpServletResponse response = mockMvc.perform(get("/files/" + NON_EXISTING_FILE_ID).session(new MockHttpSession()))
                .andReturn().getResponse();

        // Assert: Check that the response is NOT_FOUND
        assertThat(response.getStatus(), equalTo(NOT_FOUND.value()));
    }

    @Test
    public void whenFileNotAvailable_ReturnsHttpNotAvailableForLegalReasons() throws Exception {
        // Arrange: Set up the user as not authenticated
        setupAuthentication(true);

        // Act: Do the request
        final MockHttpServletResponse response = mockMvc.perform(get("/files/" + NOT_AVAILABLE_FILE_ID).session(new MockHttpSession()))
                .andReturn().getResponse();

        // Assert: Check that the response is correct
        assertThat(response.getStatus(), equalTo(UNAVAILABLE_FOR_LEGAL_REASONS.value()));
    }

    @Test
    public void whenFileNotRetrievable_ReturnsHttpNoContent() throws Exception {
        // Arrange: Set up the user as not authenticated
        setupAuthentication(true);

        // Act: Do the request
        final MockHttpServletResponse response = mockMvc.perform(get("/files/" + NOT_RETRIEVABLE_FILE_ID).session(new MockHttpSession()))
                .andReturn().getResponse();

        // Assert: Check that the response is correct
        assertThat(response.getStatus(), equalTo(NO_CONTENT.value()));
    }

    @Test
    public void getInvalidRange_ReturnsRangeNotSatisfiable() throws Exception {
        // Arrange: Authenticate the user
        setupAuthentication(true);

        final int startByte = TEST_FILE_LENGTH - 10;
        final int endByte = TEST_FILE_LENGTH + 10;

        // Act: Do the request for only a part of the file
        final MockHttpServletResponse response = mockMvc.perform(get("/files/" + TEST_FILE_ID)
                .session(new MockHttpSession())
                .header(HttpHeaders.RANGE, String.format("bytes=%d-%d", startByte, endByte)))
                .andReturn().getResponse();

        // Assert: Check that the response is correct
        assertThat(response.getStatus(), equalTo(REQUESTED_RANGE_NOT_SATISFIABLE.value()));
    }

    protected void setupAuthentication(boolean authenticated) {
        Authentication auth = new TestingAuthenticationToken("test-user", "test-credentials");
        auth.setAuthenticated(authenticated);
        when(authenticationService.getAuthentication()).thenReturn(auth);
    }
}
