package uk.gov.hmcts.reform.sscs.callback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.assertHttpStatus;
import static uk.gov.hmcts.reform.sscs.helper.IntegrationTestHelper.getRequestWithAuthHeader;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.UcTemplateContent;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.scenarios.UcScenario;
import uk.gov.hmcts.reform.sscs.model.docassembly.GenerateFileParams;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public class UcWriteFinalDecisionIt extends WriteFinalDecisionItBase {

    @Test
    public void callToMidEventCallback_willValidateTheDate() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorUC.json", "START_DATE_PLACEHOLDER", "2019-10-10");

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEvent"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());
        assertEquals("Decision notice end date must be after decision notice start date", result.getErrors().toArray()[0]);

    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willPreviewTheDocumentForDescriptorRoute() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorUC.json", "START_DATE_PLACEHOLDER", "2018-10-10");

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
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
        assertEquals("DRAFT DECISION NOTICE", parentPayload.getNoticeType());
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
        assertEquals(true, payload.isUcIsEntited());
        assertEquals("lower rate", payload.getUcAwardRate());
        Assert.assertNotNull(payload.getUcSchedule6Descriptors());
        assertEquals(1, payload.getUcSchedule6Descriptors().size());
        assertEquals(15, payload.getUcSchedule6Descriptors().get(0).getActivityAnswerPoints());
        assertEquals("a", payload.getUcSchedule6Descriptors().get(0).getActivityAnswerLetter());
        assertEquals("Cannot, unaided by another person, either: (i) mobilise more than 50 metres on level ground without stopping in order to avoid significant discomfort or exhaustion; or (ii) repeatedly mobilise 50 metres within a reasonable timescale because of significant discomfort or exhaustion.", payload.getUcSchedule6Descriptors().get(0).getActivityAnswerValue());
        assertEquals("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used.", payload.getUcSchedule6Descriptors().get(0).getActivityQuestionValue());
        assertEquals("1", payload.getUcSchedule6Descriptors().get(0).getActivityQuestionNumber());
        Assert.assertNotNull(payload.getUcNumberOfPoints());
        assertEquals(15, payload.getUcNumberOfPoints().intValue());
        assertNotNull(payload.getReasonsForDecision());
        assertEquals(1, payload.getReasonsForDecision().size());
        Assert.assertEquals("My reasons for decision", payload.getReasonsForDecision().get(0));
        assertEquals("Something else.", payload.getAnythingElse());
        assertNotNull(parentPayload.getWriteFinalDecisionTemplateContent());
        assertEquals("The appeal is allowed.\n"
            + "\n"
            + "The decision made by the Secretary of State on 01/09/2018 is set aside.\n"
            + "\n"
            + "An Test has limited capability for work. The matter is now remitted to the Secretary of State to make a final decision upon entitlement to Universal Credit (UC).\n"
            + "\n"
            + "In applying the Work Capability Assessment 15 points were scored from the activities and descriptors in Schedule 6 of the UC Regulations 2013 made up as follows:\n"
            + "\n"
            + "1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used.\ta.Cannot, unaided by another person, either: (i) mobilise more than 50 metres on level ground without stopping in order to avoid significant discomfort or exhaustion; or (ii) repeatedly mobilise 50 metres within a reasonable timescale because of significant discomfort or exhaustion.\t15\n"
            + "\n"
            + "\n"
            + "An Test does not have limited capability for work-related activity because no descriptor from Schedule 7 of the UC Regulations applied. Schedule 9, paragraph 4 did not apply.\n"
            + "\n"
            + "My reasons for decision\n"
            + "\n"
            + "Something else.\n"
            + "\n"
            + "This has been an oral (face to face) hearing. An Test attended the hearing today and the Tribunal considered the appeal bundle to page A1. A Presenting Officer attended on behalf of the Respondent.\n"
            + "\n", parentPayload.getWriteFinalDecisionTemplateContent().toString());
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willPreviewTheDocumentForUc_WhenNonLcwa_WhenQuestionsPreviouslyAnsweredAndOver15Points() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorUCNonLcwa.json", Arrays.asList("START_DATE_PLACEHOLDER", "ALLOWED_OR_REFUSED"), Arrays.asList("2018-10-10", "allowed"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
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
        assertEquals("DRAFT DECISION NOTICE", parentPayload.getNoticeType());
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
        assertEquals(false, payload.isUcIsEntited());
        assertNull(payload.getUcAwardRate());
        Assert.assertNull(payload.getUcSchedule6Descriptors());
        Assert.assertNull(payload.getUcNumberOfPoints());
        assertNotNull(payload.getReasonsForDecision());
        assertEquals(1, payload.getReasonsForDecision().size());
        Assert.assertEquals("My reasons for decision", payload.getReasonsForDecision().get(0));
        assertEquals("Something else.", payload.getAnythingElse());
        assertNotNull(parentPayload.getWriteFinalDecisionTemplateContent());
        assertEquals("The appeal is allowed.\n"
            + "\n"
            + "The decision made by the Secretary of State on 01/09/2018 is set aside.\n"
            + "\n"
            + "This is my summary.\n"
            + "\n"
            + "My reasons for decision\n"
            + "\n"
            + "Something else.\n"
            + "\n"
            + "This has been an oral (face to face) hearing. An Test attended the hearing today and the Tribunal considered the appeal bundle to page A1. A Presenting Officer attended on behalf of the Respondent.\n"
            + "\n", parentPayload.getWriteFinalDecisionTemplateContent().toString());
    }

    @Override
    protected String getBenefitType() {
        return "UC";
    }


    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willPreviewTheDocumentForScenario1() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorUCScenario.json", Arrays
            .asList("START_DATE_PLACEHOLDER","ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "ANSWER", "SCHEDULE_8_PARA_4", "\"doesSchedule9Paragraph4Apply\" : \"SCHEDULE_9_PARA_4\",", "\"ucWriteFinalDecisionSchedule7ActivitiesApply\" : \"SCHEDULE_7\","), Arrays.asList("2018-10-10", "refused",  "No", "1c", "No", "", ""));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
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
        assertEquals("DRAFT DECISION NOTICE", parentPayload.getNoticeType());
        assertEquals("Judge Full Name", parentPayload.getUserName());
        assertEquals(LocalDate.parse("2017-07-17"), payload.getHeldOn());
        assertEquals("Chester Magistrate's Court", payload.getHeldAt());
        assertEquals("Judge Full Name, Panel Member 1 and Panel Member 2", payload.getHeldBefore());
        assertEquals(false, payload.isAllowed());
        assertEquals(false, payload.isSetAside());
        assertEquals("2018-09-01", payload.getDateOfDecision());
        assertEquals("An Test",payload.getAppellantName());
        assertEquals("2018-10-10",payload.getStartDate());
        assertEquals("2018-11-10",payload.getEndDate());
        assertEquals(false, payload.isIndefinite());
        assertEquals(false, payload.isUcIsEntited());
        assertEquals("no award", payload.getUcAwardRate());
        Assert.assertNotNull(payload.getUcSchedule6Descriptors());
        assertEquals(1, payload.getUcSchedule6Descriptors().size());
        assertEquals(9, payload.getUcSchedule6Descriptors().get(0).getActivityAnswerPoints());
        assertEquals("c", payload.getUcSchedule6Descriptors().get(0).getActivityAnswerLetter());
        assertEquals("Cannot, unaided by another person, either: (i) mobilise more than 100 metres on level ground without stopping in order to avoid significant discomfort or exhaustion; or (ii) repeatedly mobilise 100 metres within a reasonable timescale because of significant discomfort or exhaustion.", payload.getUcSchedule6Descriptors().get(0).getActivityAnswerValue());
        assertEquals("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used.", payload.getUcSchedule6Descriptors().get(0).getActivityQuestionValue());
        assertEquals("1", payload.getUcSchedule6Descriptors().get(0).getActivityQuestionNumber());
        Assert.assertNotNull(payload.getUcNumberOfPoints());
        assertEquals(9, payload.getUcNumberOfPoints().intValue());
        assertNotNull(payload.getReasonsForDecision());
        assertEquals(1, payload.getReasonsForDecision().size());
        Assert.assertEquals("My reasons for decision", payload.getReasonsForDecision().get(0));
        assertEquals("Something else.", payload.getAnythingElse());
        assertNotNull(parentPayload.getWriteFinalDecisionTemplateContent());
        assertNotNull(parentPayload.getWriteFinalDecisionTemplateContent());
        assertEquals("The appeal is refused.\n"
            + "\n"
            + "The decision made by the Secretary of State on 01/09/2018 is confirmed.\n"
            + "\n"
            + "An Test does not have limited capability for work and cannot be treated as having limited capability for work. The matter is now remitted to the Secretary of State to make a final decision upon entitlement to Universal Credit (UC).\n"
            + "\n"
            + "In applying the Work Capability Assessment 9 points were scored from the activities and descriptors in Schedule 6 of the UC Regulations 2013. This is insufficient to meet the threshold for the test. Schedule 8, paragraph 4 of the UC Regulations did not apply.\n"
            + "\n"
            + "1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used.\tc.Cannot, unaided by another person, either: (i) mobilise more than 100 metres on level ground without stopping in order to avoid significant discomfort or exhaustion; or (ii) repeatedly mobilise 100 metres within a reasonable timescale because of significant discomfort or exhaustion.\t9\n"
            + "\n"
            + "\n"
            + "My reasons for decision\n"
            + "\n"
            + "Something else.\n"
            + "\n"
            + "This has been an oral (face to face) hearing. An Test attended the hearing today and the Tribunal considered the appeal bundle to page A1. A Presenting Officer attended on behalf of the Respondent.\n"
            + "\n", parentPayload.getWriteFinalDecisionTemplateContent().toString());
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willPreviewTheDocumentForScenario1_ZeroPoints() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorUCScenario.json", Arrays
            .asList("START_DATE_PLACEHOLDER","ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "ANSWER", "SCHEDULE_8_PARA_4", "\"doesSchedule9Paragraph4Apply\" : \"SCHEDULE_9_PARA_4\",", "\"ucWriteFinalDecisionSchedule7ActivitiesApply\" : \"SCHEDULE_7\","), Arrays.asList("2018-10-10", "refused",  "No", "1e", "No", "", ""));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
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
        assertEquals("DRAFT DECISION NOTICE", parentPayload.getNoticeType());
        assertEquals("Judge Full Name", parentPayload.getUserName());
        assertEquals(LocalDate.parse("2017-07-17"), payload.getHeldOn());
        assertEquals("Chester Magistrate's Court", payload.getHeldAt());
        assertEquals("Judge Full Name, Panel Member 1 and Panel Member 2", payload.getHeldBefore());
        assertEquals(false, payload.isAllowed());
        assertEquals(false, payload.isSetAside());
        assertEquals("2018-09-01", payload.getDateOfDecision());
        assertEquals("An Test",payload.getAppellantName());
        assertEquals("2018-10-10",payload.getStartDate());
        assertEquals("2018-11-10",payload.getEndDate());
        assertEquals(false, payload.isIndefinite());
        assertEquals(false, payload.isUcIsEntited());
        assertEquals("no award", payload.getUcAwardRate());
        Assert.assertNotNull(payload.getUcSchedule6Descriptors());
        assertEquals(0, payload.getUcSchedule6Descriptors().size());
        assertEquals(0, payload.getUcNumberOfPoints().intValue());
        assertNotNull(payload.getReasonsForDecision());
        assertEquals(1, payload.getReasonsForDecision().size());
        Assert.assertEquals("My reasons for decision", payload.getReasonsForDecision().get(0));
        assertEquals("Something else.", payload.getAnythingElse());
        assertNotNull(parentPayload.getWriteFinalDecisionTemplateContent());
        UcTemplateContent ucTemplateContent = (UcTemplateContent)parentPayload.getWriteFinalDecisionTemplateContent();
        Assert.assertEquals(UcScenario.SCENARIO_1, ucTemplateContent.getScenario());
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willPreviewTheDocumentForScenario1_LowPoints() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorUCScenario.json", Arrays.asList("START_DATE_PLACEHOLDER","ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "ANSWER", "SCHEDULE_8_PARA_4", "\"doesSchedule9Paragraph4Apply\" : \"SCHEDULE_9_PARA_4\",", "\"ucWriteFinalDecisionSchedule7ActivitiesApply\" : \"SCHEDULE_7\","), Arrays.asList("2018-10-10", "refused",  "No", "1c", "No", "", ""));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
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
        assertEquals("DRAFT DECISION NOTICE", parentPayload.getNoticeType());
        assertEquals("Judge Full Name", parentPayload.getUserName());
        assertEquals(LocalDate.parse("2017-07-17"), payload.getHeldOn());
        assertEquals("Chester Magistrate's Court", payload.getHeldAt());
        assertEquals("Judge Full Name, Panel Member 1 and Panel Member 2", payload.getHeldBefore());
        assertEquals(false, payload.isAllowed());
        assertEquals(false, payload.isSetAside());
        assertEquals("2018-09-01", payload.getDateOfDecision());
        assertEquals("An Test",payload.getAppellantName());
        assertEquals("2018-10-10",payload.getStartDate());
        assertEquals("2018-11-10",payload.getEndDate());
        assertEquals(false, payload.isIndefinite());
        assertEquals(false, payload.isUcIsEntited());
        assertEquals("no award", payload.getUcAwardRate());
        Assert.assertNotNull(payload.getUcSchedule6Descriptors());
        assertEquals(1, payload.getUcSchedule6Descriptors().size());
        assertEquals(9, payload.getUcSchedule6Descriptors().get(0).getActivityAnswerPoints());
        assertEquals("c", payload.getUcSchedule6Descriptors().get(0).getActivityAnswerLetter());
        assertEquals("Cannot, unaided by another person, either: (i) mobilise more than 100 metres on level ground without stopping in order to avoid significant discomfort or exhaustion; or (ii) repeatedly mobilise 100 metres within a reasonable timescale because of significant discomfort or exhaustion.", payload.getUcSchedule6Descriptors().get(0).getActivityAnswerValue());
        assertEquals("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used.", payload.getUcSchedule6Descriptors().get(0).getActivityQuestionValue());
        assertEquals("1", payload.getUcSchedule6Descriptors().get(0).getActivityQuestionNumber());
        Assert.assertNotNull(payload.getUcNumberOfPoints());
        assertEquals(9, payload.getUcNumberOfPoints().intValue());
        assertNotNull(payload.getReasonsForDecision());
        assertEquals(1, payload.getReasonsForDecision().size());
        Assert.assertEquals("My reasons for decision", payload.getReasonsForDecision().get(0));
        assertEquals("Something else.", payload.getAnythingElse());
        assertNotNull(parentPayload.getWriteFinalDecisionTemplateContent());
        UcTemplateContent ucTemplateContent = (UcTemplateContent)parentPayload.getWriteFinalDecisionTemplateContent();
        Assert.assertEquals(UcScenario.SCENARIO_1, ucTemplateContent.getScenario());
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario1_LowPoints_WhenIncorrectlyAllowed() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorUCScenario.json", Arrays.asList("START_DATE_PLACEHOLDER","ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "ANSWER", "SCHEDULE_8_PARA_4", "\"doesSchedule9Paragraph4Apply\" : \"SCHEDULE_9_PARA_4\",", "\"ucWriteFinalDecisionSchedule7ActivitiesApply\" : \"SCHEDULE_7\","), Arrays.asList("2018-10-10", "allowed",  "No", "1c", "No", "", ""));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());
        // Assert we get a user-friendly error message, as this combination is a possible (incorrect) combination selectable by the user
        assertEquals("You have awarded less than 15 points, specified that the appeal is allowed and specified that Support Group Only Appeal does not apply, "
            + "but have answered No for the Schedule 8 Paragraph 4 question. Please review your previous selection.", result.getErrors().iterator().next());
        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario1_LowPoints_WhenSchedule8Para4SetToYes() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorUCScenario.json", Arrays.asList("START_DATE_PLACEHOLDER","ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "ANSWER", "SCHEDULE_8_PARA_4", "\"doesSchedule9Paragraph4Apply\" : \"SCHEDULE_9_PARA_4\",", "\"ucWriteFinalDecisionSchedule7ActivitiesApply\" : \"SCHEDULE_7\","), Arrays.asList("2018-10-10", "refused",  "No", "1c", "Yes", "", ""));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());

        // This combination is not possible with the correctly configured ccd page flow - just assert that we defensively prevent document generation
        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario1_LowPoints_WhenSupportGroupOnlySetToYes() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorUCScenario.json", Arrays.asList("START_DATE_PLACEHOLDER","ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "ANSWER", "SCHEDULE_8_PARA_4", "\"doesSchedule9Paragraph4Apply\" : \"SCHEDULE_9_PARA_4\",", "\"ucWriteFinalDecisionSchedule7ActivitiesApply\" : \"SCHEDULE_7\","), Arrays.asList("2018-10-10", "refused",  "Yes", "1c", "No", "", ""));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());

        // This combination is not possible with the correctly configured ccd page flow - just assert that we defensively prevent document generation
        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario1_LowPoints_WhenSupportGroupOnlyNotSet() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorUCScenario.json", Arrays.asList("START_DATE_PLACEHOLDER","ALLOWED_OR_REFUSED", "\"supportGroupOnlyAppeal\" : \"SUPPORT_GROUP_ONLY\",", "ANSWER", "SCHEDULE_8_PARA_4", "\"doesSchedule9Paragraph4Apply\" : \"SCHEDULE_9_PARA_4\",", "\"ucWriteFinalDecisionSchedule7ActivitiesApply\" : \"SCHEDULE_7\","), Arrays.asList("2018-10-10", "refused",  "", "1c", "No", "", ""));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());
        // Assert we get a user-friendly error message, as this combination is a possible (incorrect) combination selectable by the user
        assertEquals("You have specified that the appeal is refused, but have a missing answer for the Support Group Only Appeal question. Please review your previous selection.", result.getErrors().iterator().next());
        verifyNoInteractions(generateFile);
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario1_LowPoints_WhenSchedule8Para4NotSet() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorUCScenario.json", Arrays.asList("START_DATE_PLACEHOLDER","ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "ANSWER", "\"doesSchedule8Paragraph4Apply\" : \"SCHEDULE_8_PARA_4\",", "\"doesSchedule9Paragraph4Apply\" : \"SCHEDULE_9_PARA_4\",", "\"ucWriteFinalDecisionSchedule7ActivitiesApply\" : \"SCHEDULE_7\","), Arrays.asList("2018-10-10", "refused",  "No", "1c", "", "", ""));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());

        // This combination is not possible with the correctly configured ccd page flow - just assert that we defensively prevent document generation
        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }


    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario1_WhenHighPoints() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorUCScenario.json", Arrays.asList("START_DATE_PLACEHOLDER","ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "ANSWER", "SCHEDULE_8_PARA_4", "\"doesSchedule9Paragraph4Apply\" : \"SCHEDULE_9_PARA_4\",", "\"ucWriteFinalDecisionSchedule7ActivitiesApply\" : \"SCHEDULE_7\","), Arrays.asList("2018-10-10", "refused",  "No", "1a", "No", "", ""));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());

        // This combination is not possible with the correctly configured ccd page flow - just assert that we defensively prevent document generation
        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willPreviewTheDocumentForScenario2() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorUCScenario.json", Arrays.asList("START_DATE_PLACEHOLDER","ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "\"doesSchedule8Paragraph4Apply\" : \"SCHEDULE_8_PARA_4\",", "SCHEDULE_9_PARA_4", "SCHEDULE_7"), Arrays.asList("2018-10-10", "refused", "Yes", "", "No", "No"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
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
        assertEquals("DRAFT DECISION NOTICE", parentPayload.getNoticeType());
        assertEquals("Judge Full Name", parentPayload.getUserName());
        assertEquals(LocalDate.parse("2017-07-17"), payload.getHeldOn());
        assertEquals("Chester Magistrate's Court", payload.getHeldAt());
        assertEquals("Judge Full Name, Panel Member 1 and Panel Member 2", payload.getHeldBefore());
        assertEquals(false, payload.isAllowed());
        assertEquals(false, payload.isSetAside());
        assertEquals("2018-09-01", payload.getDateOfDecision());
        assertEquals("An Test",payload.getAppellantName());
        assertEquals("2018-10-10",payload.getStartDate());
        assertEquals("2018-11-10",payload.getEndDate());
        assertEquals(false, payload.isIndefinite());
        assertEquals(true, payload.isUcIsEntited());
        assertEquals("lower rate", payload.getUcAwardRate());
        Assert.assertNull(payload.getUcSchedule6Descriptors());
        assertNull(payload.getUcNumberOfPoints());
        assertNotNull(payload.getReasonsForDecision());
        assertEquals(1, payload.getReasonsForDecision().size());
        Assert.assertEquals("My reasons for decision", payload.getReasonsForDecision().get(0));
        assertEquals("Something else.", payload.getAnythingElse());
        assertNotNull(parentPayload.getWriteFinalDecisionTemplateContent());
        UcTemplateContent ucTemplateContent = (UcTemplateContent)parentPayload.getWriteFinalDecisionTemplateContent();
        Assert.assertEquals(UcScenario.SCENARIO_2, ucTemplateContent.getScenario());
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario2_WhenIncorrectlyAllowed() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorUCScenario.json", Arrays.asList("START_DATE_PLACEHOLDER","ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "\"doesSchedule8Paragraph4Apply\" : \"SCHEDULE_8_PARA_4\",", "SCHEDULE_9_PARA_4", "SCHEDULE_7"), Arrays.asList("2018-10-10", "allowed", "Yes", "", "No", "No"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());
        assertEquals("You have specified that the appeal is allowed, specified that Support Group Only Appeal applies and made no selections for the Schedule 7 Activities question, but have answered No for the Schedule 9 Paragraph 4 question. Please review your previous selection.", result.getErrors().iterator().next());

        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario2_WhenSchedule9Para4NotSet() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorUCScenario.json", Arrays.asList("START_DATE_PLACEHOLDER","ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "\"doesSchedule8Paragraph4Apply\" : \"SCHEDULE_8_PARA_4\",", "\"doesSchedule9Paragraph4Apply\" : \"SCHEDULE_9_PARA_4\",", "SCHEDULE_7"), Arrays.asList("2018-10-10", "refused", "Yes", "", "", "No"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());

        // This combination is not possible with the correctly configured ccd page flow - just assert that we defensively prevent document generation
        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario2_WhenSchedule7NotSet() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorUCScenario.json", Arrays.asList("START_DATE_PLACEHOLDER","ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "\"doesSchedule8Paragraph4Apply\" : \"SCHEDULE_8_PARA_4\",", "SCHEDULE_9_PARA_4", "\"ucWriteFinalDecisionSchedule7ActivitiesApply\" : \"SCHEDULE_7\","), Arrays.asList("2018-10-10", "refused", "Yes", "", "No", ""));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());

        // This combination is not possible with the correctly configured ccd page flow - just assert that we defensively prevent document generation
        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }


    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willPreviewTheDocumentForScenario3() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorUCScenario.json", Arrays.asList("START_DATE_PLACEHOLDER","ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "\"doesSchedule8Paragraph4Apply\" : \"SCHEDULE_8_PARA_4\",", "SCHEDULE_9_PARA_4", "SCHEDULE_7"), Arrays.asList("2018-10-10", "allowed", "Yes", "", "Yes", "No"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
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
        assertEquals("DRAFT DECISION NOTICE", parentPayload.getNoticeType());
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
        assertEquals(true, payload.isUcIsEntited());
        assertEquals("higher rate", payload.getUcAwardRate());
        Assert.assertNull(payload.getUcSchedule6Descriptors());
        assertNull(payload.getUcNumberOfPoints());
        assertNotNull(payload.getReasonsForDecision());
        assertEquals(1, payload.getReasonsForDecision().size());
        Assert.assertEquals("My reasons for decision", payload.getReasonsForDecision().get(0));
        assertEquals("Something else.", payload.getAnythingElse());
        assertNotNull(parentPayload.getWriteFinalDecisionTemplateContent());
        UcTemplateContent ucTemplateContent = (UcTemplateContent)parentPayload.getWriteFinalDecisionTemplateContent();
        Assert.assertEquals(UcScenario.SCENARIO_3, ucTemplateContent.getScenario());
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario3_WhenIncorrectlyRefused() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorUCScenario.json", Arrays.asList("START_DATE_PLACEHOLDER","ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "\"doesSchedule8Paragraph4Apply\" : \"SCHEDULE_8_PARA_4\",", "SCHEDULE_9_PARA_4", "SCHEDULE_7"), Arrays.asList("2018-10-10", "refused", "Yes", "", "Yes", "No"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());
        assertEquals("You have awarded less than 15 points, specified that the appeal is refused and specified that Support Group Only Appeal applies, but have answered Yes for the Schedule 9 Paragraph 4 question. Please review your previous selection.", result.getErrors().iterator().next());

        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario3_WhenSchedule9Para4NotSet() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorUCScenario.json", Arrays.asList("START_DATE_PLACEHOLDER","ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "\"doesSchedule8Paragraph4Apply\" : \"SCHEDULE_8_PARA_4\",", "\"doesSchedule9Paragraph4Apply\" : \"SCHEDULE_9_PARA_4\",", "SCHEDULE_7"), Arrays.asList("2018-10-10", "allowed", "Yes", "", "", "No"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());

        // This combination is not possible with the correctly configured ccd page flow - just assert that we defensively prevent document generation
        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario3_WhenSchedule7NotSet() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorUCScenario.json", Arrays.asList("START_DATE_PLACEHOLDER","ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "\"doesSchedule8Paragraph4Apply\" : \"SCHEDULE_8_PARA_4\",", "SCHEDULE_9_PARA_4", "\"ucWriteFinalDecisionSchedule7ActivitiesApply\" : \"SCHEDULE_7\","), Arrays.asList("2018-10-10", "refused", "Yes", "", "Yes", ""));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());

        // This combination is not possible with the correctly configured ccd page flow - just assert that we defensively prevent document generation
        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willPreviewTheDocumentForScenario4() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorUCScenario.json", Arrays.asList("START_DATE_PLACEHOLDER","ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "\"doesSchedule8Paragraph4Apply\" : \"SCHEDULE_8_PARA_4\",", "\"doesSchedule9Paragraph4Apply\" : \"SCHEDULE_9_PARA_4\",", "SCHEDULE_7"), Arrays.asList("2018-10-10", "allowed", "Yes", "", "", "Yes"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
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
        assertEquals("DRAFT DECISION NOTICE", parentPayload.getNoticeType());
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
        assertEquals(true, payload.isUcIsEntited());
        assertEquals("higher rate", payload.getUcAwardRate());
        Assert.assertNull(payload.getUcSchedule6Descriptors());
        assertNull(payload.getUcNumberOfPoints());
        assertNotNull(payload.getReasonsForDecision());
        assertEquals(1, payload.getReasonsForDecision().size());
        Assert.assertEquals("My reasons for decision", payload.getReasonsForDecision().get(0));
        assertEquals("Something else.", payload.getAnythingElse());
        assertNotNull(parentPayload.getWriteFinalDecisionTemplateContent());
        UcTemplateContent ucTemplateContent = (UcTemplateContent)parentPayload.getWriteFinalDecisionTemplateContent();
        Assert.assertEquals(UcScenario.SCENARIO_4, ucTemplateContent.getScenario());
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario4_WhenIncorrectlyRefused() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorUCScenario.json", Arrays.asList("START_DATE_PLACEHOLDER","ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "\"doesSchedule8Paragraph4Apply\" : \"SCHEDULE_8_PARA_4\",", "\"doesSchedule9Paragraph4Apply\" : \"SCHEDULE_9_PARA_4\",", "SCHEDULE_7"), Arrays.asList("2018-10-10", "refused", "Yes", "", "", "Yes"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());
        assertEquals("You have awarded less than 15 points, specified that the appeal is refused and specified that Support Group Only Appeal applies, but have made selections for the Schedule 7 Activities question and a missing answer for the Schedule 9 Paragraph 4 question. Please review your previous selection.", result.getErrors().iterator().next());

        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }


    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario4_WhenNoSchedule7() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorUCScenario.json", Arrays.asList("START_DATE_PLACEHOLDER","ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "\"doesSchedule8Paragraph4Apply\" : \"SCHEDULE_8_PARA_4\",", "\"doesSchedule9Paragraph4Apply\" : \"SCHEDULE_9_PARA_4\",", "SCHEDULE_7"), Arrays.asList("2018-10-10", "allowed", "Yes", "", "", "No"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());

        // This combination is not possible with the correctly configured ccd page flow - just assert that we defensively prevent document generation
        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario5() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorUCScenario.json", Arrays.asList("START_DATE_PLACEHOLDER","ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "\"doesSchedule8Paragraph4Apply\" : \"SCHEDULE_8_PARA_4\",", "SCHEDULE_9_PARA_4", "SCHEDULE_7", "ANSWER"), Arrays.asList("2018-10-10", "allowed", "No", "", "No", "No", "1a"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
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
        assertEquals("DRAFT DECISION NOTICE", parentPayload.getNoticeType());
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
        assertEquals(true, payload.isUcIsEntited());
        assertEquals("lower rate", payload.getUcAwardRate());
        Assert.assertNotNull(payload.getUcSchedule6Descriptors());
        assertEquals(1, payload.getUcSchedule6Descriptors().size());
        assertEquals(15, payload.getUcSchedule6Descriptors().get(0).getActivityAnswerPoints());
        assertEquals("a", payload.getUcSchedule6Descriptors().get(0).getActivityAnswerLetter());
        assertEquals("Cannot, unaided by another person, either: (i) mobilise more than 50 metres on level ground without stopping in order to avoid significant discomfort or exhaustion; or (ii) repeatedly mobilise 50 metres within a reasonable timescale because of significant discomfort or exhaustion.", payload.getUcSchedule6Descriptors().get(0).getActivityAnswerValue());
        assertEquals("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used.", payload.getUcSchedule6Descriptors().get(0).getActivityQuestionValue());
        assertEquals("1", payload.getUcSchedule6Descriptors().get(0).getActivityQuestionNumber());
        Assert.assertNotNull(payload.getUcNumberOfPoints());
        assertEquals(15, payload.getUcNumberOfPoints().intValue());
        assertNotNull(payload.getReasonsForDecision());
        assertEquals(1, payload.getReasonsForDecision().size());
        Assert.assertEquals("My reasons for decision", payload.getReasonsForDecision().get(0));
        assertEquals("Something else.", payload.getAnythingElse());
        assertNotNull(parentPayload.getWriteFinalDecisionTemplateContent());
        UcTemplateContent ucTemplateContent = (UcTemplateContent)parentPayload.getWriteFinalDecisionTemplateContent();
        Assert.assertEquals(UcScenario.SCENARIO_5, ucTemplateContent.getScenario());
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario5_WhenIncorrectlyRefused() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorUCScenario.json", Arrays.asList("START_DATE_PLACEHOLDER","ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "\"doesSchedule8Paragraph4Apply\" : \"SCHEDULE_8_PARA_4\",", "SCHEDULE_9_PARA_4", "SCHEDULE_7", "ANSWER"), Arrays.asList("2018-10-10", "refused", "No", "", "No", "No", "1a"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());
        assertEquals("You have specified that the appeal is refused and specified that Support Group Only Appeal does not apply, but have not awarded less than 15 points, a missing answer for the Schedule 8 Paragraph 4 question, submitted an unexpected answer for the Schedule 7 Activities question and submitted an unexpected answer for the Schedule 9 Paragraph 4 question. Please review your previous selection.", result.getErrors().iterator().next());

        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario5_Schedule9Para4NotSet() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorUCScenario.json", Arrays.asList("START_DATE_PLACEHOLDER","ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "\"doesSchedule8Paragraph4Apply\" : \"SCHEDULE_8_PARA_4\",", "\"doesSchedule9Paragraph4Apply\" : \"SCHEDULE_9_PARA_4\",", "SCHEDULE_7", "ANSWER"), Arrays.asList("2018-10-10", "allowed", "No", "", "", "No", "1a"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());

        // This combination is not possible with the correctly configured ccd page flow - just assert that we defensively prevent document generation
        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario5_LowPoints() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorUCScenario.json", Arrays.asList("START_DATE_PLACEHOLDER","ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "\"doesSchedule8Paragraph4Apply\" : \"SCHEDULE_8_PARA_4\",", "\"doesSchedule9Paragraph4Apply\" : \"SCHEDULE_9_PARA_4\",", "SCHEDULE_7", "ANSWER"), Arrays.asList("2018-10-10", "allowed", "No", "", "", "No", "1c"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());

        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());

        // This combination is not possible with the correctly configured ccd page flow - just assert that we defensively prevent document generation
        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willPreviewTheDocumentForScenario6() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorUCScenario.json", Arrays.asList("START_DATE_PLACEHOLDER","ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "\"doesSchedule8Paragraph4Apply\" : \"SCHEDULE_8_PARA_4\",", "\"doesSchedule9Paragraph4Apply\" : \"SCHEDULE_9_PARA_4\",", "SCHEDULE_7", "ANSWER"), Arrays.asList("2018-10-10", "allowed", "No", "", "", "Yes", "1a"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
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
        assertEquals("DRAFT DECISION NOTICE", parentPayload.getNoticeType());
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
        assertEquals(true, payload.isUcIsEntited());
        assertEquals("higher rate", payload.getUcAwardRate());
        Assert.assertNotNull(payload.getUcSchedule6Descriptors());
        assertEquals(1, payload.getUcSchedule6Descriptors().size());
        assertEquals(15, payload.getUcSchedule6Descriptors().get(0).getActivityAnswerPoints());
        assertEquals("a", payload.getUcSchedule6Descriptors().get(0).getActivityAnswerLetter());
        assertEquals("Cannot, unaided by another person, either: (i) mobilise more than 50 metres on level ground without stopping in order to avoid significant discomfort or exhaustion; or (ii) repeatedly mobilise 50 metres within a reasonable timescale because of significant discomfort or exhaustion.", payload.getUcSchedule6Descriptors().get(0).getActivityAnswerValue());
        assertEquals("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used.", payload.getUcSchedule6Descriptors().get(0).getActivityQuestionValue());
        assertEquals("1", payload.getUcSchedule6Descriptors().get(0).getActivityQuestionNumber());
        Assert.assertNotNull(payload.getUcNumberOfPoints());
        assertEquals(15, payload.getUcNumberOfPoints().intValue());
        assertNotNull(payload.getReasonsForDecision());
        assertEquals(1, payload.getReasonsForDecision().size());
        Assert.assertEquals("My reasons for decision", payload.getReasonsForDecision().get(0));
        assertEquals("Something else.", payload.getAnythingElse());
        assertNotNull(parentPayload.getWriteFinalDecisionTemplateContent());
        UcTemplateContent ucTemplateContent = (UcTemplateContent)parentPayload.getWriteFinalDecisionTemplateContent();
        Assert.assertEquals(UcScenario.SCENARIO_6, ucTemplateContent.getScenario());
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario6_WhenIncorrectlyRefused() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorUCScenario.json", Arrays.asList("START_DATE_PLACEHOLDER","ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "\"doesSchedule8Paragraph4Apply\" : \"SCHEDULE_8_PARA_4\",", "\"doesSchedule9Paragraph4Apply\" : \"SCHEDULE_9_PARA_4\",", "SCHEDULE_7", "ANSWER"), Arrays.asList("2018-10-10", "refused", "No", "", "", "Yes", "1a"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());
        assertEquals("You have specified that the appeal is refused and specified that Support Group Only Appeal does not apply, but have not awarded less than 15 points, a missing answer for the Schedule 8 Paragraph 4 question and submitted an unexpected answer for the Schedule 7 Activities question. Please review your previous selection.", result.getErrors().iterator().next());

        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario6_WhenSchedule7NotSet() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorUCScenario.json", Arrays.asList("START_DATE_PLACEHOLDER","ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "\"doesSchedule8Paragraph4Apply\" : \"SCHEDULE_8_PARA_4\",", "\"doesSchedule9Paragraph4Apply\" : \"SCHEDULE_9_PARA_4\",", "\"ucWriteFinalDecisionSchedule7ActivitiesApply\" : \"SCHEDULE_7\",", "ANSWER"), Arrays.asList("2018-10-10", "allowed", "No", "", "", "", "1a"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());

        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());

        // This combination is not possible with the correctly configured ccd page flow - just assert that we defensively prevent document generation
        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario6_WhenNotSchedule7() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorUCScenario.json", Arrays.asList("START_DATE_PLACEHOLDER","ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "\"doesSchedule8Paragraph4Apply\" : \"SCHEDULE_8_PARA_4\",", "\"doesSchedule9Paragraph4Apply\" : \"SCHEDULE_9_PARA_4\",", "SCHEDULE_7", "ANSWER"), Arrays.asList("2018-10-10", "allowed", "No", "", "", "No", "1a"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());

        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());

        // This combination is not possible with the correctly configured ccd page flow - just assert that we defensively prevent document generation
        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario6_WhenLowPoints() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorUCScenario.json", Arrays.asList("START_DATE_PLACEHOLDER","ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "\"doesSchedule8Paragraph4Apply\" : \"SCHEDULE_8_PARA_4\",", "\"doesSchedule9Paragraph4Apply\" : \"SCHEDULE_9_PARA_4\",", "SCHEDULE_7", "ANSWER"), Arrays.asList("2018-10-10", "allowed", "No", "", "", "Yes", "1c"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());

        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());

        // This combination is not possible with the correctly configured ccd page flow - just assert that we defensively prevent document generation
        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willPreviewTheDocumentForScenario7() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorUCScenario.json", Arrays.asList("START_DATE_PLACEHOLDER","ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "SCHEDULE_8_PARA_4", "SCHEDULE_9_PARA_4", "SCHEDULE_7", "ANSWER"), Arrays.asList("2018-10-10", "allowed", "No", "Yes", "No", "No", "1c"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
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
        assertEquals("DRAFT DECISION NOTICE", parentPayload.getNoticeType());
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
        assertEquals(true, payload.isUcIsEntited());
        assertEquals("lower rate", payload.getUcAwardRate());
        Assert.assertNotNull(payload.getUcSchedule6Descriptors());
        assertEquals(1, payload.getUcSchedule6Descriptors().size());
        assertEquals(9, payload.getUcSchedule6Descriptors().get(0).getActivityAnswerPoints());
        assertEquals("c", payload.getUcSchedule6Descriptors().get(0).getActivityAnswerLetter());
        assertEquals("Cannot, unaided by another person, either: (i) mobilise more than 100 metres on level ground without stopping in order to avoid significant discomfort or exhaustion; or (ii) repeatedly mobilise 100 metres within a reasonable timescale because of significant discomfort or exhaustion.", payload.getUcSchedule6Descriptors().get(0).getActivityAnswerValue());
        assertEquals("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used.", payload.getUcSchedule6Descriptors().get(0).getActivityQuestionValue());
        assertEquals("1", payload.getUcSchedule6Descriptors().get(0).getActivityQuestionNumber());
        Assert.assertNotNull(payload.getUcNumberOfPoints());
        assertEquals(9, payload.getUcNumberOfPoints().intValue());
        assertNotNull(payload.getReasonsForDecision());
        assertEquals(1, payload.getReasonsForDecision().size());
        Assert.assertEquals("My reasons for decision", payload.getReasonsForDecision().get(0));
        assertEquals("Something else.", payload.getAnythingElse());
        assertNotNull(parentPayload.getWriteFinalDecisionTemplateContent());
        UcTemplateContent ucTemplateContent = (UcTemplateContent)parentPayload.getWriteFinalDecisionTemplateContent();
        Assert.assertEquals(UcScenario.SCENARIO_7, ucTemplateContent.getScenario());
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario7_WhenIncorrectlyRefused() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorUCScenario.json", Arrays.asList("START_DATE_PLACEHOLDER","ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "SCHEDULE_8_PARA_4", "SCHEDULE_9_PARA_4", "SCHEDULE_7", "ANSWER"), Arrays.asList("2018-10-10", "refused", "No", "Yes", "No", "No", "1c"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());
        assertEquals("You have specified that the appeal is refused and specified that Support Group Only Appeal does not apply, but have answered Yes for the Schedule 8 Paragraph 4 question, submitted an unexpected answer for the Schedule 7 Activities question and submitted an unexpected answer for the Schedule 9 Paragraph 4 question. Please review your previous selection.", result.getErrors().iterator().next());

        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario7_WhenSchedule9Para4NotSet() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorUCScenario.json", Arrays.asList("START_DATE_PLACEHOLDER","ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "SCHEDULE_8_PARA_4", "\"doesSchedule9Paragraph4Apply\" : \"SCHEDULE_9_PARA_4\",", "SCHEDULE_7", "ANSWER"), Arrays.asList("2018-10-10", "allowed", "No", "Yes", "", "No", "1c"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());

        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());

        // This combination is not possible with the correctly configured ccd page flow - just assert that we defensively prevent document generation
        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario7_WhenSchedule7NotSet() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorUCScenario.json", Arrays.asList("START_DATE_PLACEHOLDER","ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "SCHEDULE_8_PARA_4", "SCHEDULE_9_PARA_4", "\"ucWriteFinalDecisionSchedule7ActivitiesApply\" : \"SCHEDULE_7\",", "ANSWER"), Arrays.asList("2018-10-10", "allowed", "No", "Yes", "No", "", "1c"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());

        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());

        // This combination is not possible with the correctly configured ccd page flow - just assert that we defensively prevent document generation
        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario7_WhenSchedule8Para4IncorrectlyDoesNotApply() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorUCScenario.json", Arrays.asList("START_DATE_PLACEHOLDER","ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "SCHEDULE_8_PARA_4", "SCHEDULE_9_PARA_4", "SCHEDULE_7", "ANSWER"), Arrays.asList("2018-10-10", "allowed", "No", "No", "No", "No", "1c"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());
        assertEquals("You have awarded less than 15 points, specified that the appeal is allowed and specified that Support Group Only Appeal does not apply, but have answered No for the Schedule 8 Paragraph 4 question. Please review your previous selection.", result.getErrors().iterator().next());

        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());

        // This combination is not possible with the correctly configured ccd page flow - just assert that we defensively prevent document generation
        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }


    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willPreviewTheDocumentForScenario8() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorUCScenario.json", Arrays.asList("START_DATE_PLACEHOLDER","ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "SCHEDULE_8_PARA_4", "SCHEDULE_9_PARA_4", "SCHEDULE_7", "ANSWER"), Arrays.asList("2018-10-10", "allowed", "No", "Yes", "Yes", "No", "1c"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
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
        assertEquals("DRAFT DECISION NOTICE", parentPayload.getNoticeType());
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
        assertEquals(true, payload.isUcIsEntited());
        assertEquals("higher rate", payload.getUcAwardRate());
        Assert.assertNotNull(payload.getUcSchedule6Descriptors());
        assertEquals(1, payload.getUcSchedule6Descriptors().size());
        assertEquals(9, payload.getUcSchedule6Descriptors().get(0).getActivityAnswerPoints());
        assertEquals("c", payload.getUcSchedule6Descriptors().get(0).getActivityAnswerLetter());
        assertEquals("Cannot, unaided by another person, either: (i) mobilise more than 100 metres on level ground without stopping in order to avoid significant discomfort or exhaustion; or (ii) repeatedly mobilise 100 metres within a reasonable timescale because of significant discomfort or exhaustion.", payload.getUcSchedule6Descriptors().get(0).getActivityAnswerValue());
        assertEquals("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used.", payload.getUcSchedule6Descriptors().get(0).getActivityQuestionValue());
        assertEquals("1", payload.getUcSchedule6Descriptors().get(0).getActivityQuestionNumber());
        Assert.assertNotNull(payload.getUcNumberOfPoints());
        assertEquals(9, payload.getUcNumberOfPoints().intValue());
        assertNotNull(payload.getReasonsForDecision());
        assertEquals(1, payload.getReasonsForDecision().size());
        Assert.assertEquals("My reasons for decision", payload.getReasonsForDecision().get(0));
        assertEquals("Something else.", payload.getAnythingElse());
        assertNotNull(parentPayload.getWriteFinalDecisionTemplateContent());
        UcTemplateContent ucTemplateContent = (UcTemplateContent)parentPayload.getWriteFinalDecisionTemplateContent();
        Assert.assertEquals(UcScenario.SCENARIO_8, ucTemplateContent.getScenario());
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario8_WhenIncorrectlyRefused() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorUCScenario.json", Arrays.asList("START_DATE_PLACEHOLDER","ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "SCHEDULE_8_PARA_4", "SCHEDULE_9_PARA_4", "SCHEDULE_7", "ANSWER"), Arrays.asList("2018-10-10", "refused", "No", "Yes", "Yes", "No", "1c"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());
        assertEquals("You have specified that the appeal is refused and specified that Support Group Only Appeal does not apply, but have answered Yes for the Schedule 8 Paragraph 4 question, submitted an unexpected answer for the Schedule 7 Activities question and submitted an unexpected answer for the Schedule 9 Paragraph 4 question. Please review your previous selection.", result.getErrors().iterator().next());

        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willPreviewTheDocumentForScenario9() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorUCScenario.json", Arrays.asList("START_DATE_PLACEHOLDER","ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "SCHEDULE_8_PARA_4", "SCHEDULE_9_PARA_4", "SCHEDULE_7", "ANSWER"), Arrays.asList("2018-10-10", "allowed", "No", "Yes", "No", "Yes", "1c"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
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
        assertEquals("DRAFT DECISION NOTICE", parentPayload.getNoticeType());
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
        assertEquals(true, payload.isUcIsEntited());
        assertEquals("higher rate", payload.getUcAwardRate());
        Assert.assertNotNull(payload.getUcSchedule6Descriptors());
        assertEquals(1, payload.getUcSchedule6Descriptors().size());
        assertEquals(9, payload.getUcSchedule6Descriptors().get(0).getActivityAnswerPoints());
        assertEquals("c", payload.getUcSchedule6Descriptors().get(0).getActivityAnswerLetter());
        assertEquals("Cannot, unaided by another person, either: (i) mobilise more than 100 metres on level ground without stopping in order to avoid significant discomfort or exhaustion; or (ii) repeatedly mobilise 100 metres within a reasonable timescale because of significant discomfort or exhaustion.", payload.getUcSchedule6Descriptors().get(0).getActivityAnswerValue());
        assertEquals("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used.", payload.getUcSchedule6Descriptors().get(0).getActivityQuestionValue());
        assertEquals("1", payload.getUcSchedule6Descriptors().get(0).getActivityQuestionNumber());
        Assert.assertNotNull(payload.getUcNumberOfPoints());
        assertEquals(9, payload.getUcNumberOfPoints().intValue());
        assertNotNull(payload.getReasonsForDecision());
        assertEquals(1, payload.getReasonsForDecision().size());
        Assert.assertEquals("My reasons for decision", payload.getReasonsForDecision().get(0));
        assertEquals("Something else.", payload.getAnythingElse());
        assertNotNull(parentPayload.getWriteFinalDecisionTemplateContent());
        UcTemplateContent ucTemplateContent = (UcTemplateContent)parentPayload.getWriteFinalDecisionTemplateContent();
        Assert.assertEquals(UcScenario.SCENARIO_9, ucTemplateContent.getScenario());
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario9_WhenIncorrectlyRefused() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorUCScenario.json", Arrays.asList("START_DATE_PLACEHOLDER","ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "SCHEDULE_8_PARA_4", "SCHEDULE_9_PARA_4", "SCHEDULE_7", "ANSWER"), Arrays.asList("2018-10-10", "refused", "No", "Yes", "No", "Yes", "1c"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());
        assertEquals("You have specified that the appeal is refused and specified that Support Group Only Appeal does not apply, but have answered Yes for the Schedule 8 Paragraph 4 question and submitted an unexpected answer for the Schedule 7 Activities question. Please review your previous selection.", result.getErrors().iterator().next());

        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willPreviewTheDocumentForUcLcwaScenario10Refused() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorUCNonLcwa.json", Arrays.asList("START_DATE_PLACEHOLDER", "ALLOWED_OR_REFUSED", "ANSWERED_QUESTIONS"), Arrays.asList("2018-10-10", "refused", ""));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
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
        assertEquals("DRAFT DECISION NOTICE", parentPayload.getNoticeType());
        assertEquals("Judge Full Name", parentPayload.getUserName());
        assertEquals(LocalDate.parse("2017-07-17"), payload.getHeldOn());
        assertEquals("Chester Magistrate's Court", payload.getHeldAt());
        assertEquals("Judge Full Name, Panel Member 1 and Panel Member 2", payload.getHeldBefore());
        assertEquals(false, payload.isAllowed());
        assertEquals(false, payload.isSetAside());
        assertEquals("2018-09-01", payload.getDateOfDecision());
        assertEquals("An Test",payload.getAppellantName());
        assertEquals("2018-10-10",payload.getStartDate());
        assertEquals("2018-11-10",payload.getEndDate());
        assertEquals(false, payload.isIndefinite());
        assertEquals(false, payload.isUcIsEntited());
        assertNull(payload.getUcAwardRate());
        Assert.assertNull(payload.getUcSchedule6Descriptors());
        Assert.assertNull(payload.getUcNumberOfPoints());
        assertNotNull(payload.getReasonsForDecision());
        assertEquals(1, payload.getReasonsForDecision().size());
        Assert.assertEquals("My reasons for decision", payload.getReasonsForDecision().get(0));
        assertEquals("Something else.", payload.getAnythingElse());
        assertNotNull(parentPayload.getWriteFinalDecisionTemplateContent());
        assertNotNull(parentPayload.getWriteFinalDecisionTemplateContent());
        assertEquals("The appeal is refused.\n"
            + "\n"
            + "The decision made by the Secretary of State on 01/09/2018 is confirmed.\n"
            + "\n"
            + "This is my summary.\n"
            + "\n"
            + "My reasons for decision\n"
            + "\n"
            + "Something else.\n"
            + "\n"
            + "This has been an oral (face to face) hearing. An Test attended the hearing today and the Tribunal considered the appeal bundle to page A1. A Presenting Officer attended on behalf of the Respondent.\n"
            + "\n", parentPayload.getWriteFinalDecisionTemplateContent().toString());
        UcTemplateContent ucTemplateContent = (UcTemplateContent)parentPayload.getWriteFinalDecisionTemplateContent();
        Assert.assertEquals(UcScenario.SCENARIO_10, ucTemplateContent.getScenario());
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willPreviewTheDocumentForUcNonLcwaScenario10Allowed() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorUCNonLcwa.json", Arrays.asList("START_DATE_PLACEHOLDER", "ALLOWED_OR_REFUSED", "ANSWERED_QUESTIONS"), Arrays.asList("2018-10-10", "allowed", ""));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
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
        assertEquals("DRAFT DECISION NOTICE", parentPayload.getNoticeType());
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
        assertEquals(false, payload.isUcIsEntited());
        assertNull(payload.getUcAwardRate());
        Assert.assertNull(payload.getUcSchedule6Descriptors());
        Assert.assertNull(payload.getUcNumberOfPoints());
        assertNotNull(payload.getReasonsForDecision());
        assertEquals(1, payload.getReasonsForDecision().size());
        Assert.assertEquals("My reasons for decision", payload.getReasonsForDecision().get(0));
        assertEquals("Something else.", payload.getAnythingElse());
        assertNotNull(parentPayload.getWriteFinalDecisionTemplateContent());
        assertNotNull(parentPayload.getWriteFinalDecisionTemplateContent());
        assertEquals("The appeal is allowed.\n"
            + "\n"
            + "The decision made by the Secretary of State on 01/09/2018 is set aside.\n"
            + "\n"
            + "This is my summary.\n"
            + "\n"
            + "My reasons for decision\n"
            + "\n"
            + "Something else.\n"
            + "\n"
            + "This has been an oral (face to face) hearing. An Test attended the hearing today and the Tribunal considered the appeal bundle to page A1. A Presenting Officer attended on behalf of the Respondent.\n"
            + "\n", parentPayload.getWriteFinalDecisionTemplateContent().toString());
        UcTemplateContent ucTemplateContent = (UcTemplateContent)parentPayload.getWriteFinalDecisionTemplateContent();
        Assert.assertEquals(UcScenario.SCENARIO_10, ucTemplateContent.getScenario());
    }


    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willPreviewTheDocumentForUcNonLcwa_WhenPreviousQuestionsAnsweredAndOver15Points() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorUCNonLcwa.json", Arrays.asList("START_DATE_PLACEHOLDER", "ALLOWED_OR_REFUSED", "ANSWERED_QUESTIONS"), Arrays.asList("2018-10-10", "allowed", "mobilisingUnaided\", \"standingAndSitting"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
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
        assertEquals("DRAFT DECISION NOTICE", parentPayload.getNoticeType());
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
        assertEquals(false, payload.isUcIsEntited());
        assertNull(payload.getUcAwardRate());
        Assert.assertNull(payload.getUcSchedule6Descriptors());
        Assert.assertNull(payload.getUcNumberOfPoints());
        assertNotNull(payload.getReasonsForDecision());
        assertEquals(1, payload.getReasonsForDecision().size());
        Assert.assertEquals("My reasons for decision", payload.getReasonsForDecision().get(0));
        assertEquals("Something else.", payload.getAnythingElse());
        assertNotNull(parentPayload.getWriteFinalDecisionTemplateContent());
        assertNotNull(parentPayload.getWriteFinalDecisionTemplateContent());
        assertEquals("The appeal is allowed.\n"
            + "\n"
            + "The decision made by the Secretary of State on 01/09/2018 is set aside.\n"
            + "\n"
            + "This is my summary.\n"
            + "\n"
            + "My reasons for decision\n"
            + "\n"
            + "Something else.\n"
            + "\n"
            + "This has been an oral (face to face) hearing. An Test attended the hearing today and the Tribunal considered the appeal bundle to page A1. A Presenting Officer attended on behalf of the Respondent.\n"
            + "\n", parentPayload.getWriteFinalDecisionTemplateContent().toString());
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willPreviewTheDocumentForScenario12() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorUCScenario.json", Arrays.asList("START_DATE_PLACEHOLDER","ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "\"doesSchedule8Paragraph4Apply\" : \"SCHEDULE_8_PARA_4\",", "SCHEDULE_9_PARA_4", "SCHEDULE_7", "ANSWER"), Arrays.asList("2018-10-10", "allowed", "No", "", "Yes", "No", "1a"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
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
        assertEquals("DRAFT DECISION NOTICE", parentPayload.getNoticeType());
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
        assertEquals(true, payload.isUcIsEntited());
        assertEquals("higher rate", payload.getUcAwardRate());
        Assert.assertNotNull(payload.getUcSchedule6Descriptors());
        assertEquals(1, payload.getUcSchedule6Descriptors().size());
        assertEquals(15, payload.getUcSchedule6Descriptors().get(0).getActivityAnswerPoints());
        assertEquals("a", payload.getUcSchedule6Descriptors().get(0).getActivityAnswerLetter());
        assertEquals("Cannot, unaided by another person, either: (i) mobilise more than 50 metres on level ground without stopping in order to avoid significant discomfort or exhaustion; or (ii) repeatedly mobilise 50 metres within a reasonable timescale because of significant discomfort or exhaustion.", payload.getUcSchedule6Descriptors().get(0).getActivityAnswerValue());
        assertEquals("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used.", payload.getUcSchedule6Descriptors().get(0).getActivityQuestionValue());
        assertEquals("1", payload.getUcSchedule6Descriptors().get(0).getActivityQuestionNumber());
        Assert.assertNotNull(payload.getUcNumberOfPoints());
        assertEquals(15, payload.getUcNumberOfPoints().intValue());
        assertNotNull(payload.getReasonsForDecision());
        assertEquals(1, payload.getReasonsForDecision().size());
        Assert.assertEquals("My reasons for decision", payload.getReasonsForDecision().get(0));
        assertEquals("Something else.", payload.getAnythingElse());
        assertNotNull(parentPayload.getWriteFinalDecisionTemplateContent());
        UcTemplateContent ucTemplateContent = (UcTemplateContent)parentPayload.getWriteFinalDecisionTemplateContent();
        Assert.assertEquals(UcScenario.SCENARIO_12, ucTemplateContent.getScenario());
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario12_WhenIncorrectlyRefused() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorUCScenario.json", Arrays.asList("START_DATE_PLACEHOLDER","ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "\"doesSchedule8Paragraph4Apply\" : \"SCHEDULE_8_PARA_4\",", "SCHEDULE_9_PARA_4", "SCHEDULE_7", "ANSWER"), Arrays.asList("2018-10-10", "refused", "No", "", "Yes", "No", "1a"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());
        assertEquals("You have specified that the appeal is refused and specified that Support Group Only Appeal does not apply, but have not awarded less than 15 points, a missing answer for the Schedule 8 Paragraph 4 question, submitted an unexpected answer for the Schedule 7 Activities question and submitted an unexpected answer for the Schedule 9 Paragraph 4 question. Please review your previous selection.", result.getErrors().iterator().next());

        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }




    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario12_WhenSchedule9Para4NotSet() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorUCScenario.json", Arrays.asList("START_DATE_PLACEHOLDER","ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "\"doesSchedule8Paragraph4Apply\" : \"SCHEDULE_8_PARA_4\",", "\"doesSchedule9Paragraph4Apply\" : \"SCHEDULE_9_PARA_4\",", "SCHEDULE_7", "ANSWER"), Arrays.asList("2018-10-10", "allowed", "No", "", "", "No", "1a"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());

        // This combination is not possible with the correctly configured ccd page flow - just assert that we defensively prevent document generation
        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }

    @Test
    public void callToMidEventPreviewFinalDecisionCallback_willNotPreviewTheDocumentForScenario12_WhenLowPoints() throws Exception {
        setup();
        setJsonAndReplace("callback/writeFinalDecisionDescriptorUCScenario.json", Arrays.asList("START_DATE_PLACEHOLDER","ALLOWED_OR_REFUSED", "SUPPORT_GROUP_ONLY", "\"doesSchedule8Paragraph4Apply\" : \"SCHEDULE_8_PARA_4\",", "SCHEDULE_9_PARA_4", "SCHEDULE_7", "ANSWER"), Arrays.asList("2018-10-10", "allowed", "No", "", "Yes", "No", "1c"));

        String documentUrl = "document.url";
        when(generateFile.assemble(any())).thenReturn(documentUrl);

        when(userDetails.getFullName()).thenReturn("Judge Full Name");

        when(idamClient.getUserDetails("Bearer userToken")).thenReturn(userDetails);

        MockHttpServletResponse response = getResponse(getRequestWithAuthHeader(json, "/ccdMidEventPreviewFinalDecision"));
        assertHttpStatus(response, HttpStatus.OK);
        PreSubmitCallbackResponse<SscsCaseData> result = deserialize(response.getContentAsString());

        assertEquals(1, result.getErrors().size());

        // This combination is not possible with the correctly configured ccd page flow - just assert that we defensively prevent document generation
        assertEquals("http://dm-store:5005/documents/7539160a-b124-4539-b7c1-f3dcfbcea94c", result.getData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
        verifyNoInteractions(generateFile);
    }
}
