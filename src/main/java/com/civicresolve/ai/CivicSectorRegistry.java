package com.civicresolve.ai;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registry of the 14 official civic sectors and their authoritative department mappings
 * as established in the CivicResolve application taxonomy.
 */
public final class CivicSectorRegistry {

    private static final Map<String, String> SECTOR_TO_DEPARTMENT;

    static {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("Roads & Transport", "Public Works Department (PWD)");
        map.put("Sanitation & Waste", "Municipal Solid Waste Management Division");
        map.put("Water Supply", "Jal Nigam / Municipal Water Works");
        map.put("Street Lighting", "Municipal Electrical & Lighting Cell");
        map.put("Electricity", "State Power Distribution Corporation (DISCOM)");
        map.put("Healthcare", "Chief Medical Office & Public Health Dept");
        map.put("Education", "Department of Basic & Secondary Education");
        map.put("Public Safety", "City Police & Public Safety Cell");
        map.put("Municipal Services", "Municipal Corporation Administration");
        map.put("Revenue & Certificates", "Revenue & Tehsil Sub-Divisional Office");
        map.put("Social Welfare", "Social Welfare & Pension Department");
        map.put("Environment", "Pollution Control Board & Parks Department");
        map.put("Housing", "Urban Development & Housing Authority");
        map.put("Agriculture", "District Agriculture & Irrigation Department");
        SECTOR_TO_DEPARTMENT = Collections.unmodifiableMap(map);
    }

    private CivicSectorRegistry() {
    }

    /**
     * Returns an unmodifiable map of all official sectors to their default departments.
     */
    public static Map<String, String> getSectorToDepartmentMap() {
        return SECTOR_TO_DEPARTMENT;
    }

    /**
     * Returns an unmodifiable set of the 14 official sector names.
     */
    public static Set<String> getOfficialSectors() {
        return SECTOR_TO_DEPARTMENT.keySet();
    }

    /**
     * Resolves a raw sector string to its exact official sector name, ignoring case and surrounding whitespace.
     * Returns null if no match is found.
     */
    public static String findCanonicalSector(String rawSector) {
        if (rawSector == null || rawSector.isBlank()) {
            return null;
        }
        String trimmed = rawSector.trim();
        for (String official : SECTOR_TO_DEPARTMENT.keySet()) {
            if (official.equalsIgnoreCase(trimmed)) {
                return official;
            }
        }
        return null;
    }

    /**
     * Checks if the given sector name matches any of the 14 official sectors.
     */
    public static boolean isValidSector(String rawSector) {
        return findCanonicalSector(rawSector) != null;
    }

    /**
     * Returns the authoritative mapped department for an official sector.
     * Returns null if sector is not recognized.
     */
    public static String getDepartmentForSector(String sector) {
        String canonical = findCanonicalSector(sector);
        return canonical != null ? SECTOR_TO_DEPARTMENT.get(canonical) : null;
    }

    /**
     * Verifies if a given department matches the official department mapped to the sector.
     * Accepts exact match, case-insensitive match, or matches without parenthetical abbreviations.
     */
    public static boolean isDepartmentMatching(String sector, String department) {
        if (sector == null || department == null || department.isBlank()) {
            return false;
        }
        String canonicalDept = getDepartmentForSector(sector);
        if (canonicalDept == null) {
            return false;
        }
        String trimmedDept = department.trim();
        if (canonicalDept.equalsIgnoreCase(trimmedDept)) {
            return true;
        }
        // Match without parentheses (e.g. "Public Works Department" for "Public Works Department (PWD)")
        String strippedCanonical = canonicalDept.replaceAll("\\s*\\([^)]*\\)", "").trim();
        if (strippedCanonical.equalsIgnoreCase(trimmedDept)) {
            return true;
        }
        // Also check if trimmed department contains stripped canonical or vice versa
        return trimmedDept.toLowerCase().contains(strippedCanonical.toLowerCase())
                || strippedCanonical.toLowerCase().contains(trimmedDept.toLowerCase());
    }
}
