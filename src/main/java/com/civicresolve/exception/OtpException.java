package com.civicresolve.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class OtpException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus status;

    public OtpException(String message) {
        this("OTP_DELIVERY_FAILED", message, HttpStatus.BAD_GATEWAY);
    }

    public OtpException(String errorCode, String message) {
        this(errorCode, message, HttpStatus.BAD_GATEWAY);
    }

    public OtpException(String errorCode, String message, HttpStatus status) {
        super(message);
        this.errorCode = errorCode != null ? errorCode : "OTP_DELIVERY_FAILED";
        this.status = status != null ? status : HttpStatus.BAD_GATEWAY;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getStatus() {
        return status != null ? status : HttpStatus.BAD_GATEWAY;
    }
}
