package com.civicresolve.controller;

import com.civicresolve.dto.ComplaintAssignRequest;
import com.civicresolve.dto.ComplaintCreateRequest;
import com.civicresolve.dto.ComplaintResponse;
import com.civicresolve.dto.ComplaintStatusUpdateRequest;
import com.civicresolve.dto.PublicComplaintTrackingResponse;
import com.civicresolve.entity.Complaint;
import com.civicresolve.entity.ComplaintStatus;
import com.civicresolve.exception.ForbiddenException;
import com.civicresolve.exception.UnauthorizedException;
import com.civicresolve.service.ComplaintService;
import com.civicresolve.service.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

    private final ComplaintService complaintService;

    public ComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    // Create a complaint for the authenticated citizen (or by admin for citizenId)
    @PostMapping
    public ResponseEntity<ComplaintResponse> createComplaint(
            @Valid @RequestBody ComplaintCreateRequest request,
            @RequestParam(required = false) Long citizenId,
            @RequestHeader(value = "X-OTP-Token", required = false) String headerOtpToken,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            throw new UnauthorizedException("Authentication required to submit complaint");
        }

        // Point 2: For ROLE_CITIZEN, strictly ignore citizenId param. Only ADMIN may pass a different citizenId.
        Long effectiveCitizenId = (isAdmin(currentUser) && citizenId != null)
                ? citizenId
                : currentUser.getId();

        String effectiveOtpToken = (request.getOtpToken() != null && !request.getOtpToken().isBlank())
                ? request.getOtpToken()
                : headerOtpToken;

        Complaint complaint = Complaint.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .sector(request.getSector())
                .department(request.getDepartment())
                .location(request.getLocation())
                .complaintId(request.getComplaintId())
                .status(ComplaintStatus.SUBMITTED)
                .build();

        Complaint created = complaintService.createComplaint(complaint, effectiveCitizenId, effectiveOtpToken);
        return ResponseEntity.ok(new ComplaintResponse(created));
    }

    // Citizens see their own complaints; Officers see assigned/department; Admins see all.
    @GetMapping
    public ResponseEntity<List<ComplaintResponse>> getComplaints(
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            throw new UnauthorizedException("Authentication required");
        }

        List<Complaint> complaints;
        if (isAdmin(currentUser)) {
            complaints = complaintService.getAllComplaints();
        } else if (isOfficer(currentUser)) {
            complaints = complaintService.getComplaintsByOfficer(currentUser.getId());
        } else {
            complaints = complaintService.getComplaintsByCitizen(currentUser.getId());
        }

        return ResponseEntity.ok(
                complaints.stream().map(ComplaintResponse::new).toList());
    }

    // Retrieve complaints by citizen ID (Citizen can view own, Admin can view any)
    @GetMapping("/citizen/{citizenId}")
    public ResponseEntity<List<ComplaintResponse>> getComplaintsByCitizenId(
            @PathVariable Long citizenId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            throw new UnauthorizedException("Authentication required");
        }

        if (!isAdmin(currentUser) && !Objects.equals(currentUser.getId(), citizenId)) {
            throw new ForbiddenException("Access denied: You cannot view complaints of another citizen");
        }

        List<Complaint> complaints = complaintService.getComplaintsByCitizen(citizenId);
        return ResponseEntity.ok(complaints.stream().map(ComplaintResponse::new).toList());
    }

    // Retrieve an individual complaint, checking role-based ownership.
    @GetMapping("/{id}")
    public ResponseEntity<ComplaintResponse> getComplaintById(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            throw new UnauthorizedException("Authentication required");
        }

        Complaint complaint;
        if (isAdmin(currentUser)) {
            complaint = complaintService.getComplaintById(id);
        } else if (isOfficer(currentUser)) {
            complaint = complaintService.getComplaintByIdForOfficer(id, currentUser.getId());
        } else {
            complaint = complaintService.getComplaintByIdForCitizen(id, currentUser.getId());
        }

        return ResponseEntity.ok(new ComplaintResponse(complaint));
    }

    // Public tracking returns limited, safe information.
    @GetMapping("/track/{complaintId}")
    public ResponseEntity<PublicComplaintTrackingResponse> getComplaintByComplaintId(
            @PathVariable String complaintId) {

        Complaint complaint = complaintService.getComplaintByComplaintId(complaintId);
        return ResponseEntity.ok(new PublicComplaintTrackingResponse(complaint));
    }

    // Status update API: Field Officers and Admins can update complaint status, remarks, and proofUrl
    @RequestMapping(value = "/{id}/status", method = {RequestMethod.PATCH, RequestMethod.PUT})
    public ResponseEntity<ComplaintResponse> updateComplaintStatus(
            @PathVariable String id,
            @Valid @RequestBody ComplaintStatusUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            throw new UnauthorizedException("Authentication required");
        }

        boolean admin = isAdmin(currentUser);
        Complaint updated = complaintService.updateComplaintStatus(
                id,
                request.getStatus(),
                request.getEffectiveNotes(),
                request.getProofUrl(),
                currentUser.getId(),
                admin);

        return ResponseEntity.ok(new ComplaintResponse(updated));
    }

    // Assign officer API: Only Admins can assign complaints to officers.
    @RequestMapping(value = "/{id}/assign", method = {RequestMethod.PATCH, RequestMethod.PUT})
    public ResponseEntity<ComplaintResponse> assignOfficer(
            @PathVariable Long id,
            @Valid @RequestBody ComplaintAssignRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            throw new UnauthorizedException("Authentication required");
        }
        if (!isAdmin(currentUser)) {
            throw new ForbiddenException("Access denied: Only administrators can assign officers");
        }

        Complaint updated = complaintService.assignOfficer(id, request.getOfficerId());
        return ResponseEntity.ok(new ComplaintResponse(updated));
    }

    // Status filtering for Officers and Admins.
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

    // Sector filtering for Officers and Admins.
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

    // Delete complaint: restricted to Admins.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComplaint(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (currentUser == null) {
            throw new UnauthorizedException("Authentication required");
        }
        if (!isAdmin(currentUser)) {
            throw new ForbiddenException("Access denied: Only administrators can delete complaints");
        }

        complaintService.deleteComplaint(id);
        return ResponseEntity.noContent().build();
    }

    private boolean isAdmin(CustomUserDetails currentUser) {
        return currentUser != null
                && currentUser.getAuthorities().contains(
                        new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private boolean isOfficer(CustomUserDetails currentUser) {
        return currentUser != null
                && currentUser.getAuthorities().contains(
                        new SimpleGrantedAuthority("ROLE_OFFICER"));
    }
}
