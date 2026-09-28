package com.civicresolve.service.ai;

import com.civicresolve.entity.ComplaintPriority;
import org.springframework.stereotype.Component;

@Component
public class PrioritySafetyRuleEngine {

    private static final String[] CRITICAL_LIFE_THREATENING_KEYWORDS = {
            "electrocution",
            "live wire",
            "live electrical wire",
            "exposed wire",
            "fire breakout",
            "explosion",
            "life threatening",
            "life-threatening",
            "building collapse",
            "gas leak",
            "toxic gas",
            "fatal hazard",
            "immediate danger"
    };

    private static final String[] HIGH_HAZARD_KEYWORDS = {
            "fire",
            "major accident",
            "severe safety hazard",
            "public safety hazard",
            "sparking wire",
            "flooded highway",
            "open sewer pit",
            "open manhole",
            "bridge fracture"
    };

    public ComplaintPriority evaluateSafetyPriority(String title, String description, ComplaintPriority initialPriority) {
        ComplaintPriority basePriority = initialPriority != null ? initialPriority : ComplaintPriority.MEDIUM;

        String safeTitle = title != null ? title.toLowerCase() : "";
        String safeDesc = description != null ? description.toLowerCase() : "";
        String combined = (safeTitle + " " + safeDesc).trim();

        // 1. Life-threatening / extreme danger check -> Promotes to CRITICAL
        if (containsAny(combined, CRITICAL_LIFE_THREATENING_KEYWORDS)) {
            return ComplaintPriority.CRITICAL;
        }

        // 2. High hazard / safety indicator check -> Promotes to at least HIGH
        if (containsAny(combined, HIGH_HAZARD_KEYWORDS)) {
            if (basePriority == ComplaintPriority.LOW || basePriority == ComplaintPriority.MEDIUM) {
                return ComplaintPriority.HIGH;
            }
        }

        return basePriority;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) {
                return true;
            }
        }
        return false;
    }
}
