package com.civicresolve.service.otp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@ConditionalOnProperty(name = "app.otp.provider", havingValue = "mock", matchIfMissing = true)
public class MockOtpSender implements OtpSender {

    // Condition 4: Plain OTP logged ONLY via dedicated logger "otp.demo" when dev profile active
    private static final Logger demoLogger = LoggerFactory.getLogger("otp.demo");
    private final Environment environment;

    public MockOtpSender(Environment environment) {
        this.environment = environment;
    }

    private boolean isDevProfileActive() {
        if (environment == null) {
            return false;
        }
        return Arrays.asList(environment.getActiveProfiles()).contains("dev");
    }

    @Override
    public void sendSms(String phone, String otp) {
        String masked = maskPhone(phone);
        if (isDevProfileActive()) {
            demoLogger.info("[DEMO OTP SMS] To: {}, OTP Code: {}", masked, otp);
        } else {
            demoLogger.info("[MOCK OTP SMS] To: {} (OTP generated successfully)", masked);
        }
    }

    @Override
    public void sendVoiceCall(String phone, String otp) {
        String masked = maskPhone(phone);
        if (isDevProfileActive()) {
            demoLogger.info("[DEMO OTP VOICE CALL] Calling {} with spoken code: {}", masked, otp);
        } else {
            demoLogger.info("[MOCK OTP VOICE CALL] Calling {} (Call initiated)", masked);
        }
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return "***";
        }
        String clean = phone.trim();
        int len = clean.length();
        if (len < 6) {
            return "***";
        }
        String suffix = clean.substring(len - 4);
        String prefix = clean.startsWith("+91") ? "+91" : (len > 6 ? clean.substring(0, 2) : "");
        int maskedCount = Math.max(2, len - prefix.length() - suffix.length());
        return prefix + "X".repeat(maskedCount) + suffix;
    }
}
