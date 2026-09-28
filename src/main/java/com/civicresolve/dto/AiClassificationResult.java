package com.civicresolve.dto;

import com.civicresolve.entity.ComplaintPriority;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiClassificationResult {

    @NotBlank(message = "Sector is required")
    private String sector;

    private String department;

    private String urgency;

    private String sentiment;

    private ComplaintPriority priority;

    @DecimalMin(value = "0.0", message = "Confidence score must be at least 0.0")
    @DecimalMax(value = "1.0", message = "Confidence score cannot exceed 1.0")
    private Double confidence;

    private String summary;
}
