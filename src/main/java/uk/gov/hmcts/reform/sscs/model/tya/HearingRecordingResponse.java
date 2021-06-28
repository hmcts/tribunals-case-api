package uk.gov.hmcts.reform.sscs.model.tya;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class HearingRecordingResponse {
    private List<HearingRecordingRequest> releasedHearingRecordings;
    private List<HearingRecordingRequest> outstandingHearingRecordings;
    private List<HearingRecordingRequest> requestableHearingRecordings;
}
