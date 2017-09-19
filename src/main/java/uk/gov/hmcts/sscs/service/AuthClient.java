package uk.gov.hmcts.sscs.service;

import static org.slf4j.LoggerFactory.getLogger;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AuthClient {
    private static final Logger LOG = getLogger(AuthClient.class);

    private String vaultSecret;
    private OtpGenerator otpGenerator;
    private String authApiUrl;
    private RestTemplate restTemplate;

    @Autowired
    AuthClient(@Value("${auth.micro.service.key}") String vaultSecret,
               @Value("${auth.provider.service.api}") String authApiUrl,
               OtpGenerator otpGenerator, RestTemplate restTemplate) {
        this.vaultSecret = vaultSecret;
        this.otpGenerator = otpGenerator;
        this.authApiUrl = authApiUrl;
        this.restTemplate = restTemplate;
    }

    /**
     * Sends the request to auth api.
     * @param path path of the api
     * @param httpMethod http method to be invoked on auth api
     * @param requestBody request body
     * @return Response string
     */
    public String sendRequest(String path, HttpMethod httpMethod, String requestBody) {
        ResponseEntity<String> responseEntity = new ResponseEntity<>(HttpStatus.OK);
        try {
            String otp = otpGenerator.issueOneTimePassword(vaultSecret);
            String authUrl = authApiUrl + path + "?microservice=sscs&oneTimePassword=" + otp;
            HttpEntity requestEntity = new HttpEntity(requestBody);
            responseEntity = restTemplate.exchange(authUrl,httpMethod,requestEntity,String.class);
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            LOG.error("Error in auth client: ", e);
        }
        return responseEntity.getBody();
    }
}
