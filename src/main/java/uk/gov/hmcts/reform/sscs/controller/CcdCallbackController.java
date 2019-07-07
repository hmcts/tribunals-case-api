package uk.gov.hmcts.reform.sscs.controller;

import static org.springframework.http.ResponseEntity.ok;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.service.AuthorisationService.SERVICE_AUTHORISATION_HEADER;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackDispatcher;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;

@RestController
@Slf4j
public class CcdCallbackController {

    private final AuthorisationService authorisationService;
    private final PreSubmitCallbackDispatcher dispatcher;
    private final SscsCaseCallbackDeserializer deserializer;
    private final CcdService ccdService;
    private final IdamService idamService;

    @Autowired
    public CcdCallbackController(AuthorisationService authorisationService, SscsCaseCallbackDeserializer deserializer,
                                 PreSubmitCallbackDispatcher dispatcher, CcdService ccdService,
                                 IdamService idamService) {
        this.authorisationService = authorisationService;
        this.deserializer = deserializer;
        this.dispatcher = dispatcher;
        this.ccdService = ccdService;
        this.idamService = idamService;
    }

    @PostMapping(path = "/ccdAboutToStart")
    public ResponseEntity<PreSubmitCallbackResponse<SscsCaseData>> ccdAboutToStart(
        @RequestHeader(SERVICE_AUTHORISATION_HEADER) String serviceAuthHeader,
        @RequestBody String message) {
        Callback<SscsCaseData> callback = deserializer.deserialize(message);
        log.info("About to start sscs case callback `{}` received for Case ID `{}`", callback.getEvent(), callback.getCaseDetails().getId());

        authorisationService.authorise(serviceAuthHeader);

        return performRequest(ABOUT_TO_START, callback);
    }

    @PostMapping(path = "/ccdAboutToSubmit")
    public ResponseEntity<PreSubmitCallbackResponse<SscsCaseData>> ccdAboutToSubmit(
        @RequestHeader(SERVICE_AUTHORISATION_HEADER) String serviceAuthHeader,
        @RequestBody String message) {
        Callback<SscsCaseData> callback = deserializer.deserialize(message);
        log.info("About to submit sscs case callback `{}` received for Case ID `{}`", callback.getEvent(), callback.getCaseDetails().getId());

        authorisationService.authorise(serviceAuthHeader);
        return performRequest(ABOUT_TO_SUBMIT, callback);
    }

    @PostMapping(path = "/ccdSubmittedEvent")
    public void ccdSubmittedEvent(
        @RequestHeader(SERVICE_AUTHORISATION_HEADER) String serviceAuthHeader,
        @RequestBody String message) {
        validateRequest(serviceAuthHeader, message);
        processCcdSubmittedEvent(serviceAuthHeader, message);
    }

    private void validateRequest(String serviceAuthHeader, String message) {
        Preconditions.checkNotNull(message);
        Preconditions.checkNotNull(serviceAuthHeader);
    }

    private void processCcdSubmittedEvent(String serviceAuthHeader, String message) {
        Callback<SscsCaseData> callback = deserializer.deserialize(message);
        log.info("processing SubmittedEvent callback for`{}` event and Case ID `{}`", callback.getEvent(),
            callback.getCaseDetails().getId());
        authorisationService.authorise(serviceAuthHeader);
        if (isInformationReceivedForInterloc(
            callback.getCaseDetails().getCaseData().getFurtherEvidenceAction().getValue().getCode())) {
            updateToGivenEvent(callback, EventType.INTERLOC_INFORMATION_RECEIVED.getCcdType());
        }
    }

    private boolean isInformationReceivedForInterloc(String code) {
        if (StringUtils.isNotBlank(code)) {
            return code.equalsIgnoreCase("informationReceivedForInterloc");
        }
        return false;
    }

    private void updateToGivenEvent(Callback<SscsCaseData> callback, String eventType) {
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        caseData.setInterlocReviewState("interlocutoryReview");
        ccdService.updateCase(caseData, callback.getCaseDetails().getId(),
            eventType, "update to Information received event",
            "update to Information received event", idamService.getIdamTokens());
    }


    private ResponseEntity<PreSubmitCallbackResponse<SscsCaseData>> performRequest(CallbackType callbackType, Callback<SscsCaseData> callback) {

        PreSubmitCallbackResponse<SscsCaseData> callbackResponse = dispatcher.handle(callbackType, callback);

        log.info("Sscs Case CCD callback `{}` handled for Case ID `{}`", callback.getEvent(), callback.getCaseDetails().getId());

        return ok(callbackResponse);
    }

}
