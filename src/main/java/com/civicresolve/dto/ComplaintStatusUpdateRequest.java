package com.civicresolve.dto;

import com.civicresolve.entity.ComplaintStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintStatusUpdateRequest {

    @NotNull(message = "Status cannot be null")
    private ComplaintStatus status;

    private String officerNotes;

    private String remarks;

    private String proofUrl;

    public String getEffectiveNotes() {
        if (remarks != null && !remarks.isBlank()) {
            return remarks;
        }
        return officerNotes;
    }
}
