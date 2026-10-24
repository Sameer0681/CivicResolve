package com.civicresolve.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class OtpException extends RuntimeException {

    private final String errorCode;

    public OtpException(String message) {
        this("OTP_ERROR", message);
    }

    public OtpException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
