package com.civicresolve.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpSendRequest {

    // "SMS", "CALL", or "BOTH" (default "BOTH")
    @Builder.Default
    private String channel = "BOTH";
}
