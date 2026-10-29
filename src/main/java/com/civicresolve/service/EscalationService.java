package com.civicresolve.service;

import com.civicresolve.entity.Complaint;
import com.civicresolve.entity.ComplaintStatus;
import com.civicresolve.entity.Escalation;
import com.civicresolve.entity.Role;
import com.civicresolve.entity.User;
import com.civicresolve.exception.BadRequestException;
import com.civicresolve.exception.ResourceNotFoundException;
import com.civicresolve.repository.ComplaintRepository;
import com.civicresolve.repository.EscalationRepository;
import com.civicresolve.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class EscalationService {

    private final EscalationRepository escalationRepository;
    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;

    public EscalationService(EscalationRepository escalationRepository,
                             ComplaintRepository complaintRepository,
                             UserRepository userRepository) {
        this.escalationRepository = escalationRepository;
        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Escalation> getAllEscalations() {
        return escalationRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Escalation> getEscalationsForOfficer(Long officerId) {
        return escalationRepository.findByAssignedOfficer_IdOrderByCreatedAtDesc(officerId);
    }

    public Escalation createEscalation(Long complaintId, Long officerId, User admin, String reason) {
        if (reason == null || reason.isBlank() || reason.trim().length() > 2000) {
            throw new BadRequestException("Provide an escalation reason of up to 2000 characters.");
        }

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with ID: " + complaintId));
        if (complaint.getStatus() == ComplaintStatus.RESOLVED) {
            throw new BadRequestException("Resolved complaints cannot be escalated.");
        }
        User officer = userRepository.findById(officerId)
                .orElseThrow(() -> new ResourceNotFoundException("Officer not found with ID: " + officerId));

        if (officer.getRole() != Role.ROLE_OFFICER) {
            throw new BadRequestException("Escalations must be assigned to an officer.");
        }
        if (complaint.getDepartment() != null
                && !complaint.getDepartment().equalsIgnoreCase(officer.getDepartment())) {
            throw new BadRequestException("Choose an officer from the complaint's department.");
        }

        complaint.setAssignedOfficer(officer);
        complaint.setStatus(ComplaintStatus.ESCALATED);
        complaintRepository.save(complaint);

        int nextLevel = escalationRepository.findByComplaint_Id(complaintId).stream()
                .map(Escalation::getEscalationLevel)
                .filter(level -> level != null)
                .max(Comparator.naturalOrder())
                .orElse(0) + 1;
        Escalation escalation = Escalation.builder()
                .complaint(complaint)
                .escalatedBy(admin)
                .assignedOfficer(officer)
                .reason(reason.trim())
                .escalationLevel(nextLevel)
                .build();
        return escalationRepository.save(escalation);
    }

    public void reassignForComplaint(Long complaintId, User officer) {
        List<Escalation> escalations = escalationRepository.findByComplaint_Id(complaintId);
        for (Escalation escalation : escalations) {
            if (!"RESOLVED".equalsIgnoreCase(escalation.getStatus())) {
                escalation.setAssignedOfficer(officer);
            }
        }
        escalationRepository.saveAll(escalations);
    }

    public void resolveForComplaint(Long complaintId) {
        List<Escalation> escalations = escalationRepository.findByComplaint_Id(complaintId);
        for (Escalation escalation : escalations) {
            if (!"RESOLVED".equalsIgnoreCase(escalation.getStatus())
                    && !"CANCELLED".equalsIgnoreCase(escalation.getStatus())) {
                escalation.setStatus("RESOLVED");
                escalation.setResolvedAt(LocalDateTime.now());
            }
        }
        escalationRepository.saveAll(escalations);
    }

    public void cancelForComplaint(Long complaintId) {
        List<Escalation> escalations = escalationRepository.findByComplaint_Id(complaintId);
        for (Escalation escalation : escalations) {
            if (!"RESOLVED".equalsIgnoreCase(escalation.getStatus())
                    && !"CANCELLED".equalsIgnoreCase(escalation.getStatus())) {
                escalation.setStatus("CANCELLED");
                escalation.setResolvedAt(LocalDateTime.now());
            }
        }
        escalationRepository.saveAll(escalations);
    }

    public void deleteForComplaint(Long complaintId) {
        escalationRepository.deleteByComplaint_Id(complaintId);
    }
}
