package eu.elixir.ega.ebi.htsget.rest;

import eu.elixir.ega.ebi.commons.config.InvalidAuthenticationException;
import eu.elixir.ega.ebi.commons.config.InvalidInputException;
import eu.elixir.ega.ebi.commons.config.InvalidRangeException;
import eu.elixir.ega.ebi.commons.config.UnsupportedFormatException;
import eu.elixir.ega.ebi.commons.exception.NotFoundException;
import eu.elixir.ega.ebi.commons.exception.PermissionDeniedException;
import eu.elixir.ega.ebi.htsget.dto.HtsgetResponseV2;
import eu.elixir.ega.ebi.htsget.dto.HtsgetUrlV2;
import eu.elixir.ega.ebi.htsget.service.TicketServiceV2;
import org.apache.http.HttpHeaders;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.openapi4j.core.exception.ResolutionException;
import org.openapi4j.core.validation.ValidationException;
import org.openapi4j.operation.validator.model.impl.Body;
import org.openapi4j.operation.validator.model.impl.DefaultResponse;
import org.openapi4j.operation.validator.validation.OperationValidator;
import org.openapi4j.parser.OpenApi3Parser;
import org.openapi4j.parser.model.v3.OpenApi3;
import org.openapi4j.parser.model.v3.Operation;
import org.openapi4j.parser.model.v3.Path;
import org.openapi4j.schema.validator.ValidationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = TicketControllerV2.class, secure = false)
public class TicketControllerV2Test {

    private static OpenApi3 apiSpec;
    @Autowired
    private MockMvc mvc;
    @MockBean
    private TicketServiceV2 service;

    @BeforeClass
    public static void loadApiSpec() throws ResolutionException, ValidationException {
        URL spec = Thread.currentThread().getContextClassLoader().getResource("htsget-openapi.yml");
        apiSpec = new OpenApi3Parser().parse(spec, false);
    }

    @Before
    public void resetMock() {
        Mockito.reset(service);
    }

    private ResultMatcher conformsToOperation(String operationId) {
        return mvcResult -> {
            // Put all of the headers into a map
            Map<String, Collection<String>> headers = new HashMap<>();
            for (String headerName : mvcResult.getResponse().getHeaderNames()) {
                headers.put(headerName, mvcResult.getResponse().getHeaders(headerName));
            }

            // Make an OpenAPI4J response object out of the mvcResult response
            DefaultResponse response = new DefaultResponse.Builder(mvcResult.getResponse().getStatus())
                    .headers(headers)
                    .body(Body.from(mvcResult.getResponse().getContentAsString()))
                    .build();

            // Make a validator for this operation
            Operation operation = apiSpec.getOperationById(operationId);
            Path path = apiSpec.getPathItemByOperationId(operationId);
            OperationValidator operationValidator = new OperationValidator(apiSpec, path, operation);

            // Do the validation, collect errors into validationData object
            ValidationData<Object> validationData = new ValidationData<>();
            operationValidator.validateHeaders(response, validationData);
            operationValidator.validateBody(response, validationData);

            // If there were validation errors, throw an exception to fail the test
            if (!validationData.isValid()) {
                throw new Exception(validationData.results().toString());
            }
        };
    }

    @Test
    public void canGetVersion() throws Exception {
        mvc.perform(get("/htsget/version"))
                .andExpect(status().isOk())
                .andExpect(content().string("v1.0.0"));
    }

    @Test
    public void canGetRead() throws Exception {
        Mockito.when(service.getRead(anyString(), anyString(), any(), any(), any(), any(), any(), any(), any())).thenReturn(new HtsgetResponseV2("BAM"));
        mvc.perform(get("/htsget/reads/1")
                .header(HttpHeaders.AUTHORIZATION, "dummy"))
                .andExpect(status().isOk())
                .andExpect(conformsToOperation("searchReadId"));
    }

    @Test
    public void canGetVariant() throws Exception {
        Mockito.when(service.getVariant(anyString(), anyString(), any(), any(), any(), any(), any(), any(), any())).thenReturn(new HtsgetResponseV2("VCF"));
        mvc.perform(get("/htsget/variants/1")
                .header(HttpHeaders.AUTHORIZATION, "dummy"))
                .andExpect(status().isOk())
                .andExpect(conformsToOperation("searchVariantId"));
    }

    @Test
    public void unknownFormatReturnsUnsupportedFormatError() throws Exception {
        Mockito.when(service.getRead(anyString(), anyString(), any(), any(), any(), any(), any(), any(), any())).thenThrow(new UnsupportedFormatException("sushi"));
        mvc.perform(get("/htsget/reads/1?format=sushi")
                .header(HttpHeaders.AUTHORIZATION, "dummy"))
                .andExpect(status().is(400))
                .andExpect(conformsToOperation("searchReadId"))
                .andExpect(content().json("{\n" +
                        "   \"htsget\" : {\n" +
                        "      \"error\": \"UnsupportedFormat\",\n" +
                        "      \"message\": \"Unsupported Format : sushi\"\n" +
                        "   }\n" +
                        "}"));
    }

    @Test
    public void wrongInputReturnsInvalidInput() throws Exception {
        Mockito.when(service.getRead(anyString(), anyString(), any(), any(), any(), any(), any(), any(), any())).thenThrow(new InvalidInputException("sushi"));
        mvc.perform(get("/htsget/reads/1?format=sushi")
                .header(HttpHeaders.AUTHORIZATION, "dummy"))
                .andExpect(status().is(400))
                .andExpect(conformsToOperation("searchReadId"))
                .andExpect(content().json("{\n" +
                        "   \"htsget\" : {\n" +
                        "      \"error\": \"InvalidInput\",\n" +
                        "      \"message\": \"Invalid Input : sushi\"\n" +
                        "   }\n" +
                        "}"));
    }

    @Test
    public void wrongRangeReturnsInvalidRange() throws Exception {
        Mockito.when(service.getRead(anyString(), anyString(), any(), any(), any(), any(), any(), any(), any())).thenThrow(new InvalidRangeException("sushi"));
        mvc.perform(get("/htsget/reads/1?format=sushi")
                .header(HttpHeaders.AUTHORIZATION, "dummy"))
                .andExpect(status().is(400))
                .andExpect(conformsToOperation("searchReadId"))
                .andExpect(content().json("{\n" +
                        "   \"htsget\" : {\n" +
                        "      \"error\": \"InvalidRange\",\n" +
                        "      \"message\": \"Invalid Range : sushi\"\n" +
                        "   }\n" +
                        "}"));
    }

    @Test
    public void missingCredentialsReturnInvalidAuthentication() throws Exception {
        Mockito.when(service.getRead(anyString(), anyString(), any(), any(), any(), any(), any(), any(), any())).thenThrow(new InvalidAuthenticationException("sushi"));
        mvc.perform(get("/htsget/reads/1?format=sushi"))
                .andExpect(status().is(401))
                .andExpect(content().json("{\n" +
                        "   \"htsget\" : {\n" +
                        "      \"error\": \"InvalidAuthentication\",\n" +
                        "      \"message\": \"Request missing Authorization header\"\n" +
                        "   }\n" +
                        "}"));

    }

    @Test
    public void wrongCredentialsReturnPermissionDenied() throws Exception {
        Mockito.when(service.getRead(anyString(), anyString(), any(), any(), any(), any(), any(), any(), any())).thenThrow(new PermissionDeniedException("sushi"));
        mvc.perform(get("/htsget/reads/1?format=sushi")
                .header(HttpHeaders.AUTHORIZATION, "invalid"))
                .andExpect(status().is(403))
                .andExpect(content().json("{\n" +
                        "   \"htsget\" : {\n" +
                        "      \"error\": \"PermissionDenied\",\n" +
                        "      \"message\": \"Permission Denied : sushi\"\n" +
                        "   }\n" +
                        "}"));

    }

    @Test
    public void requestedResourceNotFoundReturnsNotFound() throws Exception {
        Mockito.when(service.getRead(anyString(), anyString(), any(), any(), any(), any(), any(), any(), any())).thenThrow(new NotFoundException("Not Found", "sushi"));
        mvc.perform(get("/htsget/reads/1?format=sushi")
                .header(HttpHeaders.AUTHORIZATION, "dummy"))
                .andExpect(status().is(404))
                .andExpect(content().json("{\n" +
                        "   \"htsget\" : {\n" +
                        "      \"error\": \"NotFound\",\n" +
                        "      \"message\": \"Not Found: sushi\"\n" +
                        "   }\n" +
                        "}"));

    }

    @Test
    public void requestWithOriginHeaderPropagatesHeaderToResponse() throws Exception {
        mvc.perform(get("/htsget/reads/1").header("Origin", "sushi.com"))
                .andExpect(header().string("Access-Control-Allow-Origin", "sushi.com"));
    }

    @Test
    public void preflightRequestPropagatesOriginAndHeadersToResponse() throws Exception {
        mvc.perform(options("/htsget/reads/1").header("Origin", "sushi")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "mochi, nigiri, takoyaki"))
                .andExpect(header().string("Access-Control-Allow-Origin", "sushi"))
                .andExpect(header().string("Access-Control-Allow-Headers", "mochi, nigiri, takoyaki"));
    }

    @Test
    public void preflightRequestResponseHas30DayMaxAge() throws Exception {
        mvc.perform(options("/htsget/reads/1").header("Origin", "sushi")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "mochi, nigiri, takoyaki"))
                .andExpect(header().string("Access-Control-Max-Age", String.valueOf(30 * 24 * 60 * 60)));
    }

    @Test
    public void authoriationHeaderIsCopiedToDataEdgeRequests() throws Exception {
        HtsgetResponseV2 response = new HtsgetResponseV2("BAM");
        response.addUrl(new HtsgetUrlV2(URI.create("http://test.uri")));
        Mockito.when(service.getRead(anyString(), anyString(), any(), any(), any(), any(), any(), any(), any())).thenReturn(response);
        mvc.perform(get("/htsget/reads/1")
                .header(HttpHeaders.AUTHORIZATION, "sushi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.htsget.urls[*].headers.Authorization").value("sushi"));
    }


}
