package com.civicresolve.dto;

import com.civicresolve.entity.Complaint;
import com.civicresolve.entity.ComplaintStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PublicComplaintTrackingResponse {

    private final String complaintId;
    private final ComplaintStatus status;
    private final String sector;
    private final String department;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final LocalDateTime resolvedAt;

    public PublicComplaintTrackingResponse(Complaint complaint) {
        if (complaint == null) {
            this.complaintId = null;
            this.status = null;
            this.sector = null;
            this.department = null;
            this.createdAt = null;
            this.updatedAt = null;
            this.resolvedAt = null;
            return;
        }
        this.complaintId = complaint.getComplaintId();
        this.status = complaint.getStatus();
        this.sector = complaint.getSector();
        this.department = complaint.getDepartment();
        this.createdAt = complaint.getCreatedAt();
        this.updatedAt = complaint.getUpdatedAt();
        this.resolvedAt = complaint.getResolvedAt();
    }
}
