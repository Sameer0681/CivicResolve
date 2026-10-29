package com.civicresolve.dto;

public record DepartmentSummary(
        String department,
        String sector,
        long complaintCount,
        long openComplaintCount,
        long officerCount) {
}
