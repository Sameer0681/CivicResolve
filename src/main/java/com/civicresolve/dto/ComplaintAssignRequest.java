package com.civicresolve.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintAssignRequest {

    @NotNull(message = "Officer ID cannot be null")
    private Long officerId;
}
