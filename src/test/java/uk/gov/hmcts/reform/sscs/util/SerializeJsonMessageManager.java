package uk.gov.hmcts.reform.sscs.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import lombok.Getter;
import lombok.ToString;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@Getter
@ToString
public enum SerializeJsonMessageManager {

    APPEAL_RECEIVED("tya/appealReceived.json"),
    APPEAL_RECEIVED_CCD("tya/appealReceivedCcd.json"),
    DWP_RESPOND("tya/dwpRespond.json"),
    DWP_RESPOND_CCD("tya/dwpRespondCcd.json"),
    HEARING_BOOKED("tya/hearingBooked.json"),
    HEARING_BOOKED_CCD("tya/hearingBookedCcd.json"),
    ADJOURNED("tya/adjourned.json"),
    ADJOURNED_CCD("tya/adjournedCcd.json"),
    DORMANT("tya/dormant.json"),
    DORMANT_CCD("tya/dormantCcd.json"),
    HEARING("tya/hearing.json"),
    HEARING_CCD("tya/hearingCcd.json"),
    NEW_HEARING_BOOKED("tya/newHearingBooked.json"),
    NEW_HEARING_BOOKED_CCD("tya/newHearingBookedCcd.json"),
    NOT_PAST_HEARING_BOOKED("tya/notPastHearingBooked.json"),
    NOT_PAST_HEARING_BOOKED_CCD("tya/notPastHearingBookedCcd.json"),
    PAST_HEARING_BOOKED("tya/pastHearingBooked.json"),
    PAST_HEARING_BOOKED_CCD("tya/pastHearingBookedCcd.json"),
    DWP_RESPOND_OVERDUE("tya/dwpRespondOverdue.json"),
    DWP_RESPOND_OVERDUE_CCD("tya/dwpRespondOverdueCcd.json"),
    POSTPONED("tya/postponed.json"),
    POSTPONED_CCD("tya/postponedCcd.json"),
    WITHDRAWN("tya/withdrawn.json"),
    WITHDRAWN_CCD("tya/withdrawnCcd.json"),
    CLOSED("tya/closed.json"),
    CLOSED_CCD("tya/closedCcd.json"),
    LAPSED_REVISED("tya/lapsedRevised.json"),
    LAPSED_REVISED_CCD("tya/lapsedRevisedCcd.json"),
    EMPTY_EVENT_CCD("tya/emptyEventCcd.json"),
    NO_EVENTS_CCD("tya/noEventsCcd.json"),
    APPEAL_CREATED("tya/appealCreated.json"),
    APPEAL_CREATED_CCD("tya/appealCreatedCcd.json"),
    MISSING_HEARING_CCD("tya/missingHearingBookedCcd.json"),
    MISSING_HEARING("tya/missingHearingBooked.json"),
    APPEAL_WITH_HEARING_TYPE("tya/appealWithHearingType.json"),
    APPEAL_WITH_HEARING_TYPE_CCD("tya/appealWithHearingTypeCcd.json"),
    APPEAL_WITH_WANTS_TO_ATTEND_YES("tya/appealWithWantsToAttendYes.json"),
    APPEAL_WITH_WANTS_TO_ATTEND_YES_CCD("tya/appealWithWantsToAttendYesCcd.json"),
    APPEAL_WITH_WANTS_TO_ATTEND_NO("tya/appealWithWantsToAttendYes.json"),
    APPEAL_WITH_WANTS_TO_ATTEND_NO_CCD("tya/appealWithWantsToAttendYesCcd.json"),
    APPEAL_WITH_WANTS_TO_ATTEND_IS_NOT_PRESENT("tya/appealWithWantsToAttendFieldNotPresent.json"),
    APPEAL_WITH_WANTS_TO_ATTEND_IS_NOT_PRESENT_CCD("tya/appealWithWantsToAttendFieldNotPresentInCcd.json"),
    APPEAL_WITH_NO_HEARING_OPTIONS("tya/appealWithNoHearingOptions.json"),
    APPEAL_WITH_NO_HEARING_OPTIONS_IN_CCD("tya/appealWithNoHearingOptionsInCcd.json"),
    HEARING_BOOKED_PAPER_CASE("tya/hearingBookedPaperCase.json"),
    HEARING_BOOKED_PAPER_CASE_CCD("tya/hearingBookedPaperCaseCcd.json"),
    SESSION_SAMPLE("drafts/session-sample.json");

    private final String serializedMessage;

    SerializeJsonMessageManager(String fileName) {
        this.serializedMessage = getSerialisedMessage(fileName);
    }

    private String getSerialisedMessage(String fileName) {
        try {

            ClassLoader classLoader = getClass().getClassLoader();
            File file = new File(classLoader.getResource(fileName).getFile());
            return new String(Files.readAllBytes(file.toPath()));

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    public SscsCaseData getDeserializeMessage() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        try {
            return mapper.readValue(this.serializedMessage, SscsCaseData.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
