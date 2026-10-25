package com.civicresolve.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "complaints")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 50)
    private String sector;

    @Column(length = 150)
    private String department;

    @Column(length = 30)
    private String urgency;

    @Column(length = 30)
    private String sentiment;

    @Column(length = 30)
    private String priority;

    private Double confidence;

    @Column(length = 1000)
    private String summary;

    @Column(length = 255)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ComplaintStatus status;


    @Column(columnDefinition = "TEXT")
    private String officerNotes;

    @Column(length = 500)
    private String mediaUrl;

    @Column(length = 500)
    private String proofUrl;

    private Double latitude;

    private Double longitude;

    @Column(nullable = false, unique = true, length = 30)
    private String complaintId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "citizen_id", nullable = false)
    private User citizen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "officer_id")
    private User assignedOfficer;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime resolvedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        if (this.status == null) {
            this.status = ComplaintStatus.SUBMITTED;
        }
        if (this.urgency == null || this.urgency.isBlank()) {
            this.urgency = "MEDIUM";
        }
        if (this.priority == null || this.priority.isBlank()) {
            this.priority = "MEDIUM";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
