package eu.elixir.ega.ebi.htsget.rest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openapi4j.core.exception.ResolutionException;
import org.openapi4j.core.validation.ValidationException;
import org.openapi4j.core.validation.ValidationResults;
import org.openapi4j.operation.validator.model.impl.Body;
import org.openapi4j.operation.validator.model.impl.DefaultResponse;
import org.openapi4j.operation.validator.validation.OperationValidator;
import org.openapi4j.parser.OpenApi3Parser;
import org.openapi4j.parser.model.v3.OpenApi3;
import org.openapi4j.parser.model.v3.Operation;
import org.openapi4j.parser.model.v3.Path;
import org.openapi4j.schema.validator.ValidationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;


import java.net.URL;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = TicketControllerV2.class)
@AutoConfigureMockMvc(addFilters = false)
public class TicketControllerV2Test {

    @Autowired
    private MockMvc mvc;

    class OpenAPISpecMatcher implements ResultMatcher {
        OpenApi3 api;
        String operationId;

        public OpenAPISpecMatcher(String operationId) throws ResolutionException, ValidationException {
            URL spec = Thread.currentThread().getContextClassLoader().getResource("htsget-openapi.yml");
            api = new OpenApi3Parser().parse(spec, false);
            this.operationId = operationId;
        }

        @Override
        public void match(MvcResult mvcResult) throws Exception {
            Operation operation = api.getOperationById(operationId);
            Path path = api.getPathItemByOperationId(operationId);
            OperationValidator validator = new OperationValidator(api, path, operation);
            Body body = Body.from(mvcResult.getResponse().getContentAsString());
            DefaultResponse.Builder builder = new DefaultResponse.Builder(mvcResult.getResponse().getStatus());
            builder.body(body);
            for (String headerName : mvcResult.getResponse().getHeaderNames()){
                builder.header(headerName, mvcResult.getResponse().getHeaders(headerName));
            }

            DefaultResponse response = builder.build();
            ValidationData<Object> validationData = new ValidationData<>();
            validator.validateBody(response, validationData);
            validator.validateHeaders(response, validationData);
            if (validationData.isValid()) {
                return;
            }

            ValidationResults filteredResults = new ValidationResults();
            for (ValidationResults.ValidationItem item : validationData.results().items()) {
                if (item.code() == 1023) continue;
                filteredResults.add(item);
            }
            if (filteredResults.isValid()) {
                return;
            }

            throw new Exception(filteredResults.toString());

        }
    }

    private ResultMatcher conformsToOperation(String operationId) throws ResolutionException, ValidationException {
        return new OpenAPISpecMatcher(operationId);
    }

    @Test
    public void nameTBD() throws Exception {
        mvc.perform(get("/htsget/reads/1"))
                .andExpect(status().isOk())
                .andExpect(conformsToOperation("searchReadId"));

    }

    @Test
    public void requestingUnknownFormatFails() throws Exception {
        mvc.perform(get("/htsget/reads/1?format=sushi"))
                .andExpect(status().is(400))
                .andExpect(conformsToOperation("searchReadId"))
                .andExpect(content().json("{\n" +
                        "   \"htsget\" : {\n" +
                        "      \"error\": \"UnsupportedFormat\",\n" +
                        "      \"message\": \"Unsupported Format : sushi\"\n" +
                        "   }\n" +
                        "}"));

    }

}
