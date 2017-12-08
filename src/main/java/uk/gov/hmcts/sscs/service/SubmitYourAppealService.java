package uk.gov.hmcts.sscs.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.sscs.email.SubmitYourAppealEmail;
import uk.gov.hmcts.sscs.tribunals.domain.corecase.CcdCase;


@Service
public class SubmitYourAppealService {

    private static final Logger log = LoggerFactory.getLogger(SubmitYourAppealService.class);

    private final EmailService emailService;
    private final SubmitYourAppealEmail email;

    @Autowired
    public SubmitYourAppealService(EmailService emailService, SubmitYourAppealEmail email) {
        this.emailService = emailService;
        this.email = email;
    }

    public void submitAppeal(Map<String, Object> appeal)  {
        convertJsonToCase(appeal);

        emailService.sendEmail(email);
    }

    public void convertJsonToCase(Map<String, Object> appeal) {
        //TODO: Save the case to the database
        try {
            JSONObject json = new JSONObject(appeal);
            ObjectMapper mapper = new ObjectMapper();
            mapper.readValue(json.toString(), CcdCase.class);
        } catch (IOException e) {
            log.error("Json to CCD object exception", e);
        }
    }
}
