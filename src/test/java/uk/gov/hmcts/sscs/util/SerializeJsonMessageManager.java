package uk.gov.hmcts.sscs.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import lombok.Getter;
import lombok.ToString;

import uk.gov.hmcts.sscs.model.ccd.CaseData;

@Getter
@ToString
public enum SerializeJsonMessageManager {

    APPEAL_RECEIVED("appealReceived.json"),
    APPEAL_RECEIVED_CCD("appealReceivedCcd.json"),
    DWP_RESPOND("dwpRespond.json"),
    DWP_RESPOND_CCD("dwpRespondCcd.json"),
    HEARING_BOOKED("hearingBooked.json"),
    HEARING_BOOKED_CCD("hearingBookedCcd.json"),
    ADJOURNED("adjourned.json"),
    ADJOURNED_CCD("adjournedCcd.json"),
    DORMANT("dormant.json"),
    DORMANT_CCD("dormantCcd.json"),
    HEARING("hearing.json"),
    HEARING_CCD("hearingCcd.json"),
    NEW_HEARING_BOOKED("newHearingBooked.json"),
    NEW_HEARING_BOOKED_CCD("newHearingBookedCcd.json");

    private final String serializedMessage;

    SerializeJsonMessageManager(String fileName) {
        this.serializedMessage = getSerialisedMessage(fileName,
                "src/test/resources/tya/");
    }

    private String getSerialisedMessage(String fileName, String path) {
        try {
            return new String(Files.readAllBytes(Paths.get(path + fileName)));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    public CaseData getDeserializeMessage() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        try {
            return mapper.readValue(this.serializedMessage, CaseData.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
