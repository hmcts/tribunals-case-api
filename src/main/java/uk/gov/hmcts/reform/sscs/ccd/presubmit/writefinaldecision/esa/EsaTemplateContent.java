package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.scenarios.EsaScenario;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateContent;

public abstract class EsaTemplateContent extends WriteFinalDecisionTemplateContent {

    public String getDoesNotHaveLimitedCapabilityForWorkSentence(String appellantName) {
        return appellantName + " does not have limited capability for work and cannot be treated as having limited capability for work.";
    }

    public String getDoesNotHaveLimitedCapabilityForWorkNoSchedule3Sentence(String appellantName) {
        return appellantName + " does not have limited capability for work-related activity because no descriptor from Schedule 3 applied.  Regulation 35 did not apply.";
    }

    public String getDoesHaveLimitedCapabilityForWorkSentence(String appellantName, boolean isTreatedLimitedCapability, boolean includeWorkRelatedActivities, boolean isWorkRelatedActivitiesLimited) {
        return appellantName + (isTreatedLimitedCapability ? " is to be treated as having" : " has") + " limited capability for work" + (includeWorkRelatedActivities ? " and " + (isWorkRelatedActivitiesLimited ? "has limited capability " : "") + "for work-related activity." : ".");
    }

    public String getLimitedCapabilityForWorkRelatedSentence(String appellantName, boolean isTreatedLimitedCapability) {
        return appellantName + (isTreatedLimitedCapability ? " is to be treated as having" : " has") + " limited capability for work-related activity.";
    }

    public String getContinuesToHaveWorkRelatedSentenceButNotLimitedWorkRelatedActivity(String appellantName) {
        return appellantName + " continues to have limited capability for work but does not have limited capability for "
                + "work-related activity. This is because no descriptor from Schedule 3 of the Employment and "
                + "Support Allowance (ESA) Regulations 2008 applied. Regulation 35 did not apply. The Secretary of State "
                + "has accepted that " + appellantName + " has limited capability for work. This was not in issue.";
    }

    public String getSecretaryOfStateAcceptsHasLimitedCapabilityForWorkSentence(String appellantName, boolean work) {
        return "The Secretary of State has accepted that " + appellantName + " has limited capability for "
                + (work ? "work." : "work related activity.") + " This was not in issue.";
    }

    public String getHasLimitedCapabilityForWorkNoSchedule3SentenceReg35Applies() {
        return "No descriptor from Schedule 3 of the Employment and Support Allowance (ESA) Regulations 2008 was satisfied but regulation 35 applied.";
    }

    public String getSchedule2PointsSentence(Integer points, Boolean isSufficient, List<Descriptor> esaSchedule2Descriptors) {
        String madeUpAsFollowsSuffix = esaSchedule2Descriptors == null || esaSchedule2Descriptors.isEmpty() ? "." : " made up as follows:";
        return "In applying the work capability assessment " + points + (points == 1 ? " point was" : " points were")
            + " scored from the activities and descriptors in Schedule "
            + "2 of the ESA Regulations 2008" + (isSufficient != null && isSufficient.booleanValue() ? madeUpAsFollowsSuffix
            : ". This is insufficient to meet the "
            + "threshold for the test. Regulation 29 of the Employment and Support Allowance (ESA) Regulations 2008 did not apply.");
    }

    public String getInsufficientPointsSentenceRegulation29Applied() {
        return "This is because insufficient points were scored to meet the threshold for the work capability assessment, "
                + "but regulation 29 of the Employment and Support Allowance (ESA) Regulations 2008 applied.";
    }

    public String getInsufficientPointsSentenceRegulation29AndRegulation35Applied() {
        return "This is because insufficient points were scored to meet the threshold for the work capability assessment "
                + "and none of the Schedule 3 activities and descriptors were satisfied, but the tribunal applied regulations 29 and 35 of the Employment and Support Allowance Regulations (ESA) 2008.";
    }

    public String getInsufficientPointsSentenceRegulation29AndSchedule3Applied() {
        return "This is because insufficient points were scored under Schedule 2 of the Employment and Support Allowance (ESA) Regulations 2008 to meet the threshold for the Work Capability Assessment, but the tribunal applied regulation 29.";
    }

    public String getRegulation29And35DiseaseOrDisablementSentence(boolean isRegulation29Applied, boolean isRegulation35Applied) {
        return "The tribunal applied regulation" + (isRegulation29Applied && isRegulation35Applied ? "s" : "")
                + (isRegulation29Applied ? " 29 " : "")
                + (isRegulation29Applied && isRegulation35Applied ? "and" : "")
                + (isRegulation35Applied ? " 35 " : "")
            + "because there would be a substantial risk to the mental or physical health of any person if the appellant were found not to have limited "
            + "capability for work"
            + (isRegulation35Applied ? " and for work-related activity." : ".");
    }

    public String getSchedule3AppliesParagraph(List<Descriptor> descriptors) {
        if (descriptors != null && descriptors.size() == 1) {
            return "The following activity and descriptor from Schedule 3 applied:";
        } else {
            return "The following activities and descriptors from Schedule 3 applied:";
        }
    }

    public List<String> getFaceToFaceTelephoneVideoHearingTypeSentences(String hearingType, String appellantName, String bundlePage,
                                                                 boolean appellantAttended, boolean presentingOfifficerAttened) {
        if (appellantAttended) {
            if (StringUtils.equalsIgnoreCase("faceToFace", hearingType)) {
                return Arrays.asList("This has been an oral (face to face) hearing. "
                        + getAppellantAttended(hearingType, appellantName, presentingOfifficerAttened, bundlePage));
            } else {
                return Arrays.asList("This has been a remote hearing in the form of a " + hearingType + " hearing. "
                        + getAppellantAttended(hearingType, appellantName, presentingOfifficerAttened, bundlePage));
            }
        } else {
            if (StringUtils.equalsIgnoreCase("faceToFace", hearingType)) {
                return Arrays.asList(appellantName + " requested an oral hearing but did not attend today. "
                        + (presentingOfifficerAttened ? "A " : "No ") + "Presenting Officer attended on behalf of the Respondent.",
                        getConsideredParagraph(bundlePage, appellantName));
            } else {
                return Arrays.asList("This has been a remote hearing in the form of a " + hearingType + " hearing. " + appellantName + " did not attend the hearing today. "
                        + (presentingOfifficerAttened ? "A" : "No") + " Presenting Officer attended on behalf of the Respondent.",
                        getConsideredParagraph(bundlePage, appellantName));
            }
        }
    }

    public abstract EsaScenario getScenario();
}
