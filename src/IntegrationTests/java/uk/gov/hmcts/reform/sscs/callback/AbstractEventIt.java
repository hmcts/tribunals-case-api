package uk.gov.hmcts.reform.sscs.callback;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackDispatcher;
import uk.gov.hmcts.reform.sscs.controller.CcdCallbackController;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;

@TestPropertySource(locations = "classpath:config/application_it.properties")
public abstract class AbstractEventIt {

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    protected MockMvc mockMvc;

    @MockBean
    protected AuthorisationService authorisationService;

    @Autowired
    protected SscsCaseCallbackDeserializer deserializer;

    @Autowired
    protected PreSubmitCallbackDispatcher dispatcher;

    @Autowired
    protected ObjectMapper mapper;

    String json;

    void setup(String jsonFile) throws IOException {
        CcdCallbackController controller = new CcdCallbackController(authorisationService, deserializer, dispatcher);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        mapper.registerModule(new JavaTimeModule());
        json = getJson(jsonFile);
    }

    protected String getJson(String fileLocation) throws IOException {
        String path = Objects.requireNonNull(getClass().getClassLoader()
                .getResource(fileLocation)).getFile();
        return FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());
    }

    protected MockHttpServletResponse getResponse(MockHttpServletRequestBuilder requestBuilder) throws Exception {
        return mockMvc.perform(requestBuilder).andReturn().getResponse();
    }

    public PreSubmitCallbackResponse deserialize(String source) {
        try {
            return mapper.readValue(
                    source,
                    new TypeReference<PreSubmitCallbackResponse<SscsCaseData>>() {
                    }
            );

        } catch (IOException e) {
            throw new IllegalArgumentException("Could not deserialize object", e);
        }
    }
}
