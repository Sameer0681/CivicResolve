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
    private String urgency;
    private String sentiment;
    private String priority;
    private Double confidence;
    private String summary;
    private String location;
    private ComplaintStatus status;
    private String officerNotes;
    private String mediaUrl;
    private String proofUrl;
    private Double latitude;
    private Double longitude;
    private String complaintId;
    private Long citizenId;
    private String citizenName;
    private Long assignedOfficerId;
    private String assignedOfficerName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;

    public ComplaintResponse(Complaint complaint) {
        if (complaint == null) {
            return;
        }
        this.id = complaint.getId();
        this.title = complaint.getTitle();
        this.description = complaint.getDescription();
        this.sector = complaint.getSector();
        this.department = complaint.getDepartment();
        this.urgency = complaint.getUrgency();
        this.sentiment = complaint.getSentiment();
        this.priority = complaint.getPriority();
        this.confidence = complaint.getConfidence();
        this.summary = complaint.getSummary();
        this.location = complaint.getLocation();
        this.status = complaint.getStatus();
        this.officerNotes = complaint.getOfficerNotes();
        this.mediaUrl = complaint.getMediaUrl();
        this.proofUrl = complaint.getProofUrl();
        this.latitude = complaint.getLatitude();
        this.longitude = complaint.getLongitude();
        this.complaintId = complaint.getComplaintId();
        if (complaint.getCitizen() != null) {
            this.citizenId = complaint.getCitizen().getId();
            this.citizenName = complaint.getCitizen().getFullName();
        }
        if (complaint.getAssignedOfficer() != null) {
            this.assignedOfficerId = complaint.getAssignedOfficer().getId();
            this.assignedOfficerName = complaint.getAssignedOfficer().getFullName();
        }
        this.createdAt = complaint.getCreatedAt();
        this.updatedAt = complaint.getUpdatedAt();
        this.resolvedAt = complaint.getResolvedAt();
    }
}
