package uk.gov.hmcts.reform.sscs.ccd.presubmit.createbundle;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.DWP_DOCUMENT_EVIDENCE_FILENAME_PREFIX;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.DWP_DOCUMENT_RESPONSE_FILENAME_PREFIX;

import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.service.BundleRequestExecutor;

@RunWith(JUnitParamsRunner.class)
public class CreateBundleAboutToStartTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private CreateBundleAboutToStartHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private BundleRequestExecutor bundleRequestExecutor;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        initMocks(this);
        handler = new CreateBundleAboutToStartHandler(bundleRequestExecutor);

        when(callback.getEvent()).thenReturn(EventType.CREATE_BUNDLE);

        sscsCaseData = SscsCaseData.builder().createdInGapsFrom("readyToList").appeal(Appeal.builder().mrnDetails(MrnDetails.builder().dwpIssuingOffice("3").build()).build()).build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(bundleRequestExecutor.post(any(), any())).thenReturn(new PreSubmitCallbackResponse<>(sscsCaseData));
    }

    @Test
    public void givenAValidEvent_thenReturnTrue() {
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenANonCreateBundleEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenDwpResponseDocumentHasEmptyFileName_thenPopulateFileName() {
        callback.getCaseDetails().getCaseData().setDwpResponseDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().build()).build());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(DWP_DOCUMENT_RESPONSE_FILENAME_PREFIX, response.getData().getDwpResponseDocument().getDocumentFileName());
    }

    @Test
    public void givenDwpEvidenceDocumentHasEmptyFileName_thenPopulateFileName() {
        callback.getCaseDetails().getCaseData().setDwpEvidenceBundleDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().build()).build());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(DWP_DOCUMENT_EVIDENCE_FILENAME_PREFIX, response.getData().getDwpEvidenceBundleDocument().getDocumentFileName());
    }
}