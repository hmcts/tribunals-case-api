package uk.gov.hmcts.reform.sscs.ccd.presubmit.reissuedocument;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;

import java.util.Arrays;
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
public class ReissueDocumentAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private ReissueDocumentAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;
    private SscsDocument document1;
    private SscsDocument document2;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new ReissueDocumentAboutToSubmitHandler();

        when(callback.getEvent()).thenReturn(EventType.REISSUE_DOCUMENT);

        document1 = SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentFileName("file1.pdf")
                .documentType(DECISION_NOTICE.getValue())
                .evidenceIssued("Yes")
                .documentLink(DocumentLink.builder().documentUrl("url1").build())
                .build()).build();
        document2 = SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentFileName("file2.pdf")
                .documentType(DIRECTION_NOTICE.getValue())
                .evidenceIssued("Yes")
                .documentLink(DocumentLink.builder().documentUrl("url2").build())
                .build()).build();
        SscsDocument document3 = SscsDocument.builder().value(SscsDocumentDetails.builder()
            .documentFileName("file3.pdf")
            .documentType(FINAL_DECISION_NOTICE.getValue())
            .documentLink(DocumentLink.builder().documentUrl("url1").build())
            .build()).build();
        SscsDocument document4 = SscsDocument.builder().value(SscsDocumentDetails.builder()
            .documentFileName("file4.pdf")
            .documentType(ADJOURNMENT_NOTICE.getValue())
            .documentLink(DocumentLink.builder().documentUrl("url1").build())
            .build()).build();
        List<SscsDocument> sscsDocuments = Arrays.asList(document1, document2, document3, document4);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").appeal(Appeal.builder().build())
                .sscsDocument(sscsDocuments)
                .resendToAppellant("YES")
                .resendToDwp("YES")
                .resendToRepresentative("No")
                .reissueFurtherEvidenceDocument(new DynamicList(new DynamicListItem("url2", "file2.pdf - appellantEvidence"), null))
                .build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenANonHandleEvidenceEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void returnsAnErrorIfReissuedToRepresentativeWhenThereIsNoRepOnTheAppealToReissueDocument() {
        sscsCaseData = sscsCaseData.toBuilder().resendToRepresentative("YES").build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        assertEquals("Cannot re-issue to the representative as there is no representative on the appeal", response.getErrors().toArray()[0]);
    }

    @Test
    public void returnsAnErrorIfNoPartySelectedForReissue() {
        sscsCaseData = sscsCaseData.toBuilder().resendToAppellant("No").build();
        sscsCaseData = sscsCaseData.toBuilder().resendToRepresentative("No").build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        assertEquals("No party selected to reissue document", response.getErrors().toArray()[0]);
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }


}
