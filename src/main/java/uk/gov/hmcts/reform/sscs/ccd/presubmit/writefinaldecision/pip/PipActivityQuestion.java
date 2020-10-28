package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip;

import java.util.function.Function;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityQuestion;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityType;

/**
 * Enum to encapsulate binding between an activity question key, and the corresponding getter method on SscsCaseData that yields that answer to that question.
 */
public enum PipActivityQuestion implements ActivityQuestion {

    PREPARING_FOOD("preparingFood", "Preparing food", PipActivityType.DAILY_LIVING, SscsCaseData::getPipWriteFinalDecisionPreparingFoodQuestion),
    TAKING_NUTRITION("takingNutrition", "Taking nutrition",  PipActivityType.DAILY_LIVING, SscsCaseData::getPipWriteFinalDecisionTakingNutritionQuestion),
    MANAGING_THERAPY("managingTherapy", "Managing therapy or monitoring a health condition", PipActivityType.DAILY_LIVING, SscsCaseData::getPipWriteFinalDecisionManagingTherapyQuestion),
    WASHING_AND_BATHING("washingAndBathing", "Washing and bathing", PipActivityType.DAILY_LIVING, SscsCaseData::getPipWriteFinalDecisionWashAndBatheQuestion),
    MANAGING_TOILET_NEEDS("managingToiletNeeds", "Managing toilet needs or incontinence", PipActivityType.DAILY_LIVING, SscsCaseData::getPipWriteFinalDecisionManagingToiletNeedsQuestion),
    DRESSING_AND_UNDRESSING("dressingAndUndressing", "Dressing and undressing", PipActivityType.DAILY_LIVING, SscsCaseData::getPipWriteFinalDecisionDressingAndUndressingQuestion),
    COMMUNICATING("communicating", "Communicating", PipActivityType.DAILY_LIVING, SscsCaseData::getPipWriteFinalDecisionCommunicatingQuestion),
    READING_AND_UNDERSTANDING("readingUnderstanding", "Reading and understanding signs, symbols and words", PipActivityType.DAILY_LIVING, SscsCaseData::getPipWriteFinalDecisionReadingUnderstandingQuestion),
    ENGAGING_WITH_OTHERS("engagingWithOthers", "Engaging with other people face to face", PipActivityType.DAILY_LIVING, SscsCaseData::getPipWriteFinalDecisionEngagingWithOthersQuestion),
    MAKING_BUDGETING_DECISIONS("budgetingDecisions", "Making budgeting decisions", PipActivityType.DAILY_LIVING, SscsCaseData::getPipWriteFinalDecisionBudgetingDecisionsQuestion),
    PLANNING_AND_FOLLOWING_JOURNEYS("planningAndFollowing", "Planning and following journeys", PipActivityType.MOBILITY, SscsCaseData::getPipWriteFinalDecisionPlanningAndFollowingQuestion),
    MOVING_AROUND("movingAround", "Moving around", PipActivityType.MOBILITY, SscsCaseData::getPipWriteFinalDecisionMovingAroundQuestion);

    final String key;
    final String value;
    final ActivityType activityType;
    final Function<SscsCaseData, String> answerExtractor;

    PipActivityQuestion(String key, String value, ActivityType activityType, Function<SscsCaseData, String> answerExtractor) {
        this.key = key;
        this.answerExtractor = answerExtractor;
        this.activityType = activityType;
        this.value = value;
    }

    public static PipActivityQuestion getByKey(String key) {
        for (PipActivityQuestion mapping : PipActivityQuestion.values()) {
            if (mapping.key.equals(key)) {
                return mapping;
            }
        }
        throw new IllegalArgumentException("Unknown ActivityQuestion for question key:" + key);
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    @Override
    public ActivityType getActivityType() {
        return activityType;
    }

    public Function<SscsCaseData, String> getAnswerExtractor() {
        return answerExtractor;
    }
}
