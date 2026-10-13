
package com.civicresolve.controller;

import com.civicresolve.dto.ComplaintResponse;
import com.civicresolve.entity.Complaint;
import com.civicresolve.entity.ComplaintStatus;
import com.civicresolve.service.ComplaintService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

    private final ComplaintService complaintService;

    public ComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    // Create complaint
    @PostMapping
    public ResponseEntity<ComplaintResponse> createComplaint(
            @RequestBody Complaint complaint,
            @RequestParam Long citizenId) {

        Complaint created =
                complaintService.createComplaint(complaint, citizenId);

        return ResponseEntity.ok(new ComplaintResponse(created));
    }

    // Get all complaints
    @GetMapping
    public ResponseEntity<List<ComplaintResponse>> getAllComplaints() {
        List<ComplaintResponse> responses =
                complaintService.getAllComplaints()
                        .stream()
                        .map(ComplaintResponse::new)
                        .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    // Get complaint by database ID
    @GetMapping("/{id}")
    public ResponseEntity<ComplaintResponse> getComplaintById(
            @PathVariable Long id) {

        Complaint complaint = complaintService.getComplaintById(id);

        return ResponseEntity.ok(new ComplaintResponse(complaint));
    }

    // Get complaint by complaint ID
    @GetMapping("/track/{complaintId}")
    public ResponseEntity<ComplaintResponse> getComplaintByComplaintId(
            @PathVariable String complaintId) {

        Complaint complaint =
                complaintService.getComplaintByComplaintId(complaintId);

        return ResponseEntity.ok(new ComplaintResponse(complaint));
    }

    // Get complaints by status
    @GetMapping("/status/{status}")
    public ResponseEntity<List<ComplaintResponse>> getComplaintsByStatus(
            @PathVariable ComplaintStatus status) {

        List<ComplaintResponse> responses =
                complaintService.getComplaintsByStatus(status)
                        .stream()
                        .map(ComplaintResponse::new)
                        .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    // Get complaints by sector
    @GetMapping("/sector/{sector}")
    public ResponseEntity<List<ComplaintResponse>> getComplaintsBySector(
            @PathVariable String sector) {

        List<ComplaintResponse> responses =
                complaintService.getComplaintsBySector(sector)
                        .stream()
                        .map(ComplaintResponse::new)
                        .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    // Delete complaint
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComplaint(
            @PathVariable Long id) {

        complaintService.deleteComplaint(id);

        return ResponseEntity.noContent().build();
    }
}
