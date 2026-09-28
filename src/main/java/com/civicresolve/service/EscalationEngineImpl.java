package com.civicresolve.service;

import com.civicresolve.entity.*;
import com.civicresolve.repository.ComplaintRepository;
import com.civicresolve.repository.EscalationRepository;
import com.civicresolve.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class EscalationEngineImpl implements EscalationEngine {

    private static final Logger log = LoggerFactory.getLogger(EscalationEngineImpl.class);

    private final ComplaintRepository complaintRepository;
    private final EscalationRepository escalationRepository;
    private final UserRepository userRepository;

    public EscalationEngineImpl(
            ComplaintRepository complaintRepository,
            EscalationRepository escalationRepository,
            UserRepository userRepository) {
        this.complaintRepository = complaintRepository;
        this.escalationRepository = escalationRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public List<Escalation> processEscalations() {
        return processEscalations(LocalDateTime.now());
    }

    @Override
    @Transactional
    public List<Escalation> processEscalations(LocalDateTime referenceTime) {
        LocalDateTime now = referenceTime != null ? referenceTime : LocalDateTime.now();
        List<Complaint> overdueComplaints = complaintRepository.findBySlaDeadlineBeforeAndStatusNot(now, ComplaintStatus.RESOLVED);

        List<Escalation> createdEscalations = new ArrayList<>();

        for (Complaint complaint : overdueComplaints) {
            Escalation escalation = processSingleComplaintEscalation(complaint, now);
            if (escalation != null) {
                createdEscalations.add(escalation);
            }
        }

        log.info("EscalationEngine processed {} overdue complaints at {}. Created {} new escalation records.",
                overdueComplaints.size(), now, createdEscalations.size());

        return createdEscalations;
    }

    private Escalation processSingleComplaintEscalation(Complaint complaint, LocalDateTime now) {
        EscalationLevel currentLevel = complaint.getCurrentEscalationLevel();

        // Stop at LEVEL_3 (Do not create LEVEL_4)
        if (currentLevel == EscalationLevel.LEVEL_3) {
            log.debug("Complaint ID {} is already at LEVEL_3 escalation limit. Skipping.", complaint.getComplaintId());
            return null;
        }

        int targetLevelInt;
        EscalationLevel targetLevelEnum;
        String reason;

        if (currentLevel == null) {
            targetLevelInt = 1;
            targetLevelEnum = EscalationLevel.LEVEL_1;
            reason = "SLA breached: complaint was not resolved within the configured SLA deadline.";
        } else if (currentLevel == EscalationLevel.LEVEL_1) {
            targetLevelInt = 2;
            targetLevelEnum = EscalationLevel.LEVEL_2;
            reason = "SLA still breached: promoted from LEVEL_1 to LEVEL_2.";
        } else if (currentLevel == EscalationLevel.LEVEL_2) {
            targetLevelInt = 3;
            targetLevelEnum = EscalationLevel.LEVEL_3;
            reason = "SLA still breached: promoted from LEVEL_2 to LEVEL_3.";
        } else {
            return null;
        }

        // Anti-duplicate rule: Check if escalation record for target level already exists
        if (escalationRepository.existsByComplaintAndEscalationLevel(complaint, targetLevelInt)) {
            log.debug("Escalation record for Complaint ID {} at level {} already exists. Skipping duplicate creation.",
                    complaint.getComplaintId(), targetLevelInt);
            return null;
        }

        // Officer lookup by department
        User assignedOfficer = null;
        if (complaint.getDepartment() != null && !complaint.getDepartment().isBlank()) {
            assignedOfficer = userRepository.findFirstByDepartmentAndRole(complaint.getDepartment(), Role.ROLE_OFFICER)
                    .orElse(null);
        }

        String priorityStr = complaint.getPriority() != null ? complaint.getPriority().name() : "HIGH";

        Escalation escalation = Escalation.builder()
                .complaint(complaint)
                .escalatedBy(null) // Automated system escalation (no fake user)
                .assignedOfficer(assignedOfficer)
                .reason(reason)
                .escalationLevel(targetLevelInt)
                .priorityLevel(priorityStr)
                .status("PENDING")
                .overdueSince(complaint.getSlaDeadline() != null ? complaint.getSlaDeadline() : now)
                .createdAt(now)
                .build();

        Escalation savedEscalation = escalationRepository.save(escalation);

        // Update complaint state
        complaint.setCurrentEscalationLevel(targetLevelEnum);
        complaint.setStatus(ComplaintStatus.ESCALATED);
        complaintRepository.save(complaint);

        log.info("Escalated Complaint ID {} to {} (Record ID: {}).",
                complaint.getComplaintId(), targetLevelEnum, savedEscalation.getId());

        return savedEscalation;
    }
}
