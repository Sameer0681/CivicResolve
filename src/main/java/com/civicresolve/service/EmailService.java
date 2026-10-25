package com.civicresolve.service;

public interface EmailService {

    void sendRegistrationOtp(String email, String otp, String name);

    void sendPasswordResetOtp(String email, String otp, String resetLink);
}
