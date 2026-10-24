package com.civicresolve.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpVerifyResponse {
    private boolean verified;
    private String message;
    private String otpToken;
    private LocalDateTime tokenExpiresAt;
}
