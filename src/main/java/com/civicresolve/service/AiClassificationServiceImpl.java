package com.civicresolve.service;

import com.civicresolve.dto.AiClassificationResult;
import com.civicresolve.entity.ComplaintPriority;
import com.civicresolve.service.ai.AiProvider;
import com.civicresolve.service.ai.RuleBasedAiProvider;
import com.civicresolve.service.ai.SectorRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class AiClassificationServiceImpl implements AiClassificationService {

    private static final Logger log = LoggerFactory.getLogger(AiClassificationServiceImpl.class);

    private final AiProvider primaryAiProvider;
    private final RuleBasedAiProvider ruleBasedAiProvider;

    public AiClassificationServiceImpl(
            @Qualifier("restAiProvider") AiProvider primaryAiProvider,
            RuleBasedAiProvider ruleBasedAiProvider) {
        this.primaryAiProvider = primaryAiProvider;
        this.ruleBasedAiProvider = ruleBasedAiProvider;
    }

    @Override
    public AiClassificationResult classify(String title, String description) {
        String safeTitle = title != null ? title.trim() : "";
        String safeDescription = description != null ? description.trim() : "";

        AiClassificationResult rawResult = null;
        try {
            rawResult = primaryAiProvider.classify(safeTitle, safeDescription);
        } catch (Exception e) {
            log.error("AI Provider execution error during classification for title '{}': {}. Falling back to RuleBasedAiProvider.", safeTitle, e.getMessage());
            try {
                rawResult = ruleBasedAiProvider.classify(safeTitle, safeDescription);
            } catch (Exception ex) {
                log.error("Rule-based AI fallback error: {}", ex.getMessage());
            }
        }

        if (rawResult == null) {
            rawResult = createControlledFallback(safeTitle, safeDescription);
        }

        return validateAndSanitize(rawResult, safeTitle, safeDescription);
    }

    private AiClassificationResult validateAndSanitize(AiClassificationResult result, String title, String description) {
        String combined = (title + " " + description).trim();

        // 1. Strict Sector Validation
        String validatedSector = SectorRegistry.normalizeSector(result.getSector());
        if (validatedSector == null) {
            log.warn("AI returned invalid sector: '{}'. Performing fallback keyword resolution.", result.getSector());
            validatedSector = SectorRegistry.normalizeSector(combined);
            if (validatedSector == null) {
                validatedSector = SectorRegistry.MUNICIPAL_SERVICES;
            }
        }

        // 2. Department Validation
        String validatedDepartment = result.getDepartment();
        if (validatedDepartment == null || validatedDepartment.isBlank()) {
            validatedDepartment = SectorRegistry.getDepartmentForSector(validatedSector);
        }

        // 3. Priority Validation (Must map strictly to ComplaintPriority enum)
        ComplaintPriority validatedPriority = result.getPriority();
        if (validatedPriority == null) {
            validatedPriority = ComplaintPriority.MEDIUM;
        }

        // 4. Urgency Validation
        String validatedUrgency = result.getUrgency();
        if (validatedUrgency == null || validatedUrgency.isBlank()) {
            validatedUrgency = validatedPriority.name();
        }

        // 5. Sentiment Validation
        String validatedSentiment = result.getSentiment();
        if (validatedSentiment == null || validatedSentiment.isBlank()) {
            validatedSentiment = "NEUTRAL";
        }

        // 6. Confidence Score Bounds Check [0.0, 1.0]
        Double rawConfidence = result.getConfidence();
        double validatedConfidence;
        if (rawConfidence == null || rawConfidence.isNaN()) {
            validatedConfidence = 0.85;
        } else {
            validatedConfidence = Math.max(0.0, Math.min(1.0, rawConfidence));
        }

        // 7. Summary Validation
        String validatedSummary = result.getSummary();
        if (validatedSummary == null || validatedSummary.isBlank()) {
            if (!title.isBlank()) {
                validatedSummary = title;
            } else if (!description.isBlank()) {
                validatedSummary = description.length() > 80 ? description.substring(0, 77) + "..." : description;
            } else {
                validatedSummary = "Grievance classification summary unavailable";
            }
        }

        return AiClassificationResult.builder()
                .sector(validatedSector)
                .department(validatedDepartment)
                .urgency(validatedUrgency)
                .sentiment(validatedSentiment)
                .priority(validatedPriority)
                .confidence(validatedConfidence)
                .summary(validatedSummary)
                .build();
    }

    private AiClassificationResult createControlledFallback(String title, String description) {
        String combined = (title + " " + description).trim();
        String sector = SectorRegistry.normalizeSector(combined);
        if (sector == null) {
            sector = SectorRegistry.MUNICIPAL_SERVICES;
        }

        return AiClassificationResult.builder()
                .sector(sector)
                .department(SectorRegistry.getDepartmentForSector(sector))
                .urgency("MEDIUM")
                .sentiment("NEUTRAL")
                .priority(ComplaintPriority.MEDIUM)
                .confidence(0.50)
                .summary(!title.isBlank() ? title : "Controlled fallback grievance classification")
                .build();
    }
}
