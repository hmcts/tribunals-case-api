package uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.ScannedDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.ScannedDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;

@RunWith(JUnitParamsRunner.class)
public class HandleEvidenceEventHandlerTest {

    private HandleEvidenceEventHandler handleEvidenceEventHandler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    private List<ScannedDocument> scannedDocumentList = new ArrayList<>();

    @Before
    public void setUp() {
        initMocks(this);
        handleEvidenceEventHandler = new HandleEvidenceEventHandler();

        when(callback.getEvent()).thenReturn(EventType.ACTION_FURTHER_EVIDENCE);

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder()
                .fileName("bla.pdf")
                .subtype("sscs1")
                .url(DocumentLink.builder().documentUrl("www.test.com").build())
                .scannedDate("2019-06-12T00:00:00.000")
                .controlNumber("123")
                .build()).build();

        ScannedDocument scannedDocument2 = ScannedDocument.builder()
            .value(ScannedDocumentDetails.builder()
                .fileName("bla2.pdf")
                .subtype("sscs2")
                .url(DocumentLink.builder().documentUrl("www.test2.com").build())
                .scannedDate("2019-06-12T00:00:00.000")
                .controlNumber("124")
                .build())
            .build();

        scannedDocumentList.add(scannedDocument);
        scannedDocumentList.add(scannedDocument2);
        DynamicList furtherEvidenceActionList = buildFurtherEvidenceActionItemListForGivenOption("otherDocumentManual",
            "Other document type - action manually");

        DynamicListItem value = new DynamicListItem("appellant", "Appellant (or Appointee)");
        DynamicList originalSender = new DynamicList(value, Collections.singletonList(value));

        sscsCaseData = SscsCaseData.builder()
            .scannedDocuments(scannedDocumentList)
            .furtherEvidenceAction(furtherEvidenceActionList)
            .originalSender(originalSender)
            .build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenAHandleEvidenceEvent_thenReturnTrue() {
        assertTrue(handleEvidenceEventHandler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenANonHandleEvidenceEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertFalse(handleEvidenceEventHandler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters(method = "generateFurtherEvidenceActionListScenarios")
    public void givenACaseWithScannedDocuments_shouldMoveToSscsDocuments(@Nullable DynamicList furtherEvidenceActionList,
                                                                         @Nullable DynamicList originalSender,
                                                                         @Nullable String evidenceHandle,
                                                                         String expectedDocumentType,
                                                                         @Nullable String expectedEvidenceHandled) {
        sscsCaseData.setFurtherEvidenceAction(furtherEvidenceActionList);
        sscsCaseData.setOriginalSender(originalSender);
        sscsCaseData.setEvidenceHandled(evidenceHandle);

        PreSubmitCallbackResponse<SscsCaseData> response = null;
        try {
            response = handleEvidenceEventHandler.handle(ABOUT_TO_SUBMIT, callback);
        } catch (IllegalStateException e) {
            assertTrue(furtherEvidenceActionList == null || originalSender == null);
        }
        if (null != furtherEvidenceActionList && null != originalSender) {
            assertHappyPaths(expectedDocumentType, expectedEvidenceHandled, response);
        }
    }

    private void assertHappyPaths(String expectedDocumentType, String expectedEvidenceHandled,
                                  PreSubmitCallbackResponse<SscsCaseData> response) {
        SscsDocumentDetails sscsDocumentDetail = response.getData().getSscsDocument().get(0).getValue();
        assertEquals("bla.pdf", sscsDocumentDetail.getDocumentFileName());
        assertEquals(expectedDocumentType, sscsDocumentDetail.getDocumentType());
        assertEquals("www.test.com", sscsDocumentDetail.getDocumentLink().getDocumentUrl());
        assertEquals("2019-06-12", sscsDocumentDetail.getDocumentDateAdded());
        assertEquals("123", sscsDocumentDetail.getControlNumber());
        assertEquals("No", response.getData().getSscsDocument().get(0).getValue().getEvidenceIssued());
        assertNull(response.getData().getScannedDocuments());
        assertEquals(expectedEvidenceHandled, response.getData().getEvidenceHandled());
    }

    private Object[] generateFurtherEvidenceActionListScenarios() {
        DynamicList furtherEvidenceActionListOtherDocuments =
            buildFurtherEvidenceActionItemListForGivenOption("otherDocumentManual",
                "Other document type - action manually");

        DynamicList furtherEvidenceActionListInterloc =
            buildFurtherEvidenceActionItemListForGivenOption("informationReceivedForInterloc",
                "Information received for interlocutory review");

        DynamicList furtherEvidenceActionListIssueParties =
            buildFurtherEvidenceActionItemListForGivenOption("issueFurtherEvidence",
                "Issue further evidence to all parties");

        DynamicList appellantOriginalSender = buildOriginalSenderItemListForGivenOption("appellant",
            "Appellant (or Appointee)");
        DynamicList representativeOriginalSender = buildOriginalSenderItemListForGivenOption("representative",
            "Representative");

        return new Object[]{
            //other options scenarios
            new Object[]{furtherEvidenceActionListOtherDocuments, appellantOriginalSender, null, "Other document", null},
            new Object[]{furtherEvidenceActionListOtherDocuments, appellantOriginalSender, "No", "Other document", "No"},
            new Object[]{furtherEvidenceActionListOtherDocuments, representativeOriginalSender, "No", "Other document", "No"},
            new Object[]{furtherEvidenceActionListOtherDocuments, representativeOriginalSender, null, "Other document", null},
            new Object[]{furtherEvidenceActionListOtherDocuments, representativeOriginalSender, "Yes", "Other document", "Yes"},
            new Object[]{furtherEvidenceActionListOtherDocuments, appellantOriginalSender, "Yes", "Other document", "Yes"},
            //issue parties scenarios
            new Object[]{furtherEvidenceActionListIssueParties, appellantOriginalSender, null, "appellantEvidence", "Yes"},
            new Object[]{furtherEvidenceActionListIssueParties, appellantOriginalSender, "No", "appellantEvidence", "Yes"},
            new Object[]{furtherEvidenceActionListIssueParties, appellantOriginalSender, "Yes", "appellantEvidence", "Yes"},
            new Object[]{furtherEvidenceActionListIssueParties, representativeOriginalSender, "No", "representativeEvidence", "Yes"},
            new Object[]{furtherEvidenceActionListIssueParties, representativeOriginalSender, "Yes", "representativeEvidence", "Yes"},
            new Object[]{furtherEvidenceActionListIssueParties, representativeOriginalSender, null, "representativeEvidence", "Yes"},
            //interloc scenarios
            new Object[]{furtherEvidenceActionListInterloc, appellantOriginalSender, null, "appellantEvidence", "Yes"},
            new Object[]{furtherEvidenceActionListInterloc, appellantOriginalSender, "No", "appellantEvidence", "Yes"},
            new Object[]{furtherEvidenceActionListInterloc, appellantOriginalSender, "Yes", "appellantEvidence", "Yes"},
            new Object[]{furtherEvidenceActionListInterloc, representativeOriginalSender, null, "representativeEvidence", "Yes"},
            new Object[]{furtherEvidenceActionListInterloc, representativeOriginalSender, "No", "representativeEvidence", "Yes"},
            new Object[]{furtherEvidenceActionListInterloc, representativeOriginalSender, "Yes", "representativeEvidence", "Yes"},
            //edge cases scenarios
            new Object[]{null, representativeOriginalSender, "", "", null}, //edge case: furtherEvidenceActionOption is null
            new Object[]{furtherEvidenceActionListIssueParties, null, null, "", null} //edge case: originalSender is null
        };
    }

    private DynamicList buildOriginalSenderItemListForGivenOption(String code, String label) {
        DynamicListItem value = new DynamicListItem(code, label);
        return new DynamicList(value, Collections.singletonList(value));
    }

    private DynamicList buildFurtherEvidenceActionItemListForGivenOption(String code, String label) {
        DynamicListItem selectedOption = new DynamicListItem(code, label);
        return new DynamicList(selectedOption,
            Collections.singletonList(selectedOption));
    }

    @Test
    public void givenACaseWithScannedDocumentsAndSscsCaseDocuments_thenAppendNewDocumentsToSscsDocumentsList() {
        List<SscsDocument> sscsDocuments = new ArrayList<>();
        SscsDocument doc = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentType("appellantEvidence")
                .documentFileName("exist.pdf")
                .build())
            .build();
        sscsDocuments.add(doc);

        sscsCaseData.setScannedDocuments(scannedDocumentList);
        sscsCaseData.setSscsDocument(sscsDocuments);

        PreSubmitCallbackResponse<SscsCaseData> response = handleEvidenceEventHandler.handle(ABOUT_TO_SUBMIT, callback);

        assertEquals("exist.pdf", response.getData().getSscsDocument().get(0).getValue().getDocumentFileName());
        assertEquals("bla.pdf", response.getData().getSscsDocument().get(1).getValue().getDocumentFileName());
        assertNull(response.getData().getScannedDocuments());
    }

    @Test
    public void givenACaseWithNoScannedDocuments_thenAddAnErrorToResponse() {
        sscsCaseData.setScannedDocuments(null);

        PreSubmitCallbackResponse<SscsCaseData> response = handleEvidenceEventHandler.handle(ABOUT_TO_SUBMIT, callback);

        assertEquals(1, response.getErrors().size());

        for (String error : response.getErrors()) {
            assertEquals("No further evidence to process", error);
        }
    }

}