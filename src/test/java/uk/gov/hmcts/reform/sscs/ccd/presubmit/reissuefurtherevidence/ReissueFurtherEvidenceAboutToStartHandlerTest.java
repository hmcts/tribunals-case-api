package uk.gov.hmcts.reform.sscs.ccd.presubmit.reissuefurtherevidence;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

@RunWith(JUnitParamsRunner.class)
public class ReissueFurtherEvidenceAboutToStartHandlerTest {

    private ReissueFurtherEvidenceAboutToStartHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        initMocks(this);
        handler = new ReissueFurtherEvidenceAboutToStartHandler();

        when(callback.getEvent()).thenReturn(EventType.REISSUE_FURTHER_EVIDENCE);

        SscsDocument document1 = SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentFileName("file1.pdf")
                .documentType("appellantEvidence")
                .documentLink(DocumentLink.builder().documentUrl("url1").build())
                .build()).build();
        SscsDocument document2 = SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentFileName("file2.pdf")
                .documentType("appellantEvidence")
                .documentLink(DocumentLink.builder().documentUrl("url2").build())
                .build()).build();
        List<SscsDocument> sscsDocuments = Arrays.asList(document1, document2);
        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().build())
                .sscsDocument(sscsDocuments)
                .build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    @Parameters({"APPEAL_RECEIVED", "ACTION_FURTHER_EVIDENCE"})
    public void givenANonHandleEvidenceEvent_thenReturnFalse(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        assertFalse(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_START, callback);
    }

    @Test
    @Parameters({"MID_EVENT", "ABOUT_TO_SUBMIT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void populateDocumentDropdownWithAllSscsDocuments() {
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback);

        assertEquals(Collections.EMPTY_SET, response.getErrors());
        assertEquals(2, response.getData().getReissueFurtherEvidenceDocument().getListItems().size());
        assertEquals(new DynamicListItem("url1", "file1.pdf -  appellantEvidence"), response.getData().getReissueFurtherEvidenceDocument().getListItems().get(0));
        assertEquals(new DynamicListItem("url2", "file2.pdf -  appellantEvidence"), response.getData().getReissueFurtherEvidenceDocument().getListItems().get(1));
    }

    @Test
    public void willNotPopulateDocumentDropdownWhenThereAreNoSscsDocuments() {
        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().build()).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback);

        assertNull(response.getData().getReissueFurtherEvidenceDocument());
        assertEquals("There are no documents in the appeal. Cannot reissue further evidence.", response.getErrors().iterator().next());
    }
}
