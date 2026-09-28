package com.civicresolve.service;

import com.civicresolve.dto.AiClassificationResult;
import com.civicresolve.entity.Complaint;
import com.civicresolve.entity.ComplaintPriority;
import com.civicresolve.entity.ComplaintStatus;
import com.civicresolve.entity.User;
import com.civicresolve.repository.ComplaintRepository;
import com.civicresolve.repository.UserRepository;
import com.civicresolve.service.ai.PrioritySafetyRuleEngine;
import com.civicresolve.service.ai.SectorRegistry;
import com.civicresolve.service.ai.UrgencyNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ComplaintServiceImpl implements ComplaintService {

    private static final Logger log = LoggerFactory.getLogger(ComplaintServiceImpl.class);

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final AiClassificationService aiClassificationService;
    private final PrioritySafetyRuleEngine prioritySafetyRuleEngine;
    private final UrgencyNormalizer urgencyNormalizer;
    private final SlaPolicyService slaPolicyService;

    public ComplaintServiceImpl(
            ComplaintRepository complaintRepository,
            UserRepository userRepository,
            AiClassificationService aiClassificationService,
            PrioritySafetyRuleEngine prioritySafetyRuleEngine,
            UrgencyNormalizer urgencyNormalizer,
            SlaPolicyService slaPolicyService) {

        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
        this.aiClassificationService = aiClassificationService;
        this.prioritySafetyRuleEngine = prioritySafetyRuleEngine;
        this.urgencyNormalizer = urgencyNormalizer;
        this.slaPolicyService = slaPolicyService;
    }

    @Override
    public Complaint createComplaint(Complaint complaint, Long citizenId) {

        User citizen = userRepository.findById(citizenId)
                .orElseThrow(() -> new RuntimeException("Citizen not found"));

        complaint.setCitizen(citizen);

        if (complaint.getCreatedAt() == null) {
            complaint.setCreatedAt(LocalDateTime.now());
        }

        // 1. Run AI classification
        ComplaintPriority initialPriority = complaint.getPriority();
        String rawUrgency = complaint.getUrgency();

        try {
            AiClassificationResult classification = aiClassificationService.classify(
                    complaint.getTitle(),
                    complaint.getDescription()
            );

            if (classification != null) {
                if (complaint.getSector() == null || complaint.getSector().isBlank()) {
                    complaint.setSector(classification.getSector());
                }
                if (complaint.getDepartment() == null || complaint.getDepartment().isBlank()) {
                    complaint.setDepartment(classification.getDepartment());
                }
                if (rawUrgency == null || rawUrgency.isBlank()) {
                    rawUrgency = classification.getUrgency();
                }
                if (complaint.getSentiment() == null || complaint.getSentiment().isBlank()) {
                    complaint.setSentiment(classification.getSentiment());
                }
                if (initialPriority == null) {
                    initialPriority = classification.getPriority();
                }
            }
        } catch (Exception e) {
            log.error("Failed to run AI classification for complaint title '{}': {}", complaint.getTitle(), e.getMessage());
            if (complaint.getSector() == null || complaint.getSector().isBlank()) {
                complaint.setSector(SectorRegistry.MUNICIPAL_SERVICES);
            }
            if (complaint.getDepartment() == null || complaint.getDepartment().isBlank()) {
                complaint.setDepartment(SectorRegistry.getDepartmentForSector(complaint.getSector()));
            }
            if (initialPriority == null) {
                initialPriority = ComplaintPriority.MEDIUM;
            }
        }

        // 2. Evaluate Safety Rules on Priority (Obvious electrical danger / life threatening situations)
        ComplaintPriority finalPriority = prioritySafetyRuleEngine.evaluateSafetyPriority(
                complaint.getTitle(),
                complaint.getDescription(),
                initialPriority
        );
        complaint.setPriority(finalPriority);

        // 3. Normalize Urgency
        String normalizedUrgency = urgencyNormalizer.normalize(rawUrgency != null ? rawUrgency : finalPriority.name());
        complaint.setUrgency(normalizedUrgency);

        // 4. Calculate SLA Deadline from creation time
        LocalDateTime slaDeadline = slaPolicyService.calculateSlaDeadline(finalPriority, complaint.getCreatedAt());
        complaint.setSlaDeadline(slaDeadline);

        return complaintRepository.save(complaint);
    }

    @Override
    public List<Complaint> getAllComplaints() {
        return complaintRepository.findAll();
    }

    @Override
    public Complaint getComplaintById(Long id) {
        return complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Complaint not found"));
    }

    @Override
    public Complaint getComplaintByComplaintId(String complaintId) {
        return complaintRepository.findByComplaintId(complaintId)
                .orElseThrow(() -> new RuntimeException("Complaint not found"));
    }

    @Override
    public List<Complaint> getComplaintsByStatus(ComplaintStatus status) {
        return complaintRepository.findByStatus(status);
    }

    @Override
    public List<Complaint> getComplaintsBySector(String sector) {
        return complaintRepository.findBySector(sector);
    }

    @Override
    public Complaint findComplaintByParam(String param) {
        if (param == null || param.isBlank()) {
            List<Complaint> all = complaintRepository.findAll();
            return all.isEmpty() ? null : all.get(all.size() - 1);
        }

        String trimmed = param.trim();
        Complaint found = complaintRepository.findByComplaintId(trimmed).orElse(null);
        if (found != null) {
            return found;
        }

        try {
            Long dbId = Long.parseLong(trimmed);
            return complaintRepository.findById(dbId).orElse(null);
        } catch (NumberFormatException ignored) {}

        return null;
    }

    @Override
    public void deleteComplaint(Long id) {

        if (!complaintRepository.existsById(id)) {
            throw new RuntimeException("Complaint not found");
        }

        complaintRepository.deleteById(id);
    }
}
