package com.civicresolve.service.otp;

public interface OtpSender {

    void sendSms(String phone, String otp);

    void sendVoiceCall(String phone, String otp);
}
