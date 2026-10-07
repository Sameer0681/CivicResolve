package com.civicresolve.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiClassificationResult {

    private String sector;
    private String department;
    private String urgency;
    private String sentiment;
    private String priority;
    private Double confidence;
    private String summary;
}
