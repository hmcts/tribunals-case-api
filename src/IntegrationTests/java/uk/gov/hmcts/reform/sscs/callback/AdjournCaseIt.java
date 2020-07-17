package uk.gov.hmcts.reform.sscs.callback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_ADJOURNMENT_NOTICE;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.assertHttpStatus;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.getRequestWithAuthHeader;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.GenerateFileParams;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

@SpringBootTest
@AutoConfigureMockMvc
public class AdjournCaseIt extends AbstractEventIt {

    @MockBean
    private IdamClient idamClient;
    @MockBean
    private GenerateFile generateFile;

    @MockBean
    private UserDetails userDetails;

    @Test
    public void callToMidEventCallback_whenPathIsYesNoYes_willValidateTheData_WhenDueDateInPast() throws Exception {
        setup();
        setJsonAndReplace("callback/adjournCaseYesNoYes.json", "DIRECTIONS_DUE_DATE_PLACEHOLDER", "2019-10-10");

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEvent"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());
        assertEquals("Directions due date must be in the future", result.getErrors().toArray()[0]);
    }

    @Test
    public void callToMidEventCallback_whenPathIsYesNoYes_willValidateTheData_WhenDueDateInFuture() throws Exception {
        setup();
        setJsonAndReplace("callback/adjournCaseYesNoYes.json", "DIRECTIONS_DUE_DATE_PLACEHOLDER", LocalDate.now().plus(1, ChronoUnit.DAYS).toString());

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEvent"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(0, result.getErrors().size());
    }

    @Test
    public void callToMidEventCallback_whenPathIsYesNoNo_willValidateTheNextHearingDate_WhenDateInFuture() throws Exception {
        setup();
        setJsonAndReplace("callback/adjournCaseYesNoNo.json", "NEXT_HEARING_SPECIFIC_DATE_PLACEHOLDER", LocalDate.now().plus(1, ChronoUnit.DAYS).toString());

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEvent"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(0, result.getErrors().size());
    }

    @Test
    public void callToMidEventCallback_whenPathIsYesNoNo_willValidateTheNextHearingDate_WhenDateInPast() throws Exception {
        setup();
        setJsonAndReplace("callback/adjournCaseYesNoNo.json", "NEXT_HEARING_SPECIFIC_DATE_PLACEHOLDER", LocalDate.now().plus(-1, ChronoUnit.DAYS).toString());

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEvent"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());
        assertEquals("Specified date cannot be in the past", result.getErrors().toArray()[0]);
    }

    @Test
    public void callToAboutToSubmitHandler_willWriteAdjournNoticeToCase() throws Exception {
        setup();
        setJsonAndReplace("callback/adjournCaseYesNoYes.json", "DIRECTIONS_DUE_DATE_PLACEHOLDER", "2019-10-10");

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());

        assertNull(result.getData().getOutcome());

        assertEquals(DRAFT_ADJOURNMENT_NOTICE.getValue(), result.getData().getSscsDocument().get(0).getValue().getDocumentType());
        assertEquals(LocalDate.now().toString(), result.getData().getSscsDocument().get(0).getValue().getDocumentDateAdded());
        assertEquals("Draft Adjournment Notice generated on " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")) + ".pdf", result.getData().getSscsDocument().get(0).getValue().getDocumentFileName());
    }

    @Test
    //FIXME: Might need to improve the data for this test once manual route has been fully implemented
    public void callToAboutToSubmitHandler_willWriteManuallyUploadedAdjournNoticeToCase() throws Exception {
        setup("callback/adjournCaseManuallyGenerated.json");

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdAboutToSubmit"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());

        assertNull(result.getData().getOutcome());

        assertEquals(DRAFT_ADJOURNMENT_NOTICE.getValue(), result.getData().getSscsDocument().get(0).getValue().getDocumentType());
        assertEquals(LocalDate.now().toString(), result.getData().getSscsDocument().get(0).getValue().getDocumentDateAdded());
        assertEquals("Draft Adjournment Notice generated on " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + ".pdf", result.getData().getSscsDocument().get(0).getValue().getDocumentFileName());
    }

    @Test
    public void callToMidEventPreviewAdjournCaseCallback_willPreviewTheDocumentForDescriptorRoute() throws Exception {
        setup();
        setJsonAndReplace("callback/adjournCaseYesNoNoFaceToFace.json", "NEXT_HEARING_SPECIFIC_DATE_PLACEHOLDER", LocalDate.now().plus(-1, ChronoUnit.DAYS).toString());

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewAdjournCase"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(Collections.EMPTY_SET, result.getErrors());

        assertEquals(documentUrl, result.getData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());

        ArgumentCaptor<GenerateFileParams> capture = ArgumentCaptor.forClass(GenerateFileParams.class);
        verify(generateFile).assemble(capture.capture());
        final NoticeIssuedTemplateBody parentPayload = (NoticeIssuedTemplateBody) capture.getValue().getFormPayload();
        final WriteFinalDecisionTemplateBody payload = parentPayload.getWriteFinalDecisionTemplateBody();

        assertEquals("An Test", parentPayload.getAppellantFullName());
        assertEquals("12345656789", parentPayload.getCaseId());
        assertEquals("JT 12 34 56 D", parentPayload.getNino());
        assertEquals("DRAFT ADJOURNMENT NOTICE", parentPayload.getNoticeType());
        assertEquals("Judge Full Name", parentPayload.getUserName());
        assertEquals(LocalDate.parse("2017-07-17"), payload.getHeldOn());
        assertEquals("Chester Magistrate's Court", payload.getHeldAt());
        assertEquals("Judge Full Name, Panel Member 1 and Panel Member 2", payload.getHeldBefore());
        assertEquals(true, payload.isAllowed());
        assertEquals(true, payload.isSetAside());
        assertEquals("2018-09-01", payload.getDateOfDecision());
        assertEquals("An Test",payload.getAppellantName());
        assertEquals("2018-10-10",payload.getStartDate());
        assertEquals("2018-11-10",payload.getEndDate());
        assertEquals(false, payload.isIndefinite());
        assertEquals(true, payload.isDailyLivingIsEntited());
        assertEquals(true, payload.isDailyLivingIsSeverelyLimited());
        assertEquals("enhanced rate", payload.getDailyLivingAwardRate());
        Assert.assertNotNull(payload.getDailyLivingDescriptors());
        assertEquals(2, payload.getDailyLivingDescriptors().size());
        Assert.assertNotNull(payload.getDailyLivingDescriptors().get(0));
        assertEquals(8, payload.getDailyLivingDescriptors().get(0).getActivityAnswerPoints());
        assertEquals("f", payload.getDailyLivingDescriptors().get(0).getActivityAnswerLetter());
        assertEquals("Cannot prepare and cook food.", payload.getDailyLivingDescriptors().get(0).getActivityAnswerValue());
        assertEquals("Preparing food", payload.getDailyLivingDescriptors().get(0).getActivityQuestionValue());
        assertEquals("1", payload.getDailyLivingDescriptors().get(0).getActivityQuestionNumber());
        Assert.assertNotNull(payload.getDailyLivingDescriptors().get(1));
        assertEquals(10, payload.getDailyLivingDescriptors().get(1).getActivityAnswerPoints());
        assertEquals("f", payload.getDailyLivingDescriptors().get(1).getActivityAnswerLetter());
        assertEquals("Cannot convey food and drink to their mouth and needs another person to do so.", payload.getDailyLivingDescriptors().get(1).getActivityAnswerValue());
        assertEquals("Taking nutrition", payload.getDailyLivingDescriptors().get(1).getActivityQuestionValue());
        assertEquals("2", payload.getDailyLivingDescriptors().get(1).getActivityQuestionNumber());
        Assert.assertNotNull(payload.getDailyLivingNumberOfPoints());
        assertEquals(18, payload.getDailyLivingNumberOfPoints().intValue());
        assertEquals(false, payload.isMobilityIsEntited());
        assertEquals(false, payload.isMobilityIsSeverelyLimited());
        Assert.assertNull(payload.getMobilityAwardRate());
        Assert.assertNull(payload.getMobilityDescriptors());
        assertNotNull(payload.getReasonsForDecision());
        assertEquals(1, payload.getReasonsForDecision().size());
        Assert.assertEquals("My reasons for decision", payload.getReasonsForDecision().get(0));
        assertEquals("Something else.", payload.getAnythingElse());
    }

}
