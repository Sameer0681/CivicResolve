package com.civicresolve.service.ai;

import java.util.*;

public class SectorRegistry {

    public static final String ROADS_AND_TRANSPORT = "Roads & Transport";
    public static final String SANITATION_AND_WASTE = "Sanitation & Waste";
    public static final String WATER_SUPPLY = "Water Supply";
    public static final String STREET_LIGHTING = "Street Lighting";
    public static final String ELECTRICITY = "Electricity";
    public static final String HEALTHCARE = "Healthcare";
    public static final String EDUCATION = "Education";
    public static final String PUBLIC_SAFETY = "Public Safety";
    public static final String MUNICIPAL_SERVICES = "Municipal Services";
    public static final String REVENUE_AND_CERTIFICATES = "Revenue & Certificates";
    public static final String SOCIAL_WELFARE = "Social Welfare";
    public static final String ENVIRONMENT = "Environment";
    public static final String HOUSING = "Housing";
    public static final String AGRICULTURE = "Agriculture";

    public static final List<String> CANONICAL_SECTORS = List.of(
            ROADS_AND_TRANSPORT,
            SANITATION_AND_WASTE,
            WATER_SUPPLY,
            STREET_LIGHTING,
            ELECTRICITY,
            HEALTHCARE,
            EDUCATION,
            PUBLIC_SAFETY,
            MUNICIPAL_SERVICES,
            REVENUE_AND_CERTIFICATES,
            SOCIAL_WELFARE,
            ENVIRONMENT,
            HOUSING,
            AGRICULTURE
    );

    public static final Map<String, String> SECTOR_DEPARTMENT_MAP = Map.ofEntries(
            Map.entry(ROADS_AND_TRANSPORT, "Public Works Department (PWD)"),
            Map.entry(SANITATION_AND_WASTE, "Municipal Solid Waste Management Division"),
            Map.entry(WATER_SUPPLY, "Jal Nigam / Municipal Water Works"),
            Map.entry(STREET_LIGHTING, "Municipal Electrical & Lighting Cell"),
            Map.entry(ELECTRICITY, "State Power Distribution Corporation (DISCOM)"),
            Map.entry(HEALTHCARE, "Chief Medical Office & Public Health Dept"),
            Map.entry(EDUCATION, "Department of Basic & Secondary Education"),
            Map.entry(PUBLIC_SAFETY, "City Police & Public Safety Cell"),
            Map.entry(MUNICIPAL_SERVICES, "Municipal Corporation Administration"),
            Map.entry(REVENUE_AND_CERTIFICATES, "Revenue & Tehsil Sub-Divisional Office"),
            Map.entry(SOCIAL_WELFARE, "Social Welfare & Pension Department"),
            Map.entry(ENVIRONMENT, "Pollution Control Board & Parks Department"),
            Map.entry(HOUSING, "Urban Development & Housing Authority"),
            Map.entry(AGRICULTURE, "District Agriculture & Irrigation Department")
    );

    public static boolean isValidSector(String sector) {
        if (sector == null) return false;
        return CANONICAL_SECTORS.contains(sector.trim());
    }

    public static String getDepartmentForSector(String sector) {
        if (sector == null) return "Municipal Corporation Administration";
        String canonical = normalizeSector(sector);
        if (canonical != null) {
            return SECTOR_DEPARTMENT_MAP.getOrDefault(canonical, "Municipal Corporation Administration");
        }
        return "Municipal Corporation Administration";
    }

    public static String normalizeSector(String rawSector) {
        if (rawSector == null || rawSector.isBlank()) {
            return null;
        }

        String trimmed = rawSector.trim();

        // 1. Direct canonical match
        for (String canonical : CANONICAL_SECTORS) {
            if (canonical.equalsIgnoreCase(trimmed)) {
                return canonical;
            }
        }

        String lower = trimmed.toLowerCase();

        // 2. High-specificity primary domain keywords
        if (containsAny(lower, "pothole", "tarmac", "footpath", "traffic light", "traffic signal", "road repair", "broken road")) {
            return ROADS_AND_TRANSPORT;
        }
        if (containsAny(lower, "transformer", "power cut", "discom", "outage", "high voltage", "live wire", "sparking", "blackout", "electric pole")) {
            return ELECTRICITY;
        }
        if (containsAny(lower, "hospital", "clinic", "doctor", "health center", "phc", "dengue", "mosquito fogging", "medicine", "medical")) {
            return HEALTHCARE;
        }
        if (containsAny(lower, "school", "classroom", "teacher", "student", "mid-day meal", "shiksha")) {
            return EDUCATION;
        }
        if (containsAny(lower, "water pipeline", "drinking water", "water supply", "tap water", "jal nigam", "pipe leak", "water shortage", "pipeline leak")) {
            return WATER_SUPPLY;
        }
        if (containsAny(lower, "street light", "streetlight", "light pole", "dark corridor")) {
            return STREET_LIGHTING;
        }
        if (containsAny(lower, "garbage", "dumpster", "open sewer", "choked drain", "solid waste")) {
            return SANITATION_AND_WASTE;
        }
        if (containsAny(lower, "police", "public safety", "encroachment", "nuisance", "crime")) {
            return PUBLIC_SAFETY;
        }
        if (containsAny(lower, "birth certificate", "death certificate", "caste certificate", "tehsil", "mutation")) {
            return REVENUE_AND_CERTIFICATES;
        }
        if (containsAny(lower, "pension", "ration", "social welfare", "disability aid")) {
            return SOCIAL_WELFARE;
        }
        if (containsAny(lower, "pollution", "tree felling", "smoke emission", "air quality", "toxic waste")) {
            return ENVIRONMENT;
        }
        if (containsAny(lower, "pmay", "housing board", "dilapidated building", "illegal construction")) {
            return HOUSING;
        }
        if (containsAny(lower, "kisan", "crop", "farmer", "fertilizer", "irrigation canal", "agriculture")) {
            return AGRICULTURE;
        }

        // 3. Secondary domain keywords
        if (lower.contains("electric") || lower.contains("power")) return ELECTRICITY;
        if (lower.contains("health")) return HEALTHCARE;
        if (lower.contains("education")) return EDUCATION;
        if (lower.contains("water") || lower.contains("pipeline") || lower.contains("leak")) return WATER_SUPPLY;
        if (lower.contains("light")) return STREET_LIGHTING;
        if (lower.contains("waste") || lower.contains("sanitation") || lower.contains("drain")) return SANITATION_AND_WASTE;
        if (lower.contains("road") || lower.contains("transport")) return ROADS_AND_TRANSPORT;
        if (lower.contains("safety")) return PUBLIC_SAFETY;
        if (lower.contains("revenue") || lower.contains("certificate")) return REVENUE_AND_CERTIFICATES;
        if (lower.contains("welfare")) return SOCIAL_WELFARE;
        if (lower.contains("environment")) return ENVIRONMENT;
        if (lower.contains("housing")) return HOUSING;
        if (lower.contains("agri")) return AGRICULTURE;
        if (lower.contains("municipal") || lower.contains("park")) return MUNICIPAL_SERVICES;

        return null;
    }

    private static boolean containsAny(String source, String... keywords) {
        for (String kw : keywords) {
            if (source.contains(kw)) {
                return true;
            }
        }
        return false;
    }
}
