package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

import static java.util.Optional.empty;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.splitByCharacterTypeCamelCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityQuestion;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityQuestionLookup;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.AwardType;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionPreviewDecisionServiceBase;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.scenarios.EsaScenario;
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody.NoticeIssuedTemplateBodyBuilder;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody.WriteFinalDecisionTemplateBodyBuilder;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeOutcomeService;
import uk.gov.hmcts.reform.sscs.service.EsaDecisionNoticeOutcomeService;
import uk.gov.hmcts.reform.sscs.service.EsaDecisionNoticeQuestionService;

@Slf4j
@Component
public class EsaWriteFinalDecisionPreviewDecisionService extends WriteFinalDecisionPreviewDecisionServiceBase {

    private EsaDecisionNoticeQuestionService esaDecisionNoticeQuestionService;

    @Autowired
    public EsaWriteFinalDecisionPreviewDecisionService(GenerateFile generateFile, IdamClient idamClient,
        EsaDecisionNoticeQuestionService decisionNoticeQuestionService, EsaDecisionNoticeOutcomeService outcomeService, DocumentConfiguration documentConfiguration) {
        super(generateFile, idamClient, decisionNoticeQuestionService, outcomeService, documentConfiguration);
        this.esaDecisionNoticeQuestionService = decisionNoticeQuestionService;
    }

    @Override
    public String getBenefitType() {
        return "ESA";
    }

    @Override
    protected void setTemplateContent(DecisionNoticeOutcomeService outcomeService, PreSubmitCallbackResponse<SscsCaseData> response,
        NoticeIssuedTemplateBodyBuilder builder, SscsCaseData caseData,
        WriteFinalDecisionTemplateBody payload) {


        if ("Yes".equalsIgnoreCase(caseData.getWriteFinalDecisionGenerateNotice())) {

            // Validate here for ESA instead of only validating on submit.
            // This ensures that we know we can obtain a valid allowed or refused condition below
            outcomeService.validate(response, caseData);
            if (response.getErrors().isEmpty()) {

                // If validation has produced no errors, we know that we can get an allowed/refused condition.
                Optional<EsaAllowedOrRefusedCondition> condition = EsaPointsRegulationsAndSchedule3ActivitiesCondition
                    .getPassingAllowedOrRefusedCondition(decisionNoticeQuestionService, caseData);
                if (condition.isPresent()) {
                    EsaScenario scenario = condition.get().getEsaScenario(caseData);
                    EsaTemplateContent templateContent = scenario.getContent(payload);
                    builder.writeFinalDecisionTemplateContent(templateContent);
                } else {
                    // Should never happen.
                    log.error("Unable to obtain a valid scenario before preview - Something has gone wrong for caseId: ", caseData.getCcdCaseId());
                    response.addError("Unable to obtain a valid scenario - something has gone wrong");
                }
            }
        }
    }

    @Override
    protected void setEntitlements(WriteFinalDecisionTemplateBodyBuilder builder, SscsCaseData caseData) {

        if ("Yes".equalsIgnoreCase(caseData.getWriteFinalDecisionGenerateNotice())) {
            builder.esaIsEntited(false);
            builder.esaAwardRate(null);
            Optional<AwardType> esaAwardTypeOptional = caseData.isWcaAppeal() ? EsaPointsRegulationsAndSchedule3ActivitiesCondition
                .getTheSinglePassingPointsConditionForSubmittedActivitiesAndPoints(decisionNoticeQuestionService, caseData).getAwardType() : empty();
            if (!esaAwardTypeOptional.isEmpty()) {
                String esaAwardType = esaAwardTypeOptional.get().getKey();
                if (esaAwardType != null) {
                    builder.esaAwardRate(join(
                        splitByCharacterTypeCamelCase(esaAwardType), ' ').toLowerCase());
                }

                if (AwardType.LOWER_RATE.getKey().equals(esaAwardType)
                    || AwardType.HIGHER_RATE.getKey().equals(esaAwardType)) {
                    builder.esaIsEntited(true);
                }
            }
        }
    }

    protected List<Descriptor> getEsaSchedule2DescriptorsFromQuestionKeys(SscsCaseData caseData, List<String> questionKeys) {
        return getDescriptorsFromQuestionKeys(key -> esaDecisionNoticeQuestionService.extractQuestionFromKey(EsaActivityQuestionKey.getByKey(key)), caseData, questionKeys);
    }

    @Override
    protected void setDescriptorsAndPoints(WriteFinalDecisionTemplateBodyBuilder builder, SscsCaseData caseData) {
        List<Descriptor> allSchedule2Descriptors = new ArrayList<>();
        List<String> physicalDisabilityAnswers = EsaActivityType.PHYSICAL_DISABILITIES.getAnswersExtractor().apply(caseData);
        if (physicalDisabilityAnswers != null) {
            List<Descriptor> physicalDisablityDescriptors = getEsaSchedule2DescriptorsFromQuestionKeys(caseData, physicalDisabilityAnswers);
            allSchedule2Descriptors.addAll(physicalDisablityDescriptors);
        }
        List<String> mentalAssessmentAnswers = EsaActivityType.MENTAL_ASSESSMENT.getAnswersExtractor().apply(caseData);
        if (mentalAssessmentAnswers != null) {
            List<Descriptor> mentalAssessmentDescriptors = getEsaSchedule2DescriptorsFromQuestionKeys(caseData, mentalAssessmentAnswers);
            allSchedule2Descriptors.addAll(mentalAssessmentDescriptors);
        }

        if (allSchedule2Descriptors.isEmpty()) {
            builder.esaSchedule2Descriptors(null);
            builder.esaNumberOfPoints(null);
        } else {
            builder.esaSchedule2Descriptors(allSchedule2Descriptors);
            int numberOfPoints = allSchedule2Descriptors.stream().mapToInt(Descriptor::getActivityAnswerPoints).sum();
            if (EsaPointsCondition.POINTS_GREATER_OR_EQUAL_TO_FIFTEEN.getPointsRequirementCondition().test(numberOfPoints)) {
                caseData.setDoesRegulation29Apply(null);
            }
            builder.esaNumberOfPoints(numberOfPoints);
        }
        if (caseData.getSchedule3Selections() != null && !caseData.getSchedule3Selections().isEmpty()) {
            builder.esaSchedule3Descriptors(getEsaSchedule3DescriptorsFromQuestionKeys(caseData, caseData.getSchedule3Selections()));
        }
        builder.regulation29Applicable(caseData.getDoesRegulation29Apply() == null ? null :  caseData.getDoesRegulation29Apply().toBoolean());
        builder.regulation35Applicable(caseData.getDoesRegulation35Apply() == null ? null :  caseData.getDoesRegulation35Apply().toBoolean());
        builder.supportGroupOnly(caseData.isSupportGroupOnlyAppeal());
    }

    protected List<Descriptor> getEsaSchedule3DescriptorsFromQuestionKeys(SscsCaseData caseData, List<String> questionKeys) {
        return getQuestionOnlyDescriptorsFromQuestionKeys(key -> esaDecisionNoticeQuestionService.extractQuestionFromKey(EsaSchedule3QuestionKey.getByKey(key)), caseData, questionKeys);
    }

    protected List<Descriptor> getQuestionOnlyDescriptorsFromQuestionKeys(ActivityQuestionLookup activityQuestionlookup, SscsCaseData caseData, List<String> questionKeys) {
        
        List<Descriptor> descriptors = questionKeys
            .stream().map(q ->
                buildDescriptorFromActivityQuestion(activityQuestionlookup.getByKey(q))).collect(Collectors.toList());

        // FIXME Extract sort order from question text
        //descriptors.sort(new DescriptorLexicographicalComparator());

        return descriptors;
    }

    protected Descriptor buildDescriptorFromActivityQuestion(ActivityQuestion activityQuestion) {
        return Descriptor.builder()
            .activityQuestionValue(activityQuestion.getValue())
            .build();
    }
}