package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.scenarios;

import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.PipTemplateContent;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public class PipScenarioDTest {

    @Test
    public void testScenario1() {

        List<Descriptor> dailyLivingDescriptors =
                Arrays.asList(Descriptor.builder()
                                .activityQuestionValue("1. Preparing Food")
                                .activityAnswerValue("1")
                                .activityAnswerLetter("d").activityAnswerPoints(2).build(),
                    Descriptor.builder()
                        .activityQuestionValue("2. Taking Nutrition")
                        .activityAnswerValue("2")
                        .activityAnswerLetter("d").activityAnswerPoints(4).build());

        WriteFinalDecisionTemplateBody body =
                WriteFinalDecisionTemplateBody.builder()
                        .hearingType("faceToFace")
                        .attendedHearing(true)
                        .presentingOfficerAttended(false)
                        .dateOfDecision("2020-09-20")
                        .startDate("2020-12-17")
                        .dailyLivingIsEntited(false)
                        .mobilityIsEntited(false)
                        .dailyLivingNumberOfPoints(6)
                        .dailyLivingAwardRate("noAward")
                        .mobilityAwardRate("notConsidered")
                    .pageNumber("A1")
                        .appellantName("Felix Sydney")
                        .reasonsForDecision(Arrays.asList("My first reasons", "My second reasons"))
                        .anythingElse("Something else")
                        .dailyLivingDescriptors(dailyLivingDescriptors).build();

        PipTemplateContent content = PipScenario.SCENARIO_1.getContent(body);


        String expectedContent = "The appeal is refused.\n"
                + "\n"
                + "The decision made by the Secretary of State on 20/09/2020 in respect of Personal Independence Payment is confirmed.\n"
                + "\n"
                + "Felix Sydney is not entitled to the daily living component from 17/12/2020. They score 6 points. This is insufficient to meet the threshold for the test.\n"
                + "\n"
                + "1. Preparing Food\td.1\t2\n"
                + "2. Taking Nutrition\td.2\t4"
                + "\n"
                + "\n\n"
                + "Only the daily living component was in issue on this appeal and the mobility component was not considered. \n"
                + "\n"
                + "My first reasons\n"
                + "\n"
                + "My second reasons\n"
                + "\n"
                + "Something else\n"
                + "\n"
                + "This has been an oral (face to face) hearing. Felix Sydney attended the hearing today and the Tribunal considered the appeal bundle to page A1. No Presenting Officer attended on behalf of the Respondent.\n"
                + "\n";

        Assert.assertEquals(9, content.getComponents().size());

        Assert.assertEquals(expectedContent, content.toString());

    }
    
}