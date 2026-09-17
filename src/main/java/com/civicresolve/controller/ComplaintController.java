package com.civicresolve.controller;

import com.civicresolve.entity.Complaint;
import com.civicresolve.entity.ComplaintStatus;
import com.civicresolve.service.ComplaintService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

    private final ComplaintService complaintService;

    public ComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    // Create complaint
    @PostMapping
    public ResponseEntity<Complaint> createComplaint(
            @RequestBody Complaint complaint,
            @RequestParam Long citizenId) {

        Complaint createdComplaint =
                complaintService.createComplaint(complaint, citizenId);

        return ResponseEntity.ok(createdComplaint);
    }

    // Get all complaints
    @GetMapping
    public ResponseEntity<List<Complaint>> getAllComplaints() {

        return ResponseEntity.ok(
                complaintService.getAllComplaints()
        );
    }

    // Get complaint by database ID
    @GetMapping("/{id}")
    public ResponseEntity<Complaint> getComplaintById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                complaintService.getComplaintById(id)
        );
    }

    // Get complaint by complaint ID
    @GetMapping("/track/{complaintId}")
    public ResponseEntity<Complaint> getComplaintByComplaintId(
            @PathVariable String complaintId) {

        return ResponseEntity.ok(
                complaintService.getComplaintByComplaintId(complaintId)
        );
    }

    // Get complaints by status
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Complaint>> getComplaintsByStatus(
            @PathVariable ComplaintStatus status) {

        return ResponseEntity.ok(
                complaintService.getComplaintsByStatus(status)
        );
    }

    // Get complaints by sector
    @GetMapping("/sector/{sector}")
    public ResponseEntity<List<Complaint>> getComplaintsBySector(
            @PathVariable String sector) {

        return ResponseEntity.ok(
                complaintService.getComplaintsBySector(sector)
        );
    }

    // Delete complaint
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComplaint(
            @PathVariable Long id) {

        complaintService.deleteComplaint(id);

        return ResponseEntity.noContent().build();
    }
}
