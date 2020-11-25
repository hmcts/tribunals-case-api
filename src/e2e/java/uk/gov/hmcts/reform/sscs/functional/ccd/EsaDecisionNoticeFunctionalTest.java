package uk.gov.hmcts.reform.sscs.functional.ccd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static uk.gov.hmcts.reform.sscs.functional.handlers.BaseHandler.getJsonCallbackForTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import uk.gov.hmcts.reform.sscs.functional.mya.BaseFunctionTest;

@RunWith(JUnitParamsRunner.class)
@TestPropertySource(locations = "classpath:config/application_e2e.properties")
public class EsaDecisionNoticeFunctionalTest extends BaseFunctionTest {

    @ClassRule
    public static final SpringClassRule scr = new SpringClassRule();

    @Rule
    public final SpringMethodRule smr = new SpringMethodRule();

    @Autowired
    protected ObjectMapper objectMapper;

    // The Scenarios are defined https://tools.hmcts.net/confluence/display/SSCS/ESA+DN+template+content+-+judges+input
    @Test
    public void scenario1_refused_non_support_group_less_than_15pts_sch2_reg29NotApplies_shouldGeneratePdfWithExpectedText() throws IOException {
        String json = getJsonCallbackForTest("handlers/writefinaldecision/esaRefusedNonSupportGroupOnlyCallback.json");
        byte[] bytes = callPreviewFinalDecision(json);
        try (PDDocument document = PDDocument.load(bytes)) {
            String pdfText = new PDFTextStripper().getText(document);
            String pdfTextWithoutNewLines = replaceNewLines(pdfText);
            assertThat(pdfTextWithoutNewLines, containsString("1. The appeal is refused."));
            assertThat(pdfTextWithoutNewLines, containsString("2. The decision made by the Secretary of State on 16/11/2020 is confirmed."));
            assertThat(pdfTextWithoutNewLines, containsString("3. Joe Bloggs does not have limited capability for work and cannot be treated as having limited capability for work."));
            assertThat(pdfTextWithoutNewLines, containsString("4. In applying the work capability assessment 12 points were scored from the activities and descriptors in Schedule 2 of the ESA Regulations 2008. This is insufficient to meet the threshold for the test. Regulation 29 of the Employment and Support Allowance (ESA) Regulations 2008 did not apply."));
            assertThat(pdfTextWithoutNewLines, containsString("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used."));
            assertThat(pdfTextWithoutNewLines, containsString("14. Coping with change."));
            assertThat(pdfTextWithoutNewLines, containsString("6 points"));
            assertThat(pdfTextWithoutNewLines, containsString("5. Reasons for decision 1"));
            assertThat(pdfTextWithoutNewLines, containsString("6. Reasons for decision 2"));
            assertThat(pdfTextWithoutNewLines, containsString("7. Anything else."));
            assertThat(pdfTextWithoutNewLines, containsString("8. This has been a remote hearing in the form of a video hearing. Joe Bloggs attended and the Tribunal considered the appeal bundle to page B7. A Presenting Officer attended on behalf of the Respondent."));
            assertThat(pdfTextWithoutNewLines, not(containsString("9.")));
        }
    }

    @Test
    public void scenario2_refused_isSupportGroup_noSch3_Reg29NotApplies_shouldGeneratePdfWithExpectedText() throws IOException {
        String json = getJsonCallbackForTest("handlers/writefinaldecision/esaRefusedSupportNoSch3NoReg35Callback.json");
        byte[] bytes = callPreviewFinalDecision(json);
        try (PDDocument document = PDDocument.load(bytes)) {
            String pdfText = new PDFTextStripper().getText(document);
            String pdfTextWithoutNewLines = replaceNewLines(pdfText);
            assertThat(pdfTextWithoutNewLines, containsString("1. The appeal is refused."));
            assertThat(pdfTextWithoutNewLines, containsString("2. The decision made by the Secretary of State on 16/11/2020 is confirmed."));
            assertThat(pdfTextWithoutNewLines, containsString("3. Joe Bloggs continues to have limited capability for work but does not have limited capability for work-related activity. This is because no descriptor from Schedule 3 of the Employment and Support Allowance (ESA) Regulations 2008 applied. Regulation 35 did not apply. The Secretary of State has accepted that Joe Bloggs has limited capability for work. This was not in issue."));
            assertThat(pdfTextWithoutNewLines, containsString("4. Reasons for decision"));
            assertThat(pdfTextWithoutNewLines, containsString("5. Anything else"));
            assertThat(pdfTextWithoutNewLines, containsString("6. No party has objected to the matter being decided without a hearing. Having considered the appeal bundle to page B7 and the requirements of rules 2 and 27 of the Tribunal Procedure (First-tier Tribunal) (Social Entitlement Chamber) Rules 2008 the Tribunal is satisfied that it is able to decide the case in this way."));
            assertThat(pdfTextWithoutNewLines, not(containsString("7.")));
        }
    }

    @Test
    public void scenario3_allowed_isSupportGroup_noSch3_Reg35Applies() throws IOException {
        String json = getJsonCallbackForTest("handlers/writefinaldecision/esaAllowedSupportNoSch3NoReg35Callback.json");
        byte[] bytes = callPreviewFinalDecision(json);
        try (PDDocument document = PDDocument.load(bytes)) {
            String pdfText = new PDFTextStripper().getText(document);
            String pdfTextWithoutNewLines = replaceNewLines(pdfText);
            assertThat(pdfTextWithoutNewLines, containsString("1. The appeal is allowed."));
            assertThat(pdfTextWithoutNewLines, containsString("2. The decision made by the Secretary of State on 17/11/2020 is set aside."));
            assertThat(pdfTextWithoutNewLines, containsString("3. Joe Bloggs is to be treated as having limited capability for work-related activity."));
            assertThat(pdfTextWithoutNewLines, containsString("4. The Secretary of State has accepted that Joe Bloggs has limited capability for work related activity. This was not an issue."));
            assertThat(pdfTextWithoutNewLines, containsString("5. No descriptor from Schedule 3 of the Employment and Support Allowance (ESA) Regulations 2008 was satisfied but regulation 35 applied."));
            assertThat(pdfTextWithoutNewLines, containsString("6. The tribunal applied regulation 35 because there would be a substantial risk to the mental or physical health of any person if the appellant were found not to have limited capability for work and for work-related activity."));
            assertThat(pdfTextWithoutNewLines, containsString("7. Reasons for decision"));
            assertThat(pdfTextWithoutNewLines, containsString("8. Anything else"));
            assertThat(pdfTextWithoutNewLines, containsString("9. This has been an oral (face to face) hearing. Joe Bloggs attended the hearing today and the Tribunal considered the appeal bundle to page B7. No Presenting Officer attended on behalf of the Respondent."));
            assertThat(pdfTextWithoutNewLines, containsString("10. Any recommendation given below does not form part of the Tribunal's decision and is not binding on the Secretary of State. The Tribunal recommends that the Department does not reassess Joe Bloggs within 3 months from today's date."));
            assertThat(pdfTextWithoutNewLines, not(containsString("11.")));
        }
    }

    @Test
    public void scenario4_allowed_isSupportGroup_selectionMadeForSch3() throws IOException {
        String json = getJsonCallbackForTest("handlers/writefinaldecision/esaAllowedIsSupportGroupSch3SelectionMadeCallback.json");
        byte[] bytes = callPreviewFinalDecision(json);
        try (PDDocument document = PDDocument.load(bytes)) {
            String pdfText = new PDFTextStripper().getText(document);
            String pdfTextWithoutNewLines = replaceNewLines(pdfText);
            assertThat(pdfTextWithoutNewLines, containsString("1. The appeal is allowed."));
            assertThat(pdfTextWithoutNewLines, containsString("2. The decision made by the Secretary of State on 17/11/2020 is set aside."));
            assertThat(pdfTextWithoutNewLines, containsString("3. Joe Bloggs has limited capability for work-related activity."));
            assertThat(pdfTextWithoutNewLines, containsString("4. The Secretary of State has accepted that Joe Bloggs has limited capability for work. This was not an issue."));
            assertThat(pdfTextWithoutNewLines, containsString("5. The following activity and descriptor from Schedule 3 applied:"));
            assertThat(pdfTextWithoutNewLines, containsString("1. Mobilising unaided by another person with or without a walking stick"));
            assertThat(pdfTextWithoutNewLines, containsString("6. Reasons for decision"));
            assertThat(pdfTextWithoutNewLines, containsString("7. Anything else"));
            assertThat(pdfTextWithoutNewLines, containsString("8. This has been a remote hearing in the form of a triage hearing. Joe Bloggs did not attend the hearing today. No Presenting Officer attended on behalf of the Respondent. Having considered the appeal bundle to page B7 and the requirements of rules 2 and 31 of The Tribunal Procedure (First-tier Tribunal)(Social Entitlement Chamber) Rules 2008 the Tribunal is satisfied that reasonable steps were taken to notify Joe Bloggs of the hearing and that it is in the interests of justice to proceed today."));
            assertThat(pdfTextWithoutNewLines, containsString("9. Any recommendation given below does not form part of the Tribunal's decision and is not binding on the Secretary of State. In view of the degree of disability found by the Tribunal, and unless the regulations change, the Tribunal would recommend that the appellant is not re-assessed."));
            assertThat(pdfTextWithoutNewLines, not(containsString("10.")));
        }
    }

    @Test
    public void scenario5_allowed_notSupportGroup_moreThan15Points_noSch3_NoReg35() throws IOException {
        String json = getJsonCallbackForTest("handlers/writefinaldecision/esaAllowedNonSupportGroupNoSch3NoReg35Callback.json");
        byte[] bytes = callPreviewFinalDecision(json);
        try (PDDocument document = PDDocument.load(bytes)) {
            String pdfText = new PDFTextStripper().getText(document);
            String pdfTextWithoutNewLines = replaceNewLines(pdfText);
            assertThat(pdfTextWithoutNewLines, containsString("1. The appeal is allowed."));
            assertThat(pdfTextWithoutNewLines, containsString("2. The decision made by the Secretary of State on 17/11/2020 is set aside."));
            assertThat(pdfTextWithoutNewLines, containsString("3. Joe Bloggs has limited capability for work."));
            assertThat(pdfTextWithoutNewLines, containsString("4. In applying the work capability assessment 30 points were scored from the activities and descriptors in Schedule 2 of the ESA Regulations 2008 made up as follows:"));
            assertThat(pdfTextWithoutNewLines, containsString("5. Joe Bloggs does not have limited capability for work-related activity because no descriptor from Schedule 3 applied. Regulation 35 did not apply."));
            assertThat(pdfTextWithoutNewLines, containsString("1. Mobilising unaided by another"));
            assertThat(pdfTextWithoutNewLines, containsString("2. Standing and sitting."));
            assertThat(pdfTextWithoutNewLines, containsString("15 points"));
            assertThat(pdfTextWithoutNewLines, containsString("6. Reasons for decision"));
            assertThat(pdfTextWithoutNewLines, containsString("7. Anything else"));
            assertThat(pdfTextWithoutNewLines, containsString("8. This has been a remote hearing in the form of a telephone hearing. Joe Bloggs attended and the Tribunal considered the appeal bundle to page B7. No Presenting Officer attended on behalf of the Respondent."));
            assertThat(pdfTextWithoutNewLines, containsString("9. Any recommendation given below does not form part of the Tribunal's decision and is not binding on the Secretary of State. The Tribunal makes no recommendation as to when the Department should reassess Joe Bloggs."));
            assertThat(pdfTextWithoutNewLines, not(containsString("10.")));
        }
    }

    @Test
    public void scenario8_allowed_notSupportGroup_LessThan15PointsSch2_Reg29Applies_Reg35Applies() throws IOException {
        String json = getJsonCallbackForTest("handlers/writefinaldecision/esaAllowedNoSupportGroupLessThan15PointsReg29AndReg35AppliesCallback.json");
        byte[] bytes = callPreviewFinalDecision(json);
        try (PDDocument document = PDDocument.load(bytes)) {
            String pdfText = new PDFTextStripper().getText(document);
            String pdfTextWithoutNewLines = replaceNewLines(pdfText);
            assertThat(pdfTextWithoutNewLines, containsString("1. The appeal is allowed."));
            assertThat(pdfTextWithoutNewLines, containsString("2. The decision made by the Secretary of State on 17/11/2020 is set aside."));
            assertThat(pdfTextWithoutNewLines, containsString("3. Joe Bloggs is to be treated as having limited capability for work and for work-related activity."));
            assertThat(pdfTextWithoutNewLines, containsString("4. This is because insufficient points were scored to meet the threshold for the work capability assessment and none of the Schedule 3 activities and descriptors were satisfied, but the tribunal applied regulations 29 and 35 of the Employment and Support Allowance Regulations (ESA) 2008."));
            assertThat(pdfTextWithoutNewLines, containsString("5. In applying the work capability assessment 9 points were scored from the activities and descriptors in Schedule 2 of the ESA Regulations 2008 made up as follows:"));
            assertThat(pdfTextWithoutNewLines, containsString("1. Mobilising unaided by another"));
            assertThat(pdfTextWithoutNewLines, containsString("9 points"));
            assertThat(pdfTextWithoutNewLines, containsString("6. The tribunal applied regulations 29 and 35 because there would be a substantial risk to the mental or physical health of any person if the appellant were found not to have limited capability for work and for work-related activity."));
            assertThat(pdfTextWithoutNewLines, containsString("7. Reasons for decision"));
            assertThat(pdfTextWithoutNewLines, containsString("8. Anything else"));
            assertThat(pdfTextWithoutNewLines, containsString("9. This has been a remote hearing in the form of a triage hearing. Joe Bloggs did not attend the hearing today. No Presenting Officer attended on behalf of the Respondent. Having considered the appeal bundle to page B7 and the requirements of rules 2 and 31 of The Tribunal Procedure (First-tier Tribunal)(Social Entitlement Chamber) Rules 2008 the Tribunal is satisfied that reasonable steps were taken to notify Joe Bloggs of the hearing and that it is in the interests of justice to proceed today."));
            assertThat(pdfTextWithoutNewLines, containsString("10. Any recommendation given below does not form part of the Tribunal's decision and is not binding on the Secretary of State. The Tribunal recommends that the Department reassesses Joe Bloggs within 3 months from today's date."));
            assertThat(pdfTextWithoutNewLines, not(containsString("11.")));
        }
    }

    @Test
    @Parameters({
        "noRecommendation, The Tribunal makes no recommendation as to when the Department should reassess Joe Bloggs.",
        "doNotReassess, In view of the degree of disability found by the Tribunal\\, and unless the regulations change\\, the Tribunal would recommend that the appellant is not re-assessed.",
        "reassess12, The Tribunal recommends that the Department reassesses Joe Bloggs within 12 months from today's date.",
        "reassess3, The Tribunal recommends that the Department reassesses Joe Bloggs within 3 months from today's date.",
        "doNotReassess3, The Tribunal recommends that the Department does not reassess Joe Bloggs within 3 months from today's date.",
        "doNotReassess18, The Tribunal recommends that the Department does not reassess Joe Bloggs within 18 months from today's date.",
    })
    public void scenario8_allowed_notSupportGroup_LessThan15PointsSch2_Reg29Applies_Reg35Applies_shouldHaveExpectedText(String code, String expectedText) throws IOException {
        String json = getJsonCallbackForTest("handlers/writefinaldecision/esaDwpReassessTheAwardCallback.json");
        json = json.replaceAll("DWP_REASSESS_THE_AWARD", code);
        byte[] bytes = callPreviewFinalDecision(json);
        try (PDDocument document = PDDocument.load(bytes)) {
            String pdfText = new PDFTextStripper().getText(document);
            String pdfTextWithoutNewLines = replaceNewLines(pdfText);
            assertThat(pdfTextWithoutNewLines, containsString("1. The appeal is allowed."));
            assertThat(pdfTextWithoutNewLines, containsString("2. The decision made by the Secretary of State on 17/11/2020 is set aside."));
            assertThat(pdfTextWithoutNewLines, containsString("3. Joe Bloggs is to be treated as having limited capability for work and for work-related activity."));
            assertThat(pdfTextWithoutNewLines, containsString("4. This is because insufficient points were scored to meet the threshold for the work capability assessment and none of the Schedule 3 activities and descriptors were satisfied, but the tribunal applied regulations 29 and 35 of the Employment and Support Allowance Regulations (ESA) 2008."));
            assertThat(pdfTextWithoutNewLines, containsString("5. In applying the work capability assessment 9 points were scored from the activities and descriptors in Schedule 2 of the ESA Regulations 2008 made up as follows:"));
            assertThat(pdfTextWithoutNewLines, containsString("1. Mobilising unaided by another"));
            assertThat(pdfTextWithoutNewLines, containsString("9 points"));
            assertThat(pdfTextWithoutNewLines, containsString("6. The tribunal applied regulations 29 and 35 because there would be a substantial risk to the mental or physical health of any person if the appellant were found not to have limited capability for work and for work-related activity."));
            assertThat(pdfTextWithoutNewLines, containsString("7. Reasons for decision"));
            assertThat(pdfTextWithoutNewLines, containsString("8. Anything else"));
            assertThat(pdfTextWithoutNewLines, containsString("9. This has been a remote hearing in the form of a triage hearing. Joe Bloggs did not attend the hearing today. No Presenting Officer attended on behalf of the Respondent. Having considered the appeal bundle to page B7 and the requirements of rules 2 and 31 of The Tribunal Procedure (First-tier Tribunal)(Social Entitlement Chamber) Rules 2008 the Tribunal is satisfied that reasonable steps were taken to notify Joe Bloggs of the hearing and that it is in the interests of justice to proceed today."));
            assertThat(pdfTextWithoutNewLines, containsString("10. Any recommendation given below does not form part of the Tribunal's decision and is not binding on the Secretary of State."));
            assertThat(pdfTextWithoutNewLines, containsString(expectedText));
            assertThat(pdfTextWithoutNewLines, not(containsString("11.")));
        }
    }

    @Test
    @Parameters({
        "noRecommendation, The Tribunal makes no recommendation as to when the Department should reassess Joe Bloggs.",
        "doNotReassess, In view of the degree of disability found by the Tribunal\\, and unless the regulations change\\, the Tribunal would recommend that the appellant is not re-assessed.",
        "reassess12, The Tribunal recommends that the Department reassesses Joe Bloggs within 12 months from today's date.",
        "reassess3, The Tribunal recommends that the Department reassesses Joe Bloggs within 3 months from today's date.",
        "doNotReassess3, The Tribunal recommends that the Department does not reassess Joe Bloggs within 3 months from today's date.",
        "doNotReassess18, The Tribunal recommends that the Department does not reassess Joe Bloggs within 18 months from today's date.",
    })
    public void scenario8_allowed_notSupportGroup_LessThan15PointsSch2_Reg29Applies_Reg35Applies_shouldHaveExpectedText_When_ZeroPoints(String code, String expectedText) throws IOException {
        String json = getJsonCallbackForTest("handlers/writefinaldecision/esaDwpReassessTheAwardCallbackZeroPoints.json");
        json = json.replaceAll("DWP_REASSESS_THE_AWARD", code);
        byte[] bytes = callPreviewFinalDecision(json);
        try (PDDocument document = PDDocument.load(bytes)) {
            String pdfText = new PDFTextStripper().getText(document);
            String pdfTextWithoutNewLines = replaceNewLines(pdfText);
            assertThat(pdfTextWithoutNewLines, containsString("1. The appeal is allowed."));
            assertThat(pdfTextWithoutNewLines, containsString("2. The decision made by the Secretary of State on 17/11/2020 is set aside."));
            assertThat(pdfTextWithoutNewLines, containsString("3. Joe Bloggs is to be treated as having limited capability for work and for work-related activity."));
            assertThat(pdfTextWithoutNewLines, containsString("4. This is because insufficient points were scored to meet the threshold for the work capability assessment and none of the Schedule 3 activities and descriptors were satisfied, but the tribunal applied regulations 29 and 35 of the Employment and Support Allowance Regulations (ESA) 2008."));
            assertThat(pdfTextWithoutNewLines, containsString("5. In applying the work capability assessment 0 points were scored from the activities and descriptors in Schedule 2 of the ESA Regulations 2008."));
            assertThat(pdfTextWithoutNewLines, containsString("6. The tribunal applied regulations 29 and 35 because there would be a substantial risk to the mental or physical health of any person if the appellant were found not to have limited capability for work and for work-related activity."));
            assertThat(pdfTextWithoutNewLines, containsString("7. Reasons for decision"));
            assertThat(pdfTextWithoutNewLines, containsString("8. Anything else"));
            assertThat(pdfTextWithoutNewLines, containsString("9. This has been a remote hearing in the form of a triage hearing. Joe Bloggs did not attend the hearing today. No Presenting Officer attended on behalf of the Respondent. Having considered the appeal bundle to page B7 and the requirements of rules 2 and 31 of The Tribunal Procedure (First-tier Tribunal)(Social Entitlement Chamber) Rules 2008 the Tribunal is satisfied that reasonable steps were taken to notify Joe Bloggs of the hearing and that it is in the interests of justice to proceed today."));
            assertThat(pdfTextWithoutNewLines, containsString("10. Any recommendation given below does not form part of the Tribunal's decision and is not binding on the Secretary of State."));
            assertThat(pdfTextWithoutNewLines, containsString(expectedText));
            assertThat(pdfTextWithoutNewLines, not(containsString("11.")));
        }
    }

    @Test
    public void scenario10_refused_nonWca() throws IOException {
        String json = getJsonCallbackForTest("handlers/writefinaldecision/esaAllowedNonWcaCallback.json");
        json = json.replaceFirst("allowed", "refused");
        byte[] bytes = callPreviewFinalDecision(json);
        try (PDDocument document = PDDocument.load(bytes)) {
            String pdfText = new PDFTextStripper().getText(document);
            String pdfTextWithoutNewLines = replaceNewLines(pdfText);
            assertThat(pdfTextWithoutNewLines, containsString("1. The appeal is refused."));
            assertThat(pdfTextWithoutNewLines, containsString("2. The decision made by the Secretary of State on 11/11/2020 is confirmed."));
            assertThat(pdfTextWithoutNewLines, containsString("3. Joe Bloggs does not have limited capability for work and cannot be treated as having limited capability for work."));
            assertThat(pdfTextWithoutNewLines, containsString("4. Summary of outcome decision"));
            assertThat(pdfTextWithoutNewLines, containsString("5. Reasons for decision 1"));
            assertThat(pdfTextWithoutNewLines, containsString("6. Reasons for decision 2"));
            assertThat(pdfTextWithoutNewLines, containsString("7. Anything else."));
            assertThat(pdfTextWithoutNewLines, containsString("8. This has been a remote hearing in the form of a telephone hearing. Joe Bloggs attended and the Tribunal considered the appeal bundle to page B7. A Presenting Officer attended on behalf of the Respondent."));
            assertThat(pdfTextWithoutNewLines, not(containsString("9.")));
        }
    }

    @Test
    public void scenario11_allowed_nonWcaAppeal() throws IOException {
        String json = getJsonCallbackForTest("handlers/writefinaldecision/esaAllowedNonWcaCallback.json");
        byte[] bytes = callPreviewFinalDecision(json);
        try (PDDocument document = PDDocument.load(bytes)) {
            String pdfText = new PDFTextStripper().getText(document);
            String pdfTextWithoutNewLines = replaceNewLines(pdfText);
            assertThat(pdfTextWithoutNewLines, containsString("1. The appeal is allowed."));
            assertThat(pdfTextWithoutNewLines, containsString("2. The decision made by the Secretary of State on 11/11/2020 is set aside."));
            assertThat(pdfTextWithoutNewLines, containsString("3. Joe Bloggs has limited capability for work."));
            assertThat(pdfTextWithoutNewLines, containsString("4. Summary of outcome decision"));
            assertThat(pdfTextWithoutNewLines, containsString("5. Reasons for decision 1"));
            assertThat(pdfTextWithoutNewLines, containsString("6. Reasons for decision 2"));
            assertThat(pdfTextWithoutNewLines, containsString("7. Anything else."));
            assertThat(pdfTextWithoutNewLines, containsString("8. This has been a remote hearing in the form of a telephone hearing. Joe Bloggs attended and the Tribunal considered the appeal bundle to page B7. A Presenting Officer attended on behalf of the Respondent."));
            assertThat(pdfTextWithoutNewLines, not(containsString("9.")));
        }
    }

    private String replaceNewLines(String pdfText) {
        return pdfText.replaceAll("-\n", "-").replaceAll("[\\n\\t]", " ").replaceAll("\\s{2,}", " ");
    }

    private byte[] callPreviewFinalDecision(String json) throws IOException {
        HttpResponse httpResponse = sscsMyaBackendRequests.midEvent(new StringEntity(json), "PreviewFinalDecision");
        CcdEventResponse ccdEventResponse = getCcdEventResponse(httpResponse);
        assertThat(httpResponse.getStatusLine().getStatusCode(), is(200));
        assertThat(ccdEventResponse.getData().getWriteFinalDecisionPreviewDocument(), is(not(nullValue())));
        return sscsMyaBackendRequests.toBytes(ccdEventResponse.getData().getWriteFinalDecisionPreviewDocument().getDocumentUrl());
    }

    private CcdEventResponse getCcdEventResponse(HttpResponse httpResponse) throws IOException {
        String response = EntityUtils.toString(httpResponse.getEntity());
        return objectMapper.readValue(response, CcdEventResponse.class);
    }
}
