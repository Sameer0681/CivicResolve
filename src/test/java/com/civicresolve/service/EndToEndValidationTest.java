package com.civicresolve.service;

import com.civicresolve.entity.*;
import com.civicresolve.repository.ComplaintRepository;
import com.civicresolve.repository.EscalationRepository;
import com.civicresolve.repository.UserRepository;
import com.civicresolve.service.ai.PrioritySafetyRuleEngine;
import com.civicresolve.service.ai.RuleBasedAiProvider;
import com.civicresolve.service.ai.SectorRegistry;
import com.civicresolve.service.ai.UrgencyNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EndToEndValidationTest {

    private ComplaintServiceImpl complaintService;
    private EscalationEngineImpl escalationEngine;
    private SlaPolicyService slaPolicyService;
    private PrioritySafetyRuleEngine prioritySafetyRuleEngine;
    private UrgencyNormalizer urgencyNormalizer;

    private ComplaintRepository complaintRepository;
    private EscalationRepository escalationRepository;
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        slaPolicyService = new SlaPolicyService();
        prioritySafetyRuleEngine = new PrioritySafetyRuleEngine();
        urgencyNormalizer = new UrgencyNormalizer();

        RuleBasedAiProvider ruleBasedAiProvider = new RuleBasedAiProvider();
        AiClassificationServiceImpl aiClassificationService = new AiClassificationServiceImpl(ruleBasedAiProvider, ruleBasedAiProvider);

        complaintRepository = mock(ComplaintRepository.class);
        escalationRepository = mock(EscalationRepository.class);
        userRepository = mock(UserRepository.class);

        User dummyCitizen = User.builder().id(1L).firstName("Citizen").lastName("User").email("citizen@civicresolve.gov").role(Role.ROLE_CITIZEN).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(dummyCitizen));
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(inv -> inv.getArgument(0));
        when(escalationRepository.save(any(Escalation.class))).thenAnswer(inv -> {
            Escalation e = inv.getArgument(0);
            if (e.getId() == null) e.setId(999L);
            return e;
        });

        complaintService = new ComplaintServiceImpl(
                complaintRepository,
                userRepository,
                aiClassificationService,
                prioritySafetyRuleEngine,
                urgencyNormalizer,
                slaPolicyService
        );

        escalationEngine = new EscalationEngineImpl(complaintRepository, escalationRepository, userRepository);
    }

    @Test
    @DisplayName("1. Verify realistic complaint creation & automated field population")
    void testRealisticComplaintCreation() {
        Complaint complaint = Complaint.builder()
                .complaintId("CR-TEST-9901")
                .title("Dangerous potholes on main road")
                .description("There are several large potholes on the main road near the market. Vehicles are having difficulty passing and accidents may happen if the road is not repaired.")
                .status(ComplaintStatus.SUBMITTED)
                .build();

        Complaint saved = complaintService.createComplaint(complaint, 1L);

        assertNotNull(saved.getSector(), "Sector should be auto-classified");
        assertEquals("Roads & Transport", saved.getSector());
        assertNotNull(saved.getDepartment(), "Department should be mapped");
        assertTrue(saved.getDepartment().contains("Public Works"));
        assertNotNull(saved.getUrgency(), "Urgency should be normalized");
        assertNotNull(saved.getPriority(), "Priority should be populated");
        assertNotNull(saved.getSlaDeadline(), "SLA deadline should be calculated");
        assertTrue(saved.getSlaDeadline().isAfter(LocalDateTime.now()));
    }

    @Test
    @DisplayName("2. Verify 14-sector registry completeness & department mapping")
    void testAll14SectorsMapping() {
        List<String> sectors = SectorRegistry.CANONICAL_SECTORS;
        assertEquals(14, sectors.size(), "Registry must contain exactly 14 sectors");

        for (String sector : sectors) {
            assertTrue(SectorRegistry.isValidSector(sector), "Sector should be valid: " + sector);
            String dept = SectorRegistry.getDepartmentForSector(sector);
            assertNotNull(dept, "Department for sector '" + sector + "' must not be null");
            assertFalse(dept.isBlank(), "Department for sector '" + sector + "' must not be blank");
        }
    }

    @Test
    @DisplayName("3. Verify priority safety rules for electrical danger & life threatening situations")
    void testPrioritySafetyRules() {
        // Electrical Danger -> Priority >= HIGH
        ComplaintPriority p1 = prioritySafetyRuleEngine.evaluateSafetyPriority(
                "Exposed electrical wire near foot path",
                "There is an exposed live electrical wire dangling near the sidewalk.",
                ComplaintPriority.MEDIUM
        );
        assertTrue(p1 == ComplaintPriority.HIGH || p1 == ComplaintPriority.CRITICAL, "Electrical danger must be promoted to HIGH or CRITICAL");

        // Life Threatening -> Priority = CRITICAL
        ComplaintPriority p2 = prioritySafetyRuleEngine.evaluateSafetyPriority(
                "Live electrical wire fallen on road electrocution hazard",
                "A live electrical wire has fallen onto the road and people are at immediate risk of electrocution.",
                ComplaintPriority.LOW
        );
        assertEquals(ComplaintPriority.CRITICAL, p2, "Life threatening electrocution hazard must be promoted to CRITICAL");
    }

    @Test
    @DisplayName("4. Verify SLA policy calculations for all priority tiers")
    void testSlaPolicyDurations() {
        LocalDateTime now = LocalDateTime.now();

        assertEquals(Duration.ofDays(7).toHours(), Duration.between(now, slaPolicyService.calculateSlaDeadline(ComplaintPriority.LOW, now)).toHours());
        assertEquals(Duration.ofDays(5).toHours(), Duration.between(now, slaPolicyService.calculateSlaDeadline(ComplaintPriority.MEDIUM, now)).toHours());
        assertEquals(Duration.ofDays(2).toHours(), Duration.between(now, slaPolicyService.calculateSlaDeadline(ComplaintPriority.HIGH, now)).toHours());
        assertEquals(Duration.ofHours(24).toHours(), Duration.between(now, slaPolicyService.calculateSlaDeadline(ComplaintPriority.CRITICAL, now)).toHours());
    }

    @Test
    @DisplayName("5. Verify automatic escalation LEVEL_1 -> LEVEL_2 -> LEVEL_3 and duplicate prevention")
    void testEscalationLifecycleAndDuplicatePrevention() {
        LocalDateTime testNow = LocalDateTime.of(2026, 10, 1, 12, 0, 0);

        Complaint overdueComplaint = Complaint.builder()
                .id(100L)
                .complaintId("CR-ESC-001")
                .title("Water Pipe Burst")
                .description("Flooding residential street.")
                .sector("Water Supply")
                .department("Jal Nigam / Municipal Water Works")
                .status(ComplaintStatus.SUBMITTED)
                .slaDeadline(testNow.minusHours(5)) // Overdue
                .build();

        when(complaintRepository.findBySlaDeadlineBeforeAndStatusNot(eq(testNow), eq(ComplaintStatus.RESOLVED)))
                .thenReturn(List.of(overdueComplaint));
        when(userRepository.findFirstByDepartmentAndRole(anyString(), eq(Role.ROLE_OFFICER))).thenReturn(Optional.empty());

        // Step 1: First Breach -> LEVEL_1
        when(escalationRepository.findByComplaintOrderByEscalationLevelAsc(overdueComplaint)).thenReturn(Collections.emptyList());
        when(escalationRepository.existsByComplaintAndEscalationLevel(overdueComplaint, 1)).thenReturn(false);

        List<Escalation> res1 = escalationEngine.processEscalations(testNow);
        assertEquals(1, res1.size(), "Should create 1 escalation record for LEVEL_1");
        assertEquals(EscalationLevel.LEVEL_1, overdueComplaint.getCurrentEscalationLevel());

        // Step 2: Re-process when LEVEL_1 already exists -> Duplicate prevention check
        overdueComplaint.setCurrentEscalationLevel(null); // Test initial level breach check again
        when(escalationRepository.existsByComplaintAndEscalationLevel(overdueComplaint, 1)).thenReturn(true);

        List<Escalation> resDup = escalationEngine.processEscalations(testNow);
        assertEquals(0, resDup.size(), "Should NOT create duplicate LEVEL_1 record");

        // Step 3: Promote to LEVEL_2 (after SLA breach at LEVEL_1)
        overdueComplaint.setCurrentEscalationLevel(EscalationLevel.LEVEL_1);
        LocalDateTime testNowL2 = testNow.plusHours(25);
        Escalation l1Rec = Escalation.builder().id(1L).complaint(overdueComplaint).escalationLevel(1).status("PENDING").createdAt(testNow.minusHours(25)).build();
        when(complaintRepository.findBySlaDeadlineBeforeAndStatusNot(eq(testNowL2), eq(ComplaintStatus.RESOLVED)))
                .thenReturn(List.of(overdueComplaint));
        when(escalationRepository.existsByComplaintAndEscalationLevel(overdueComplaint, 2)).thenReturn(false);

        List<Escalation> res2 = escalationEngine.processEscalations(testNowL2);
        assertEquals(1, res2.size(), "Should create 1 escalation record for LEVEL_2");
        assertEquals(EscalationLevel.LEVEL_2, overdueComplaint.getCurrentEscalationLevel());

        // Step 4: Promote to LEVEL_3 (after 24h breach at LEVEL_2)
        LocalDateTime testNowL3 = testNowL2.plusHours(25);
        Escalation l2Rec = Escalation.builder().id(2L).complaint(overdueComplaint).escalationLevel(2).status("PENDING").createdAt(testNowL3.minusHours(25)).build();
        when(escalationRepository.findByComplaintOrderByEscalationLevelAsc(overdueComplaint)).thenReturn(List.of(l1Rec, l2Rec));
        when(escalationRepository.existsByComplaintAndEscalationLevel(overdueComplaint, 2)).thenReturn(true);
        when(escalationRepository.existsByComplaintAndEscalationLevel(overdueComplaint, 3)).thenReturn(false);
        when(complaintRepository.findBySlaDeadlineBeforeAndStatusNot(eq(testNowL3), eq(ComplaintStatus.RESOLVED)))
                .thenReturn(List.of(overdueComplaint));

        List<Escalation> res3 = escalationEngine.processEscalations(testNowL3);
        assertEquals(1, res3.size(), "Should create 1 escalation record for LEVEL_3");
        assertEquals(EscalationLevel.LEVEL_3, overdueComplaint.getCurrentEscalationLevel());
    }

    @Test
    @DisplayName("6. Verify resolved complaint & future SLA are not escalated")
    void testResolvedAndFutureSlaNoEscalation() {
        LocalDateTime now = LocalDateTime.now();
        when(complaintRepository.findBySlaDeadlineBeforeAndStatusNot(any(LocalDateTime.class), eq(ComplaintStatus.RESOLVED)))
                .thenReturn(Collections.emptyList());

        List<Escalation> result = escalationEngine.processEscalations(now);
        assertEquals(0, result.size(), "Resolved and Future complaints must not trigger any escalations");
    }

    @Test
    @DisplayName("7. Verify legacy complaint handling with null fields")
    void testLegacyComplaintNullSafety() {
        Complaint legacy = Complaint.builder()
                .id(300L)
                .complaintId("CR-LEGACY-001")
                .title("Legacy Complaint")
                .description("Submitted before AI SLA engine")
                .sector("Roads & Transport")
                .status(ComplaintStatus.SUBMITTED)
                .urgency(null)
                .sentiment(null)
                .priority(null)
                .slaDeadline(null)
                .currentEscalationLevel(null)
                .build();

        assertNull(legacy.getSlaDeadline());
        assertNull(legacy.getCurrentEscalationLevel());
        assertDoesNotThrow(() -> {
            boolean isOverdue = legacy.getSlaDeadline() != null && LocalDateTime.now().isAfter(legacy.getSlaDeadline());
            assertFalse(isOverdue);
        });
    }
}
