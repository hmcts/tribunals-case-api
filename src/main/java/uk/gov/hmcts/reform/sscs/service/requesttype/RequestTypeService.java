package uk.gov.hmcts.reform.sscs.service.requesttype;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.model.tya.HearingRecording;
import uk.gov.hmcts.reform.sscs.model.tya.HearingRecordingRequest;
import uk.gov.hmcts.reform.sscs.model.tya.HearingRecordingResponse;
import uk.gov.hmcts.reform.sscs.service.OnlineHearingService;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class RequestTypeService {
    private final OnlineHearingService onlineHearingService;


    public RequestTypeService(OnlineHearingService onlineHearingService) {
        this.onlineHearingService = onlineHearingService;
    }

    public HearingRecordingResponse findHearingRecordings(String identifier) {
        Optional<SscsCaseDetails> caseDetails = onlineHearingService.getCcdCaseByIdentifier(identifier);
        return caseDetails.map(x -> mapToHearingRecordingTemp(x.getData())).orElse(new HearingRecordingResponse());
    }

    //FIXME remove this
    private HearingRecordingResponse mapToHearingRecordingTemp(SscsCaseData sscsCaseData) {
        return HearingRecordingResponse.builder()
                .releasedHearingRecordings(List.of(HearingRecordingRequest.builder()
                        .hearingId("1")
                        .hearingDate("02/08/2020")
                        .hearingTime("02:45")
                        .venue("White House")
                        .hearingRecordings(List.of(HearingRecording.builder()
                                .fileName("Test file1.mp3")
                                .build(),
                                HearingRecording.builder()
                                        .fileName("Test file1.mp3")
                                        .build()))
                        .build(),
                        HearingRecordingRequest.builder()
                                .hearingId("1")
                                .hearingDate("02/08/2020")
                                .hearingTime("02:45")
                                .venue("White House")
                                .hearingRecordings(List.of(HearingRecording.builder()
                                        .fileName("Test file1.mp3")
                                        .build()))
                                .build())
                        )
                .outstandingHearingRecordings(List.of(HearingRecordingRequest.builder()
                        .hearingId("2")
                        .hearingDate("02/08/2020")
                        .hearingTime("02:45")
                        .venue("White House 2")
                        .build(),
                        HearingRecordingRequest.builder()
                                .hearingId("2")
                                .hearingDate("02/08/2020")
                                .hearingTime("02:45")
                                .venue("White House 2")
                                .build()))
                .requestableHearingRecordings(List.of(HearingRecordingRequest.builder()
                        .hearingId("1")
                        .hearingDate("02/08/2020")
                        .hearingTime("02:45")
                        .venue("White House 3")
                        .build(),
                        HearingRecordingRequest.builder()
                                .hearingId("2")
                                .hearingDate("02/08/2020")
                                .hearingTime("02:45")
                                .venue("White House 2")
                                .build()))
                .build();
    }

    private HearingRecordingResponse mapToHearingRecording(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getHearings() == null || sscsCaseData.getHearings().isEmpty()) {
            return new HearingRecordingResponse();
        } else {
            //FIXME pass request party
            List<HearingRecordingRequest> releasedRecordings = sscsCaseData.getSscsHearingRecordingCaseData().getReleasedHearings()
                    .stream()
                    .filter(request -> UploadParty.APPELLANT.equals(request.getValue().getRequestingParty()))
                    .map(request -> selectHearingRecordings(request))
                    .collect(Collectors.toList());

            List<HearingRecordingRequest> requestedRecordings = sscsCaseData.getSscsHearingRecordingCaseData().getRequestedHearings()
                    .stream()
                    .filter(request -> UploadParty.APPELLANT.equals(request.getValue().getRequestingParty()))
                    .map(request -> selectHearingRecordings(request))
                    .collect(Collectors.toList());

            List<String> allRequestedHearingIds = Stream.of(releasedRecordings, requestedRecordings)
                    .flatMap(Collection::stream)
                    .map(r -> r.getHearingId())
                    .collect(Collectors.toList());

            List<HearingRecordingRequest> requestabledRecordings = sscsCaseData.getHearings().stream()
                    .filter(hearing -> isHearingWithRecording(hearing, sscsCaseData.getSscsHearingRecordingCaseData()))
                    .filter(hearing -> !allRequestedHearingIds.contains(hearing.getValue().getHearingId()))
                    .map(hearing -> selectHearingRecordings(hearing, sscsCaseData.getSscsHearingRecordingCaseData()))
                    .collect(Collectors.toList());

            return new HearingRecordingResponse(releasedRecordings, requestedRecordings, requestabledRecordings);
        }
    }

    private HearingRecordingRequest selectHearingRecordings(uk.gov.hmcts.reform.sscs.ccd.domain.HearingRecordingRequest request) {
        return HearingRecordingRequest.builder()
                .hearingId(request.getValue().getRequestedHearing())
                .hearingDate(request.getValue().getDateRequested())
                .venue(request.getValue().getRequestedHearingName())
                .hearingRecordings(request.getValue().getSscsHearingRecordingList()
                        .stream()
                        .map(r -> HearingRecording.builder()
                                .fileName(r.getValue().getFileName())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    private HearingRecordingRequest selectHearingRecordings(Hearing hearing, SscsHearingRecordingCaseData hearingRecordingsData) {
        return HearingRecordingRequest.builder()
                .hearingId(hearing.getValue().getHearingId())
                .hearingDate(hearing.getValue().getHearingDate())
                .hearingTime(hearing.getValue().getTime())
                .venue(hearing.getValue().getVenue().getName())
                .hearingRecordings(hearingRecordingsData.getSscsHearingRecordings().stream()
                    .filter(r -> r.getValue().getHearingId().equals(hearing.getValue().getHearingId()))
                    .map(r -> HearingRecording.builder()
                            .fileName(r.getValue().getFileName())
                            .build())
                        .collect(Collectors.toList()))
                .build();


    }

    private boolean isHearingWithRecording(Hearing hearing, SscsHearingRecordingCaseData hearingRecordingsData) {
        List<SscsHearingRecording> sscsHearingRecordings = hearingRecordingsData.getSscsHearingRecordings();

        if (sscsHearingRecordings != null) {
            return sscsHearingRecordings.stream().anyMatch(r -> r.getValue().getHearingId().equals(hearing.getValue().getHearingId()));
        }
        return false;
    }

    public boolean requestHearingRecordings(String identifier, List<String> hearingIds) {
        log.info("Hearing recordings request for {}", hearingIds);
        return true;
    }
}
