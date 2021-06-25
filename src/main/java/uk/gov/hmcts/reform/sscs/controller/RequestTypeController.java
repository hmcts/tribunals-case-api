package uk.gov.hmcts.reform.sscs.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.reform.sscs.model.tya.HearingRecording;
import uk.gov.hmcts.reform.sscs.service.requesttype.RequestTypeService;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;

@Slf4j
@RestController
@RequestMapping("/api/request")
public class RequestTypeController {

    private final RequestTypeService requestTypeService;

    public RequestTypeController(RequestTypeService requestTypeService) {
        this.requestTypeService = requestTypeService;
    }

    @ApiOperation(value = "getHearingRecording",
            notes = "Returns hearing recordings given the CCD case id",
            response = HearingRecording.class, responseContainer = "List")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Hearing Recordings",
            response = HearingRecording.class, responseContainer = "List")})
    @GetMapping(value = "/{identifier}/hearingrecording", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<HearingRecording>> getHearingRecording(@PathVariable("identifier") String identifier) {
        return ok(requestTypeService.findHearingRecordings(identifier));
    }

}
