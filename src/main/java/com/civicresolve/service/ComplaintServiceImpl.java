package com.civicresolve.service;

import com.civicresolve.ai.AiClassificationResult;
import com.civicresolve.ai.GeminiAiProvider;
import com.civicresolve.entity.Complaint;
import com.civicresolve.entity.ComplaintStatus;
import com.civicresolve.entity.Role;
import com.civicresolve.entity.User;
import com.civicresolve.exception.BadRequestException;
import com.civicresolve.exception.ConflictException;
import com.civicresolve.exception.ResourceNotFoundException;
import com.civicresolve.exception.UnauthorizedException;
import com.civicresolve.repository.ComplaintRepository;
import com.civicresolve.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional
public class ComplaintServiceImpl implements ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final GeminiAiProvider geminiAiProvider;
    private final OtpService otpService;

    public ComplaintServiceImpl(
            ComplaintRepository complaintRepository,
            UserRepository userRepository,
            GeminiAiProvider geminiAiProvider) {
        this(complaintRepository, userRepository, geminiAiProvider, null);
    }

    @Autowired
    public ComplaintServiceImpl(
            ComplaintRepository complaintRepository,
            UserRepository userRepository,
            GeminiAiProvider geminiAiProvider,
            @Autowired(required = false) OtpService otpService) {
        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
        this.geminiAiProvider = geminiAiProvider;
        this.otpService = otpService;
    }

    @Override
    public Complaint createComplaint(Complaint complaint, Long citizenId) {
        return createComplaint(complaint, citizenId, null);
    }

    @Override
    public Complaint createComplaint(Complaint complaint, Long citizenId, String otpToken) {
        User citizen = userRepository.findById(citizenId)
                .orElseThrow(() -> new ResourceNotFoundException("Citizen not found with ID: " + citizenId));

        // OTP verification check
        if (otpService != null && otpService.isOtpRequired()) {
            if (otpToken == null || otpToken.isBlank()) {
                otpService.triggerComplaintSubmissionOtp(citizen);
            }
            otpService.validateAndConsumeToken(otpToken, citizenId);
        } else if (otpService != null && otpToken != null && !otpToken.isBlank()) {
            otpService.validateAndConsumeToken(otpToken, citizenId);
        }

        // Parse coordinates from location string if formatted as "lat, lng"
        if (complaint.getLocation() != null && !complaint.getLocation().isBlank()) {
            String loc = complaint.getLocation().trim();
            if (loc.matches("^[-+]?[0-9]*\\.?[0-9]+,\\s*[-+]?[0-9]*\\.?[0-9]+$")) {
                String[] parts = loc.split(",");
                try {
                    complaint.setLatitude(Double.parseDouble(parts[0].trim()));
                    complaint.setLongitude(Double.parseDouble(parts[1].trim()));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        // Combine title and description for classification
        String complaintText = complaint.getTitle() + "\n" + complaint.getDescription();

        // Classify using Gemini
        AiClassificationResult aiResult = geminiAiProvider.classify(complaintText);
        if (aiResult != null && aiResult.getSector() != null && !aiResult.getSector().isBlank()) {
            // Store all AI-generated classification results
            complaint.setSector(aiResult.getSector());
            complaint.setDepartment(aiResult.getDepartment());
            if (aiResult.getUrgency() != null && !aiResult.getUrgency().isBlank()) {
                complaint.setUrgency(aiResult.getUrgency());
            }
            complaint.setSentiment(aiResult.getSentiment());
            if (aiResult.getPriority() != null && !aiResult.getPriority().isBlank()) {
                complaint.setPriority(aiResult.getPriority());
            }
            complaint.setConfidence(aiResult.getConfidence());
            complaint.setSummary(aiResult.getSummary());
        } else if (complaint.getSector() == null || complaint.getSector().isBlank()) {
            throw new BadRequestException("AI classification failed: sector is missing and no sector was provided");
        }

        complaint.setCitizen(citizen);

        // Condition 2: If client passes a complaintId and it's unique, keep as is; if duplicate, return 409
        if (complaint.getComplaintId() != null && !complaint.getComplaintId().isBlank()) {
            String clientProvidedId = complaint.getComplaintId().trim();
            if (complaintRepository.existsByComplaintId(clientProvidedId)) {
                throw new ConflictException("Complaint with ID '" + clientProvidedId + "' already exists");
            }
            complaint.setComplaintId(clientProvidedId);
            return complaintRepository.save(complaint);
        }

        // Condition 2: Format CR-YYYY-NNNN with yearly reset, max 3 retries on conflict
        int currentYear = LocalDateTime.now().getYear();
        String prefix = "CR-" + currentYear + "-";
        int maxAttempts = 3;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                long nextSeq = getNextSequenceNumber(prefix);
                complaint.setComplaintId(String.format("%s%04d", prefix, nextSeq));
                return complaintRepository.save(complaint);
            } catch (DataIntegrityViolationException e) {
                if (attempt == maxAttempts) {
                    throw new ConflictException("Failed to generate a unique ticket ID after " + maxAttempts + " attempts. Please retry.");
                }
            }
        }

        return complaintRepository.save(complaint);
    }

    private synchronized long getNextSequenceNumber(String prefix) {
        try {
            Optional<Complaint> latest = complaintRepository.findTopByComplaintIdStartingWithOrderByComplaintIdDesc(prefix);
            if (latest.isPresent()) {
                String latestId = latest.get().getComplaintId();
                int lastDash = latestId.lastIndexOf('-');
                if (lastDash != -1 && lastDash < latestId.length() - 1) {
                    try {
                        return Long.parseLong(latestId.substring(lastDash + 1)) + 1;
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return 1L;
    }

    @Override
    public Complaint updateComplaintStatus(Long id, ComplaintStatus status, String officerNotes, Long officerOrAdminId, boolean isAdmin) {
        return updateComplaintStatus(String.valueOf(id), status, officerNotes, null, officerOrAdminId, isAdmin);
    }

    @Override
    public Complaint updateComplaintStatus(String complaintIdOrId, ComplaintStatus newStatus, String remarks, String proofUrl, Long officerOrAdminId, boolean isAdmin) {
        Complaint complaint = null;
        if (complaintIdOrId != null) {
            complaint = complaintRepository.findByComplaintId(complaintIdOrId).orElse(null);
            if (complaint == null && complaintIdOrId.matches("^\\d+$")) {
                complaint = complaintRepository.findById(Long.parseLong(complaintIdOrId)).orElse(null);
            }
        }

        if (complaint == null) {
            throw new ResourceNotFoundException("Complaint not found with identifier: " + complaintIdOrId);
        }

        if (!isAdmin) {
            User officer = userRepository.findById(officerOrAdminId)
                    .orElseThrow(() -> new ResourceNotFoundException("Officer not found with ID: " + officerOrAdminId));

            boolean isAssigned = complaint.getAssignedOfficer() != null
                    && Objects.equals(complaint.getAssignedOfficer().getId(), officerOrAdminId);
            boolean isSameDept = officer.getDepartment() != null
                    && officer.getDepartment().equalsIgnoreCase(complaint.getDepartment());

            if (!isAssigned && !isSameDept) {
                throw new UnauthorizedException("Officer cannot update complaints outside their assignment or department");
            }
        }

        complaint.setStatus(newStatus);
        if (remarks != null && !remarks.isBlank()) {
            complaint.setOfficerNotes(remarks);
        }
        if (proofUrl != null && !proofUrl.isBlank()) {
            complaint.setProofUrl(proofUrl);
        }

        if (newStatus == ComplaintStatus.RESOLVED) {
            complaint.setResolvedAt(LocalDateTime.now());
        }

        return complaintRepository.save(complaint);
    }

    @Override
    public Complaint assignOfficer(Long id, Long officerId) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with ID: " + id));

        User officer = userRepository.findById(officerId)
                .orElseThrow(() -> new ResourceNotFoundException("Officer not found with ID: " + officerId));

        if (officer.getRole() != Role.ROLE_OFFICER && officer.getRole() != Role.ROLE_ADMIN) {
            throw new IllegalArgumentException("Assigned user must have OFFICER or ADMIN role");
        }

        complaint.setAssignedOfficer(officer);
        if (complaint.getStatus() == ComplaintStatus.SUBMITTED) {
            complaint.setStatus(ComplaintStatus.IN_PROGRESS);
        }

        return complaintRepository.save(complaint);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Complaint> getAllComplaints() {
        return complaintRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public Complaint getComplaintById(Long id) {
        return complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Complaint getComplaintByComplaintId(String complaintId) {
        return complaintRepository.findByComplaintId(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with ticket ID: " + complaintId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Complaint> getComplaintsByStatus(ComplaintStatus status) {
        return complaintRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Complaint> getComplaintsBySector(String sector) {
        return complaintRepository.findBySector(sector);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Complaint> getComplaintsByCitizen(Long citizenId) {
        User citizen = userRepository.findById(citizenId)
                .orElseThrow(() -> new ResourceNotFoundException("Citizen not found with ID: " + citizenId));

        return complaintRepository.findByCitizenOrderByCreatedAtDesc(citizen);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Complaint> getComplaintsByOfficer(Long officerId) {
        User officer = userRepository.findById(officerId)
                .orElseThrow(() -> new ResourceNotFoundException("Officer not found with ID: " + officerId));

        List<Complaint> assigned = complaintRepository.findByAssignedOfficerOrderByCreatedAtDesc(officer);
        if (assigned.isEmpty() && officer.getDepartment() != null && !officer.getDepartment().isBlank()) {
            return complaintRepository.findByDepartmentOrderByCreatedAtDesc(officer.getDepartment());
        }
        return assigned;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Complaint> getComplaintsByDepartment(String department) {
        return complaintRepository.findByDepartmentOrderByCreatedAtDesc(department);
    }

    @Override
    @Transactional(readOnly = true)
    public Complaint getComplaintByIdForCitizen(Long id, Long citizenId) {
        return complaintRepository.findByIdAndCitizen_Id(id, citizenId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found or not owned by citizen"));
    }

    @Override
    @Transactional(readOnly = true)
    public Complaint getComplaintByIdForOfficer(Long id, Long officerId) {
        User officer = userRepository.findById(officerId)
                .orElseThrow(() -> new ResourceNotFoundException("Officer not found with ID: " + officerId));

        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with ID: " + id));

        boolean isAssigned = complaint.getAssignedOfficer() != null
                && Objects.equals(complaint.getAssignedOfficer().getId(), officerId);
        boolean isSameDept = officer.getDepartment() != null
                && officer.getDepartment().equalsIgnoreCase(complaint.getDepartment());

        if (!isAssigned && !isSameDept) {
            throw new UnauthorizedException("Officer cannot view complaints outside their assignment or department");
        }

        return complaint;
    }

    @Override
    public void deleteComplaint(Long id) {
        if (!complaintRepository.existsById(id)) {
            throw new ResourceNotFoundException("Complaint not found with ID: " + id);
        }
        complaintRepository.deleteById(id);
    }
}
