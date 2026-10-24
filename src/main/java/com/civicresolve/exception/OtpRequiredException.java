package com.civicresolve.exception;

import lombok.Getter;

@Getter
public class OtpRequiredException extends RuntimeException {

    private final String maskedPhone;
    private final long expiresInSeconds;
    private final long resendAfterSeconds;

    public OtpRequiredException(String message, String maskedPhone, long expiresInSeconds, long resendAfterSeconds) {
        super(message);
        this.maskedPhone = maskedPhone;
        this.expiresInSeconds = expiresInSeconds;
        this.resendAfterSeconds = resendAfterSeconds;
    }
}
