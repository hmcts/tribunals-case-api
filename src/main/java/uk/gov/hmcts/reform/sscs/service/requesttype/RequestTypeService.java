package uk.gov.hmcts.reform.sscs.service.requesttype;

import com.rometools.utils.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRecordingRequest;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.model.tya.HearingRecording;
import uk.gov.hmcts.reform.sscs.model.tya.HearingRecordingResponse;
import uk.gov.hmcts.reform.sscs.service.OnlineHearingService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RequestTypeService {
    private final OnlineHearingService onlineHearingService;


    public RequestTypeService(OnlineHearingService onlineHearingService) {
        this.onlineHearingService = onlineHearingService;
    }

    public List<HearingRecordingResponse> findHearingRecordings(String identifier) {
        Optional<SscsCaseDetails> caseDetails = onlineHearingService.getCcdCaseByIdentifier(identifier);
        return caseDetails.map(x -> mapToHearingRecording(x)).orElse(Collections.emptyList());
    }

    private List<HearingRecordingResponse> mapToHearingRecording(SscsCaseData sscsCaseData) {
        List<HearingRecordingRequest> requestedHearingsCollection = sscsCaseData.getSscsHearingRecordingCaseData().getRequestedHearings();
        List<HearingRecordingRequest> releasedHearingsCollection = sscsCaseData.getSscsHearingRecordingCaseData().getReleasedHearings();

        sscsCaseData.getHearings().stream()
                .filter(hearing -> isHearingWithRecording(hearing, sscsCaseData.getSscsHearingRecordingCaseData()))
                .map(hearing -> new DynamicListItem(hearing.getValue().getHearingId(), selectHearing(hearing)))
                .collect(Collectors.toList());

        List<HearingRecordingResponse> sscsHearingRecordings =
                caseDetails.getData().getSscsHearingRecordingCaseData().getSscsHearingRecordings();

        if (Lists.isEmpty(sscsHearingRecordings)) {
            return Collections.emptyList();
        } else {
            return sscsHearingRecordings.stream().map(x -> new HearingRecording(
                    x.getValue().getHearingType(),
                    x.getValue().getHearingDate(),
                    x.getValue().getFileName(),
                    x.getValue().getDocumentLink()
            )).collect(Collectors.toList());
        }
    }
}
