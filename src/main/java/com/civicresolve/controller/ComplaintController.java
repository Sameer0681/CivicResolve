
package com.civicresolve.controller;

import com.civicresolve.dto.ComplaintResponse;
import com.civicresolve.dto.PublicComplaintTrackingResponse;
import com.civicresolve.entity.Complaint;
import com.civicresolve.entity.ComplaintStatus;
import com.civicresolve.service.ComplaintService;
import com.civicresolve.service.CustomUserDetails;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

    private final ComplaintService complaintService;

    public ComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    // Create a complaint for the authenticated citizen.
    @PostMapping
    public ResponseEntity<ComplaintResponse> createComplaint(
            @RequestBody Complaint complaint,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        Complaint created = complaintService.createComplaint(
                complaint, currentUser.getId());

        return ResponseEntity.ok(new ComplaintResponse(created));
    }

    // Citizens see their own complaints; admins see all complaints.
    @GetMapping
    public ResponseEntity<List<ComplaintResponse>> getComplaints(
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        List<Complaint> complaints;

        if (isAdmin(currentUser)) {
            complaints = complaintService.getAllComplaints();
        } else {
            complaints = complaintService.getComplaintsByCitizen(
                    currentUser.getId());
        }

        return ResponseEntity.ok(
                complaints.stream().map(ComplaintResponse::new).toList());
    }

    // Retrieve an individual complaint, checking citizen ownership.
    @GetMapping("/{id}")
    public ResponseEntity<ComplaintResponse> getComplaintById(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        Complaint complaint = isAdmin(currentUser)
                ? complaintService.getComplaintById(id)
                : complaintService.getComplaintByIdForCitizen(
                        id, currentUser.getId());

        return ResponseEntity.ok(new ComplaintResponse(complaint));
    }

    // Public tracking returns limited information.
    @GetMapping("/track/{complaintId}")
    public ResponseEntity<PublicComplaintTrackingResponse> getComplaintByComplaintId(
            @PathVariable String complaintId) {

        Complaint complaint =
                complaintService.getComplaintByComplaintId(complaintId);

        return ResponseEntity.ok(
                new PublicComplaintTrackingResponse(complaint));
    }

    // Global status filtering is for admins.
    @GetMapping("/status/{status}")
    public ResponseEntity<List<ComplaintResponse>> getComplaintsByStatus(
            @PathVariable ComplaintStatus status) {

        List<ComplaintResponse> responses =
                complaintService.getComplaintsByStatus(status)
                        .stream()
                        .map(ComplaintResponse::new)
                        .toList();

        return ResponseEntity.ok(responses);
    }

    // Global sector filtering is for admins.
    @GetMapping("/sector/{sector}")
    public ResponseEntity<List<ComplaintResponse>> getComplaintsBySector(
            @PathVariable String sector) {

        List<ComplaintResponse> responses =
                complaintService.getComplaintsBySector(sector)
                        .stream()
                        .map(ComplaintResponse::new)
                        .toList();

        return ResponseEntity.ok(responses);
    }

    // SecurityConfig must restrict this endpoint to admins.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComplaint(@PathVariable Long id) {
        complaintService.deleteComplaint(id);
        return ResponseEntity.noContent().build();
    }

    private boolean isAdmin(CustomUserDetails currentUser) {
        return currentUser != null
                && currentUser.getAuthorities().contains(
                        new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
