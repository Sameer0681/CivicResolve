package com.civicresolve.service;

import com.civicresolve.entity.Complaint;
import com.civicresolve.entity.ComplaintStatus;
import com.civicresolve.entity.User;
import com.civicresolve.repository.ComplaintRepository;
import com.civicresolve.repository.UserRepository;
import org.springframework.stereotype.Service;
import com.civicresolve.ai.AiClassificationResult;
import com.civicresolve.ai.GeminiAiProvider;

import java.util.List;

@Service
public class ComplaintServiceImpl implements ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final GeminiAiProvider geminiAiProvider;

    public ComplaintServiceImpl(
            ComplaintRepository complaintRepository,
            UserRepository userRepository,
            GeminiAiProvider geminiAiProvider) {

        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
        this.geminiAiProvider = geminiAiProvider;
    }

    @Override
    public List<Complaint> getComplaintsByCitizen(Long citizenId) {
        User citizen = userRepository.findById(citizenId)
                .orElseThrow(() -> new RuntimeException("Citizen not found"));

        return complaintRepository.findByCitizen(citizen);
    }

    @Override
    public Complaint getComplaintByIdForCitizen(Long id, Long citizenId) {
        return complaintRepository.findByIdAndCitizen_Id(id, citizenId)
                .orElseThrow(() -> new RuntimeException("Complaint not found"));
    }

    @Override
    public Complaint createComplaint(Complaint complaint, Long citizenId) {
        User citizen = userRepository.findById(citizenId)
                .orElseThrow(() -> new RuntimeException("Citizen not found"));

        // Combine title and description for better classification
        String complaintText = complaint.getTitle() + "\n"
                + complaint.getDescription();

        // Classify the complaint using Gemini
        AiClassificationResult aiResult = geminiAiProvider.classify(complaintText);

        // Validate essential AI output before saving
        if (aiResult == null
                || aiResult.getSector() == null
                || aiResult.getSector().isBlank()) {
            throw new RuntimeException(
                    "AI classification failed: sector is missing");
        }

        // Store the AI-generated classification
        complaint.setSector(aiResult.getSector());
        complaint.setDepartment(aiResult.getDepartment());

        // Preserve the existing citizen relationship
        complaint.setCitizen(citizen);

        // Generate a unique tracking ID if one was not provided
        if (complaint.getComplaintId() == null
                || complaint.getComplaintId().isBlank()) {
            complaint.setComplaintId(
                    "CR-" + java.util.UUID.randomUUID()
                            .toString()
                            .substring(0, 8)
                            .toUpperCase());
        }

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
    public void deleteComplaint(Long id) {

        if (!complaintRepository.existsById(id)) {
            throw new RuntimeException("Complaint not found");
        }

        complaintRepository.deleteById(id);
    }
}
