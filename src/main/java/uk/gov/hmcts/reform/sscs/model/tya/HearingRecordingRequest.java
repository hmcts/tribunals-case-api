package uk.gov.hmcts.reform.sscs.model.tya;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class HearingRecordingRequest {
    private String hearingId;
    private String venue;
    private String hearingDate;
    private String hearingTime;
    private List<HearingRecording> hearingRecordings;
}
