package uk.gov.hmcts.reform.sscs.callback;

import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.OTHER_DOCUMENT_MANUAL;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_JUDGE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_TCW;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.assertHttpStatus;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.createUploadResponse;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.getRequestWithAuthHeader;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.hamcrest.core.StringEndsWith;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.document.domain.Document;
import uk.gov.hmcts.reform.document.domain.UploadResponse;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.idam.Authorize;
import uk.gov.hmcts.reform.sscs.idam.IdamApiClient;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.service.EvidenceManagementService;
import uk.gov.hmcts.reform.sscs.service.FooterService;

@SpringBootTest
@AutoConfigureMockMvc
@RunWith(JUnitParamsRunner.class)
public class CcdCallbackEndpointIt extends AbstractEventIt {

    @Autowired
    private FooterService footerService;

    @MockBean
    private CoreCaseDataApi coreCaseDataApi;

    @MockBean
    private IdamApiClient idamApiClient;

    @MockBean
    private AuthTokenGenerator authTokenGenerator;

    @MockBean
    private EvidenceManagementService evidenceManagementService;

    @MockBean
    UploadResponse uploadResponse;

    @Before
    public void setup() throws IOException {
        setup("callback/actionFurtherEvidenceCallback.json");
    }

    private Document createDocument() {
        Document document = new Document();
        Document.Links links = new Document.Links();
        Document.Link link = new Document.Link();
        link.href = "a location";
        links.self = link;
        document.links = links;
        return document;
    }

    @Test
    @Parameters({"form", "coversheet"})
    public void shouldHandleActionFurtherEvidenceEventCallback(String documentType) throws Exception {
        json = getJson("callback/actionFurtherEvidenceCallback.json");
        json = json.replaceAll("DOCUMENT_TYPE", documentType);

        byte[] pdfBytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdf/sample.pdf"));
        when(evidenceManagementService.download(any(), anyString())).thenReturn(pdfBytes);

        UploadResponse uploadResponse = createUploadResponse();
        when(evidenceManagementService.upload(any(), anyString())).thenReturn(uploadResponse);

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));

        assertHttpStatus(response, HttpStatus.OK);

        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(((MockHttpServletResponse) response).getContentAsString());

        List<SscsDocument> documentList = result.getData().getSscsDocument();
        if (documentType.equalsIgnoreCase("coversheet")) {
            Assert.assertTrue(CollectionUtils.isEmpty(documentList));
            assertNull(result.getData().getScannedDocuments());
        } else {
            assertEquals(1, documentList.size());
            assertNull(result.getData().getScannedDocuments());
            assertEquals("appellantEvidence", documentList.get(0).getValue().getDocumentType());
            assertEquals("3", documentList.get(0).getValue().getControlNumber());
            assertEquals("scanned.pdf", documentList.get(0).getValue().getDocumentFileName());
            assertEquals("some location", documentList.get(0).getValue().getDocumentLink().getDocumentUrl());
        }
    }

    @Test
    public void actionFurtherEvidenceDropdownAboutToStartCallback() throws Exception {
        json = getJson("callback/actionFurtherEvidenceCallback.json");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToStart"));

        assertHttpStatus(response, HttpStatus.OK);

        @SuppressWarnings({"unchecked", "CastCanBeRemovedNarrowingVariableType"})
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(((MockHttpServletResponse) response).getContentAsString());

        assertEquals(3, result.getData().getOriginalSender().getListItems().size());
        assertEquals(3, result.getData().getFurtherEvidenceAction().getListItems().size());
        assertEquals(OTHER_DOCUMENT_MANUAL.getCode(), result.getData().getFurtherEvidenceAction().getListItems().get(0).getCode());
        assertEquals(SEND_TO_INTERLOC_REVIEW_BY_JUDGE.getCode(), result.getData().getFurtherEvidenceAction().getListItems().get(1).getCode());
        assertEquals(SEND_TO_INTERLOC_REVIEW_BY_TCW.getCode(), result.getData().getFurtherEvidenceAction().getListItems().get(2).getCode());
    }

    @Test
    public void givenFurtherEvidenceIssueToAllParties_shouldUpdateDwpFurtherEvidenceState() throws Exception {
        json = getJson("callback/actionFurtherEvidenceWithInterlocOptionCallback.json");
        json = json.replaceFirst("informationReceivedForInterlocJudge", "issueFurtherEvidence");
        json = json.replaceFirst("Information received for interlocutory review", "Issue further evidence to all parties");

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));

        assertHttpStatus(response, HttpStatus.OK);

        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());
        assertNull(result.getData().getInterlocReviewState());
        assertEquals("furtherEvidenceReceived", result.getData().getDwpFurtherEvidenceStates());
    }

    @Test
    public void givenFurtherEvidenceIssueToAllParties_onSubmitted_willStart_IssueFurtherEvidenceEvent() throws Exception {
        mockIdam();
        given(coreCaseDataApi.startEventForCaseWorker(eq("Bearer authToken"), eq("s2s token"),
                eq("userId"), eq("SSCS"), eq("Benefit"), eq("12345656789"),
                eq("issueFurtherEvidence")))
                .willReturn(StartEventResponse.builder().build());

        given(coreCaseDataApi.submitEventForCaseWorker(eq("Bearer authToken"), eq("s2s token"),
                eq("userId"), eq("SSCS"), eq("Benefit"), eq("12345656789"),
                eq(true), any(CaseDataContent.class)))
                .willReturn(CaseDetails.builder()
                        .id(123L)
                        .data(new HashMap<>())
                        .build());

        json = getJson("callback/actionFurtherEvidenceWithInterlocOptionCallback.json");
        json = json.replaceFirst("informationReceivedForInterlocJudge", "issueFurtherEvidence");
        json = json.replaceFirst("Information received for interlocutory review", "Issue further evidence to all parties");


        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdSubmittedEvent"));

        assertHttpStatus(response, HttpStatus.OK);

        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        verify(coreCaseDataApi).startEventForCaseWorker(any(), anyString(), anyString(), anyString(),
                anyString(), eq("12345656789"), eq("issueFurtherEvidence"));
    }

    @Test
    public void givenSubmittedCallbackForActionFurtherEvidence_shouldUpdateFieldAndTriggerEvent() throws Exception {
        mockIdam();
        mockCcd();

        json = getJson("callback/actionFurtherEvidenceWithInterlocOptionCallback.json");

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdSubmittedEvent"));

        assertHttpStatus(response, HttpStatus.OK);

        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());
        assertEquals("interlocutoryReview", result.getData().getInterlocReviewState());
        assertNull(result.getData().getDwpFurtherEvidenceStates());
    }

    private void mockCcd() {
        given(coreCaseDataApi.startEventForCaseWorker(eq("Bearer authToken"), eq("s2s token"),
            eq("userId"), eq("SSCS"), eq("Benefit"), eq("12345656789"),
            eq("interlocInformationReceived")))
            .willReturn(StartEventResponse.builder().build());

        Map<String, Object> data = new HashMap<>();
        data.put("interlocReviewState", "interlocutoryReview");
        given(coreCaseDataApi.submitEventForCaseWorker(eq("Bearer authToken"), eq("s2s token"),
            eq("userId"), eq("SSCS"), eq("Benefit"), eq("12345656789"),
            eq(true), any(CaseDataContent.class)))
            .willReturn(CaseDetails.builder()
                .id(123L)
                .data(data)
                .build());
    }

    private void mockIdam() {
        given(idamApiClient.authorizeCodeType(anyString(), eq("code"), eq("sscs"),
            eq("https://localhost:3000/authenticated"), eq(" ")))
            .willReturn(Authorize.builder().code("code").build());

        given(idamApiClient.authorizeToken(anyString(), eq("authorization_code"),
            eq("https://localhost:3000/authenticated"), eq("sscs"), anyString(), eq(" ")))
            .willReturn(Authorize.builder().accessToken("authToken").build());

        given(idamApiClient.getUserDetails("Bearer authToken"))
            .willReturn(UserDetails.builder().id("userId").build());

        given(authTokenGenerator.generate()).willReturn("s2s token");
    }

    @Test
    public void coversheetFurtherEvidence_shouldNotAddToDocuments() throws Exception {
        json = getJson("callback/actionFurtherEvidenceWithInterlocOptionCallback.json");
        json = json.replaceFirst("informationReceivedForInterlocJudge", "otherDocumentManual");
        json = json.replaceFirst("Information received for interlocutory review", "Other document type - action manually");
        json = json.replaceFirst("appellantEvidence", "coversheet");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));

        assertHttpStatus(response, HttpStatus.OK);

        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(((MockHttpServletResponse) response).getContentAsString());

        assertNull(result.getData().getInterlocReviewState());
        assertNull(result.getData().getScannedDocuments());
        assertNull(result.getData().getSscsDocument());
        assertEquals("Yes", result.getData().getEvidenceHandled());
    }

    @Test
    public void shouldHandleInterlocEventCallback() throws Exception {
        json = getJson("callback/interlocEventCallback.json");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));

        assertHttpStatus(response, HttpStatus.OK);

        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(((MockHttpServletResponse) response).getContentAsString());

        assertEquals("No", result.getData().getLinkedCasesBoolean());
        assertEquals("reviewByTcw", result.getData().getInterlocReviewState());
    }

    @Test
    public void shouldHandleSendToDwpOfflineEventCallback() throws Exception {
        json = getJson("callback/sendToDwpOfflineEventCallback.json");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));

        assertHttpStatus(response, HttpStatus.OK);

        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(((MockHttpServletResponse) response).getContentAsString());

        assertNull(result.getData().getHmctsDwpState());
    }

    @Test
    public void shouldHandleTcwDecisionAppealToProceedEventCallback() throws Exception {
        json = getJson("callback/tcwDecisionAppealToProceedEventCallback.json");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));

        assertHttpStatus(response, HttpStatus.OK);

        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(((MockHttpServletResponse) response).getContentAsString());

        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void shouldHandleJudgeDecisionAppealToProceedEventCallback() throws Exception {
        json = getJson("callback/judgeDecisionAppealToProceedEventCallback.json");

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));

        assertHttpStatus(response, HttpStatus.OK);

        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(((MockHttpServletResponse) response).getContentAsString());

        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    @Parameters({"dormantAppealState", "readyToList","responseReceived"})
    public void shouldAddAppendixToFooterOfPdfOnEventCallback(String state) throws Exception {
        json = getJson("callback/appealDormantCallback.json");
        json = json.replaceAll("dormantAppealState", state);

        ArgumentCaptor<List<MultipartFile>> captor = ArgumentCaptor.forClass(List.class);
        byte[] pdfBytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdf/sample.pdf"));
        when(evidenceManagementService.download(any(), anyString())).thenReturn(pdfBytes);

        UploadResponse.Embedded embedded = mock(UploadResponse.Embedded.class);
        List<Document> documents = Collections.singletonList(createDocument());
        when(embedded.getDocuments()).thenReturn(documents);
        when(uploadResponse.getEmbedded()).thenReturn(embedded);
        when(evidenceManagementService.upload(captor.capture(), anyString())).thenReturn(uploadResponse);

        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));

        assertHttpStatus(response, HttpStatus.OK);

        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(((MockHttpServletResponse) response).getContentAsString());

        assertNull(result.getData().getScannedDocuments());
        assertEquals(1, result.getData().getSscsDocument().size());
        byte[] newBytes = captor.getValue().get(0).getBytes();
        PDDocument newPdf = PDDocument.load(newBytes);
        String text = new PDFTextStripper().getText(newPdf);
        assertThat(text, StringEndsWith.endsWith("Appellant evidence Addition A | Page 1\n"));
    }

    @Test
    @Parameters({"appealCreated", "incompleteApplication", "validAppeal"})
    public void shouldNotAddAppendixToFooterOfPdfOnEventCallback(String state) throws Exception {
        json = getJson("callback/appealDormantCallback.json");
        json = json.replaceAll("dormantAppealState", state);
        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));

        assertHttpStatus(response, HttpStatus.OK);

        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(((MockHttpServletResponse) response).getContentAsString());

        assertNull(result.getData().getScannedDocuments());
        assertEquals(1, result.getData().getSscsDocument().size());
        verifyNoMoreInteractions(evidenceManagementService);
    }

    @Test
    public void shouldNotAddAppendixToFooterOfPdfOnIfNotIssuingFurtherEvidenceToAllPartiesEventCallback() throws Exception {
        json = getJson("callback/appealDormantCallback.json");
        json = json.replaceFirst("issueFurtherEvidence", "otherDocumentManual");
        json = json.replaceFirst("Issue further evidence to all parties", "Other document type - action manually");
        HttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));

        assertHttpStatus(response, HttpStatus.OK);

        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(((MockHttpServletResponse) response).getContentAsString());

        assertNull(result.getData().getScannedDocuments());
        assertEquals(1, result.getData().getSscsDocument().size());
        verifyNoMoreInteractions(evidenceManagementService);
    }

}
