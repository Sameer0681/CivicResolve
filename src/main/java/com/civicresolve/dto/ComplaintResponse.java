
package com.civicresolve.dto;

import com.civicresolve.entity.Complaint;
import com.civicresolve.entity.ComplaintStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ComplaintResponse {

    private Long id;
    private String title;
    private String description;
    private String sector;
    private String department;
    private String location;
    private ComplaintStatus status;
    private String complaintId;
    private Long citizenId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ComplaintResponse(Complaint complaint) {
        this.id = complaint.getId();
        this.title = complaint.getTitle();
        this.description = complaint.getDescription();
        this.sector = complaint.getSector();
        this.department = complaint.getDepartment();
        this.location = complaint.getLocation();
        this.status = complaint.getStatus();
        this.complaintId = complaint.getComplaintId();
        this.citizenId = complaint.getCitizen() != null
                ? complaint.getCitizen().getId()
                : null;
        this.createdAt = complaint.getCreatedAt();
        this.updatedAt = complaint.getUpdatedAt();
    }
}
