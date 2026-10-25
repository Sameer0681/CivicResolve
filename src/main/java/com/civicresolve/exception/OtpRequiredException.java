package com.civicresolve.exception;

import lombok.Getter;

@Getter
public class OtpRequiredException extends RuntimeException {

    private final String maskedPhone;
    private final long expiresInSeconds;
    private final long resendAfterSeconds;
    private final String channel;

    public OtpRequiredException(String message, String maskedPhone, long expiresInSeconds, long resendAfterSeconds) {
        this(message, maskedPhone, expiresInSeconds, resendAfterSeconds, null);
    }

    public OtpRequiredException(String message, String maskedPhone, long expiresInSeconds, long resendAfterSeconds, String channel) {
        super(message);
        this.maskedPhone = maskedPhone;
        this.expiresInSeconds = expiresInSeconds;
        this.resendAfterSeconds = resendAfterSeconds;
        this.channel = channel;
    }
}
