package com.civicresolve.service;

import com.civicresolve.dto.OtpSendRequest;
import com.civicresolve.dto.OtpSendResponse;
import com.civicresolve.dto.OtpVerifyResponse;
import com.civicresolve.entity.User;

public interface OtpService {

    OtpSendResponse sendOtp(User citizen, OtpSendRequest request);

    OtpVerifyResponse verifyOtp(User citizen, String otp);

    void validateAndConsumeToken(String otpToken, Long citizenId);

    boolean isOtpRequired();
}
