package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;


import java.util.Arrays;
import java.util.Collections;
import javax.validation.Validator;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.AwardType;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionMidEventValidationHandlerBase;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionMidEventValidationHandlerTestBase;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeService;

@RunWith(JUnitParamsRunner.class)
public class UcWriteFinalDecisionMidEventValidationHandlerTest extends WriteFinalDecisionMidEventValidationHandlerTestBase {

    @Override
    protected String getBenefitType() {
        return "UC";
    }

    @Override
    protected void setValidPointsAndActivitiesScenario(SscsCaseData caseData, String descriptorFlowValue) {
        sscsCaseData.getSscsUcCaseData().setDoesSchedule8Paragraph4Apply(YesNo.NO);
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(
                Arrays.asList("mobilisingUnaided"));

        // < 15 points - correct for these fields
        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionMobilisingUnaidedQuestion("mobilisingUnaided1b");
    }

    @Override
    protected WriteFinalDecisionMidEventValidationHandlerBase createValidationHandler(Validator validator, DecisionNoticeService decisionNoticeService) {
        return new UcWriteFinalDecisionMidEventValidationHandler(validator, decisionNoticeService);
    }

    @Override
    protected void setNoAwardsScenario(SscsCaseData caseData) {

    }

    @Override
    protected void setEmptyActivitiesListScenario(SscsCaseData caseData) {
        caseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(Collections.emptyList());
        caseData.getSscsUcCaseData().setUcWriteFinalDecisionMentalAssessmentQuestion(Collections.emptyList());
    }

    @Override
    protected void setNullActivitiesListScenario(SscsCaseData caseData) {
        caseData.getSscsUcCaseData().setUcWriteFinalDecisionPhysicalDisabilitiesQuestion(null);
        caseData.getSscsUcCaseData().setUcWriteFinalDecisionMentalAssessmentQuestion(null);
    }

    @Test
    public void givenAnEmptySchedule7ActivitiesApplyFlag_thenSetTheFlag() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        assertNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionSchedule7ActivitiesApply());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals("Yes", sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionSchedule7ActivitiesApply());

        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void givenAnSchedule7ActivitiesApplyFlagSetToNo_thenDoNotChangeTheFlag() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsUcCaseData().setUcWriteFinalDecisionSchedule7ActivitiesApply("No");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals("No", sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionSchedule7ActivitiesApply());

        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    @Parameters({
            "Yes, NO",
            "No,YES",
            "null, NO"
    })
    public void givenUcCaseWithWcaAppealFlow_thenSetShowSummaryOfOutcomePage(
            @Nullable String wcaFlow, YesNo expectedShowResult) {

        sscsCaseData.setWcaAppeal(wcaFlow);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(expectedShowResult, response.getData().getShowFinalDecisionNoticeSummaryOfOutcomePage());
    }

    @Test
    @Parameters({"STANDARD_RATE, STANDARD_RATE",})
    @Override
    public void shouldExhibitBenefitSpecificBehaviourWhenAnAnAwardIsGivenAndNoActivitiesSelected(AwardType dailyLiving, AwardType mobility) {

        setValidPointsAndActivitiesScenario(sscsCaseData, "Yes");
        setEmptyActivitiesListScenario(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getWarnings().size());
        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void shouldExhibitBenefitSpecificBehaviourWhenNoAwardsAreGivenAndNoActivitiesAreSelected() {

        setValidPointsAndActivitiesScenario(sscsCaseData, "Yes");
        setNoAwardsScenario(sscsCaseData);
        setEmptyActivitiesListScenario(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getWarnings().size());
        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void shouldExhibitBenefitSpecificBehaviourWhenNoAwardsAreGivenAndNoActivitiesAreSelectedAndEndDateTypeIsSetEndDate() {

        setValidPointsAndActivitiesScenario(sscsCaseData, "Yes");
        setNoAwardsScenario(sscsCaseData);
        setEmptyActivitiesListScenario(sscsCaseData);

        sscsCaseData.setWriteFinalDecisionEndDateType("setEndDate");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
        assertEquals(0, response.getWarnings().size());

        assertEquals("setEndDate", caseDetails.getCaseData().getWriteFinalDecisionEndDateType());

    }

    @Test
    public void shouldExhibitBenefitSpecificBehaviourWhenNoAwardsAreGivenAndNoActivitiesAreSelectedAndEndDateTypeIsIndefinite() {

        setValidPointsAndActivitiesScenario(sscsCaseData, "Yes");
        setEmptyActivitiesListScenario(sscsCaseData);

        sscsCaseData.setWriteFinalDecisionEndDateType("indefinite");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getWarnings().size());
        assertEquals(0, response.getErrors().size());

        assertEquals("indefinite", caseDetails.getCaseData().getWriteFinalDecisionEndDateType());

    }

}