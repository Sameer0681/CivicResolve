package com.civicresolve.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplaintCreateRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Sector is required")
    private String sector;

    private String department;

    private String location;

    // Optional client-supplied ticket ID
    private String complaintId;

    // Optional OTP token in request body (also accepted in X-OTP-Token header)
    private String otpToken;
}
