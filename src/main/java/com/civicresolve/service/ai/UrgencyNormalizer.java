package com.civicresolve.service.ai;

import org.springframework.stereotype.Component;

@Component
public class UrgencyNormalizer {

    public String normalize(String rawUrgency) {
        if (rawUrgency == null || rawUrgency.isBlank()) {
            return "MEDIUM";
        }

        String lower = rawUrgency.trim().toLowerCase();

        if (lower.contains("critical") || lower.contains("emergency") || lower.contains("fatal") || lower.contains("life")) {
            return "CRITICAL";
        }
        if (lower.contains("high") || lower.contains("urgent") || lower.contains("immediate") || lower.contains("severe")) {
            return "HIGH";
        }
        if (lower.contains("low") || lower.contains("minor") || lower.contains("slight") || lower.contains("cosmetic")) {
            return "LOW";
        }

        return "MEDIUM";
    }
}
