package com.civicresolve.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpSendResponse {
    private String message;
    private String channel;
    private String maskedPhone;
    private long expiresInSeconds;
    private long cooldownSeconds;
}
