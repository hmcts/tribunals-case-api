package uk.gov.hmcts.reform.sscs.functional.handlers.sscs1casecreation;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.equalTo;

import io.restassured.http.ContentType;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;


@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:config/application_e2e.properties")
@SpringBootTest
public class Sscs1PdfHandlerTest {
    @Value("${test-url}")
    private String testUrl;
    private IdamTokens idamTokens;
    @Autowired
    private IdamService idamService;

    @Before
    public void setUp() {
        baseURI = testUrl;
        useRelaxedHTTPSValidation();
        idamTokens = idamService.getIdamTokens();
    }

    @Test
    public void hitCallback() throws IOException {

        String body = getJsonCallbackForTest();

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", idamTokens.getIdamOauth2Token())
            .header("ServiceAuthorization", idamTokens.getServiceAuthorization())
            .body(body)
            .expect()
            .statusCode(200)
            .when()
            .post("/ccdAboutToSubmit/")
            .then()
            .statusCode(HttpStatus.OK.value())
            .assertThat().body("data.sscsDocument[0].value.documentType", equalTo("sscs1"));
    }

    private String getJsonCallbackForTest() throws IOException {
        String path = Objects.requireNonNull(getClass().getClassLoader()
                .getResource("handlers/validappeal/validAppealCreatedCallback.json")).getFile();
        return FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());
    }
}
