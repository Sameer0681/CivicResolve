package com.civicresolve.dto;

import com.civicresolve.entity.Complaint;
import com.civicresolve.entity.ComplaintStatus;
import lombok.Getter;

@Getter
public class PublicComplaintTrackingResponse {

    private final String complaintId;
    private final ComplaintStatus status;
    private final String sector;
    private final String department;

    public PublicComplaintTrackingResponse(Complaint complaint) {
        this.complaintId = complaint.getComplaintId();
        this.status = complaint.getStatus();
        this.sector = complaint.getSector();
        this.department = complaint.getDepartment();
    }
}