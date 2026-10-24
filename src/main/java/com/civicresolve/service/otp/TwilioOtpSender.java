package com.civicresolve.service.otp;

import com.civicresolve.exception.OtpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "app.otp.provider", havingValue = "twilio")
public class TwilioOtpSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(TwilioOtpSender.class);

    private final RestClient restClient;
    private final String accountSid;
    private final String fromNumber;

    public TwilioOtpSender(
            @Value("${twilio.account-sid:}") String accountSid,
            @Value("${twilio.auth-token:}") String authToken,
            @Value("${twilio.phone-number:}") String fromNumber) {
        this.accountSid = accountSid;
        this.fromNumber = fromNumber;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.twilio.com/2010-04-01/Accounts/" + accountSid)
                .defaultHeaders(headers -> headers.setBasicAuth(accountSid, authToken))
                .build();
    }

    @Override
    public void sendSms(String phone, String otp) {
        String masked = MockOtpSender.maskPhone(phone);
        log.info("Sending Twilio SMS OTP to {}", masked);

        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("To", phone);
            formData.add("From", fromNumber);
            formData.add("Body", "Your CivicResolve verification code is: " + otp + ". Valid for 5 minutes.");

            restClient.post()
                    .uri("/Messages.json")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Twilio SMS OTP dispatched successfully to {}", masked);
        } catch (Exception e) {
            log.error("Failed to dispatch Twilio SMS to {}: {}", masked, e.getMessage());
            throw new OtpException("Failed to dispatch SMS verification code: " + e.getMessage());
        }
    }

    @Override
    public void sendVoiceCall(String phone, String otp) {
        String masked = MockOtpSender.maskPhone(phone);
        log.info("Initiating Twilio Voice OTP call to {}", masked);

        try {
            String twiml = "<Response><Say voice=\"alice\">Your CivicResolve verification code is <say-as interpret-as=\"digits\">"
                    + otp
                    + "</say-as>. I repeat, your code is <say-as interpret-as=\"digits\">"
                    + otp
                    + "</say-as>. Goodbye.</Say></Response>";

            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("To", phone);
            formData.add("From", fromNumber);
            formData.add("Twiml", twiml);

            restClient.post()
                    .uri("/Calls.json")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Twilio Voice OTP call initiated successfully to {}", masked);
        } catch (Exception e) {
            log.error("Failed to initiate Twilio Voice call to {}: {}", masked, e.getMessage());
            throw new OtpException("Failed to initiate voice call verification: " + e.getMessage());
        }
    }
}
