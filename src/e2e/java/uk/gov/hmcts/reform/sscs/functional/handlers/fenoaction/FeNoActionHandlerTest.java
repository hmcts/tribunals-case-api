package uk.gov.hmcts.reform.sscs.functional.handlers.fenoaction;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.functional.handlers.uploaddocument.BaseHandlerTest;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:config/application_e2e.properties")
@SpringBootTest
public class FeNoActionHandlerTest extends BaseHandlerTest {

    @Test
    public void givenUploadDocumentEventIsTriggered_shouldUploadDocument() throws IOException {
        caseDetails = createCaseInWithDwpStateUsingGivenCallback("feNoActionCallback.json");

        SscsCaseDetails actualCase = ccdService.updateCase(caseDetails.getCaseData(), caseDetails.getId(),
            EventType.FE_NO_ACTION.getCcdType(), CREATED_BY_FUNCTIONAL_TEST, CREATED_BY_FUNCTIONAL_TEST, idamTokens);
        assertEquals(State.WITH_DWP.getId(), actualCase.getState());
        assertEquals("feActionedNR", actualCase.getData().getDwpState());
    }

}

