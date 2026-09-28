package com.civicresolve.service.ai;

import com.civicresolve.dto.AiClassificationResult;
import com.civicresolve.entity.ComplaintPriority;
import org.springframework.stereotype.Component;

@Component("ruleBasedAiProvider")
public class RuleBasedAiProvider implements AiProvider {

    @Override
    public AiClassificationResult classify(String title, String description) {
        String safeTitle = title != null ? title.trim() : "";
        String safeDescription = description != null ? description.trim() : "";
        String combined = (safeTitle + " " + safeDescription).trim();
        String lower = combined.toLowerCase();

        // 1. Sector & Department
        String sector = SectorRegistry.normalizeSector(combined);
        if (sector == null) {
            sector = SectorRegistry.MUNICIPAL_SERVICES;
        }
        String department = SectorRegistry.getDepartmentForSector(sector);

        // 2. Urgency, Sentiment, Priority & Confidence
        ComplaintPriority priority = ComplaintPriority.MEDIUM;
        String urgency = "MEDIUM";
        String sentiment = "NEUTRAL";
        double confidence = 0.85;

        // Critical pattern check
        if (containsAny(lower, "emergency", "fire", "live wire", "open sewer", "collapsing", "fatal", "hazard", "explosion", "sparking", "toxic leak", "burst pipe", "danger")) {
            priority = ComplaintPriority.CRITICAL;
            urgency = "CRITICAL";
            sentiment = "HIGHLY_NEGATIVE";
            confidence = 0.95;
        }
        // High priority pattern check
        else if (containsAny(lower, "urgent", "immediately", "severe", "major outage", "overflowing", "broken main", "no water", "blackout", "fever outbreak", "unsafe")) {
            priority = ComplaintPriority.HIGH;
            urgency = "HIGH";
            sentiment = "NEGATIVE";
            confidence = 0.90;
        }
        // Low priority pattern check
        else if (containsAny(lower, "minor", "suggestion", "cosmetic", "paint", "enhancement")) {
            priority = ComplaintPriority.LOW;
            urgency = "LOW";
            sentiment = "NEUTRAL";
            confidence = 0.80;
        }

        // Summary generation
        String summary;
        if (!safeTitle.isBlank()) {
            summary = safeTitle;
        } else if (!safeDescription.isBlank()) {
            summary = safeDescription.length() > 80 ? safeDescription.substring(0, 77) + "..." : safeDescription;
        } else {
            summary = "Grievance classification pending description";
        }

        return AiClassificationResult.builder()
                .sector(sector)
                .department(department)
                .urgency(urgency)
                .sentiment(sentiment)
                .priority(priority)
                .confidence(confidence)
                .summary(summary)
                .build();
    }

    private boolean containsAny(String source, String... keywords) {
        for (String kw : keywords) {
            if (source.contains(kw)) {
                return true;
            }
        }
        return false;
    }
}
