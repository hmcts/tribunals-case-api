package uk.gov.hmcts.reform.sscs.functional.tya;

import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.RestAssured;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler;


@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:config/application_e2e.properties")
@SpringBootTest
public class GetAppealStatus extends BaseHandler {

    @Value("${test-url}")
    private String testUrl;

    protected CaseDetails<SscsCaseData> caseDetails;

    @Before
    public void setUp() {
        super.setUp();
    }

    @Test
    public void testDwpRespond() throws IOException {
        caseDetails = createCaseInWithDwpStateUsingGivenCallback("handlers/uploaddocument/uploadDocumentCallback.json");

        RestAssured.baseURI = testUrl;
        RestAssured.useRelaxedHTTPSValidation();

        String response = RestAssured
                .given()
                .when()
                .get("appeals?mya=true&caseId=" + caseDetails.getId())
                .then()
                .statusCode(HttpStatus.OK.value())
                .and()
                .extract().body().asString();
        assertThat(response).contains("status\":\"APPEAL_RECEIVED");
    }

    @Test
    public void testResponseReceived() throws IOException {
        caseDetails = createCaseInResponseReceivedState("handlers/uploaddocument/uploadDocumentCallback.json");

        RestAssured.baseURI = testUrl;
        RestAssured.useRelaxedHTTPSValidation();

        String response = RestAssured
                .given()
                .when()
                .get("appeals?mya=true&caseId=" + caseDetails.getId())
                .then()
                .statusCode(HttpStatus.OK.value())
                .and()
                .extract().body().asString();
        assertThat(response).contains("status\":\"DWP_RESPOND");

    }
}
