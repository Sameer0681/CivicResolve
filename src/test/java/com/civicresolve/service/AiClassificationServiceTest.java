package com.civicresolve.service;

import com.civicresolve.dto.AiClassificationResult;
import com.civicresolve.entity.ComplaintPriority;
import com.civicresolve.service.ai.AiProvider;
import com.civicresolve.service.ai.RuleBasedAiProvider;
import com.civicresolve.service.ai.SectorRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AiClassificationServiceTest {

    private AiClassificationServiceImpl aiClassificationService;
    private RuleBasedAiProvider ruleBasedAiProvider;

    @BeforeEach
    void setUp() {
        ruleBasedAiProvider = new RuleBasedAiProvider();
        // Use ruleBasedAiProvider as both primary and fallback for unit testing
        aiClassificationService = new AiClassificationServiceImpl(ruleBasedAiProvider, ruleBasedAiProvider);
    }

    @Test
    @DisplayName("1. Road/infrastructure complaint classification test")
    void testRoadInfrastructureComplaint() {
        String title = "Pothole on Main Street";
        String description = "Large pothole causing severe traffic disruption and danger to commuters.";

        AiClassificationResult result = aiClassificationService.classify(title, description);

        assertNotNull(result);
        assertEquals(SectorRegistry.ROADS_AND_TRANSPORT, result.getSector());
        assertTrue(SectorRegistry.isValidSector(result.getSector()));
        assertEquals("Public Works Department (PWD)", result.getDepartment());
        assertNotNull(result.getPriority());
        assertTrue(result.getConfidence() >= 0.0 && result.getConfidence() <= 1.0);
    }

    @Test
    @DisplayName("2. Electricity complaint classification test")
    void testElectricityComplaint() {
        String title = "Transformer failure in Sector 4";
        String description = "Power distribution is down due to a burnt transformer near block B.";

        AiClassificationResult result = aiClassificationService.classify(title, description);

        assertNotNull(result);
        assertEquals(SectorRegistry.ELECTRICITY, result.getSector());
        assertTrue(SectorRegistry.isValidSector(result.getSector()));
        assertEquals("State Power Distribution Corporation (DISCOM)", result.getDepartment());
        assertNotNull(result.getPriority());
        assertTrue(result.getConfidence() >= 0.0 && result.getConfidence() <= 1.0);
    }

    @Test
    @DisplayName("3. Water complaint classification test")
    void testWaterComplaint() {
        String title = "Contaminated drinking water supply";
        String description = "Tap water has foul smell and dirty brown color in residential area.";

        AiClassificationResult result = aiClassificationService.classify(title, description);

        assertNotNull(result);
        assertEquals(SectorRegistry.WATER_SUPPLY, result.getSector());
        assertTrue(SectorRegistry.isValidSector(result.getSector()));
        assertEquals("Jal Nigam / Municipal Water Works", result.getDepartment());
        assertNotNull(result.getPriority());
        assertTrue(result.getConfidence() >= 0.0 && result.getConfidence() <= 1.0);
    }

    @Test
    @DisplayName("4. Healthcare complaint classification test")
    void testHealthcareComplaint() {
        String title = "Mosquito fogging needed at Primary Health Center";
        String description = "Dengue fever outbreak risk high due to stagnant water near civil clinic.";

        AiClassificationResult result = aiClassificationService.classify(title, description);

        assertNotNull(result);
        assertEquals(SectorRegistry.HEALTHCARE, result.getSector());
        assertTrue(SectorRegistry.isValidSector(result.getSector()));
        assertEquals("Chief Medical Office & Public Health Dept", result.getDepartment());
        assertNotNull(result.getPriority());
        assertTrue(result.getConfidence() >= 0.0 && result.getConfidence() <= 1.0);
    }

    @Test
    @DisplayName("5. Education complaint classification test")
    void testEducationComplaint() {
        String title = "Damaged roof in primary school classroom";
        String description = "Government school building roof leaking water during heavy rainfall.";

        AiClassificationResult result = aiClassificationService.classify(title, description);

        assertNotNull(result);
        assertEquals(SectorRegistry.EDUCATION, result.getSector());
        assertTrue(SectorRegistry.isValidSector(result.getSector()));
        assertEquals("Department of Basic & Secondary Education", result.getDepartment());
        assertNotNull(result.getPriority());
        assertTrue(result.getConfidence() >= 0.0 && result.getConfidence() <= 1.0);
    }

    @Test
    @DisplayName("6. Ambiguous wording complaint classification test")
    void testAmbiguousWordingComplaint() {
        String title = "General inconvenience in Ward 12";
        String description = "Things are not proper here, need urgent attention from officials.";

        AiClassificationResult result = aiClassificationService.classify(title, description);

        assertNotNull(result);
        assertTrue(SectorRegistry.isValidSector(result.getSector()), "Sector must belong to 14-sector registry");
        assertNotNull(result.getDepartment());
        assertNotNull(result.getPriority());
        assertTrue(result.getConfidence() >= 0.0 && result.getConfidence() <= 1.0);
    }

    @Test
    @DisplayName("7. Clearly critical complaint classification test")
    void testClearlyCriticalComplaint() {
        String title = "Live high-voltage wire hanging near school gate - Extreme Danger";
        String description = "Emergency! Sparking live wire hanging low. Immediate hazard to children!";

        AiClassificationResult result = aiClassificationService.classify(title, description);

        assertNotNull(result);
        assertTrue(SectorRegistry.isValidSector(result.getSector()));
        assertEquals(ComplaintPriority.CRITICAL, result.getPriority());
        assertEquals("CRITICAL", result.getUrgency());
        assertTrue(result.getConfidence() >= 0.90);
    }

    @Test
    @DisplayName("8. Invalid/malformed AI response handling test")
    void testInvalidMalformedAiResponse() {
        // Mock a faulty provider that returns invalid sector, null priority, out of bounds confidence
        AiProvider malformedProvider = (title, description) -> AiClassificationResult.builder()
                .sector("Invalid Nonexistent Sector 99")
                .department(null)
                .priority(null)
                .confidence(5.0) // Invalid out of bounds
                .summary("")
                .build();

        AiClassificationServiceImpl customService = new AiClassificationServiceImpl(malformedProvider, ruleBasedAiProvider);

        AiClassificationResult result = customService.classify("Test Title", "Test Description");

        assertNotNull(result);
        assertTrue(SectorRegistry.isValidSector(result.getSector()), "Invalid sector must be corrected to a valid canonical sector");
        assertNotNull(result.getDepartment());
        assertNotNull(result.getPriority());
        assertTrue(result.getConfidence() <= 1.0 && result.getConfidence() >= 0.0, "Confidence score must be clamped to [0.0, 1.0]");
        assertFalse(result.getSummary().isBlank());
    }

    @Test
    @DisplayName("9. AI provider failure fallback test")
    void testAiProviderFailure() {
        // Mock a provider that throws an exception
        AiProvider failingProvider = (title, description) -> {
            throw new RuntimeException("AI API Connection Timeout / 503 Service Unavailable");
        };

        AiClassificationServiceImpl customService = new AiClassificationServiceImpl(failingProvider, ruleBasedAiProvider);

        AiClassificationResult result = customService.classify("Burst Water Pipeline", "Major pipeline leakage on MG Road");

        assertNotNull(result, "Application must not crash on AI provider failure");
        assertTrue(SectorRegistry.isValidSector(result.getSector()));
        assertEquals(SectorRegistry.WATER_SUPPLY, result.getSector());
        assertEquals("Jal Nigam / Municipal Water Works", result.getDepartment());
        assertNotNull(result.getPriority());
        assertTrue(result.getConfidence() >= 0.0 && result.getConfidence() <= 1.0);
    }
}
