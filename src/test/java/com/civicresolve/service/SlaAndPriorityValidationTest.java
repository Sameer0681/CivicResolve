package com.civicresolve.service;

import com.civicresolve.entity.Complaint;
import com.civicresolve.entity.ComplaintPriority;
import com.civicresolve.entity.User;
import com.civicresolve.repository.ComplaintRepository;
import com.civicresolve.repository.UserRepository;
import com.civicresolve.service.ai.PrioritySafetyRuleEngine;
import com.civicresolve.service.ai.RuleBasedAiProvider;
import com.civicresolve.service.ai.UrgencyNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SlaAndPriorityValidationTest {

    private SlaPolicyService slaPolicyService;
    private PrioritySafetyRuleEngine prioritySafetyRuleEngine;
    private UrgencyNormalizer urgencyNormalizer;
    private ComplaintServiceImpl complaintService;
    private ComplaintRepository complaintRepository;
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        slaPolicyService = new SlaPolicyService();
        prioritySafetyRuleEngine = new PrioritySafetyRuleEngine();
        urgencyNormalizer = new UrgencyNormalizer();

        RuleBasedAiProvider ruleBasedAiProvider = new RuleBasedAiProvider();
        AiClassificationServiceImpl aiClassificationService = new AiClassificationServiceImpl(ruleBasedAiProvider, ruleBasedAiProvider);

        complaintRepository = mock(ComplaintRepository.class);
        userRepository = mock(UserRepository.class);

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(new User()));
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(invocation -> invocation.getArgument(0));

        complaintService = new ComplaintServiceImpl(
                complaintRepository,
                userRepository,
                aiClassificationService,
                prioritySafetyRuleEngine,
                urgencyNormalizer,
                slaPolicyService
        );
    }

    @Test
    @DisplayName("1. LOW priority -> 7-day SLA test")
    void testLowPrioritySla() {
        Duration duration = slaPolicyService.getSlaDuration(ComplaintPriority.LOW);
        assertEquals(Duration.ofDays(7), duration);
    }

    @Test
    @DisplayName("2. MEDIUM priority -> 5-day SLA test")
    void testMediumPrioritySla() {
        Duration duration = slaPolicyService.getSlaDuration(ComplaintPriority.MEDIUM);
        assertEquals(Duration.ofDays(5), duration);
    }

    @Test
    @DisplayName("3. HIGH priority -> 2-day SLA test")
    void testHighPrioritySla() {
        Duration duration = slaPolicyService.getSlaDuration(ComplaintPriority.HIGH);
        assertEquals(Duration.ofDays(2), duration);
    }

    @Test
    @DisplayName("4. CRITICAL priority -> 24-hour SLA test")
    void testCriticalPrioritySla() {
        Duration duration = slaPolicyService.getSlaDuration(ComplaintPriority.CRITICAL);
        assertEquals(Duration.ofHours(24), duration);
    }

    @Test
    @DisplayName("5. Obvious electrical danger cannot remain LOW")
    void testElectricalDangerPromotionFromLow() {
        Complaint complaint = Complaint.builder()
                .title("Sparking wire near market")
                .description("A severe safety hazard caused by a sparking wire overhead")
                .priority(ComplaintPriority.LOW)
                .build();

        Complaint saved = complaintService.createComplaint(complaint, 1L);

        assertNotNull(saved);
        assertTrue(saved.getPriority() == ComplaintPriority.HIGH || saved.getPriority() == ComplaintPriority.CRITICAL,
                "Electrical danger priority must be promoted to at least HIGH");
    }

    @Test
    @DisplayName("6. Obvious life-threatening situation promoted to CRITICAL")
    void testLifeThreateningPromotionToCritical() {
        Complaint complaint = Complaint.builder()
                .title("Electrocution hazard near school gate")
                .description("Live wire touching iron gate. Immediate life threatening danger!")
                .priority(ComplaintPriority.LOW)
                .build();

        Complaint saved = complaintService.createComplaint(complaint, 1L);

        assertNotNull(saved);
        assertEquals(ComplaintPriority.CRITICAL, saved.getPriority(), "Life threatening danger must be promoted to CRITICAL");
    }

    @Test
    @DisplayName("7. Normal complaint does not get unnecessarily promoted")
    void testNormalComplaintNotPromoted() {
        Complaint complaint = Complaint.builder()
                .title("Garbage collection delay")
                .description("Community bin in block C has not been emptied for two days.")
                .priority(ComplaintPriority.LOW)
                .build();

        Complaint saved = complaintService.createComplaint(complaint, 1L);

        assertNotNull(saved);
        assertEquals(ComplaintPriority.LOW, saved.getPriority(), "Normal low priority complaint should remain LOW");
    }

    @Test
    @DisplayName("8. SLA deadline is calculated from complaint creation time")
    void testSlaDeadlineCalculatedFromCreationTime() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 10, 1, 10, 0, 0);
        Complaint complaint = Complaint.builder()
                .title("Broken streetlight pole")
                .description("Light pole leaning dangerously near road")
                .createdAt(createdAt)
                .priority(ComplaintPriority.HIGH)
                .build();

        Complaint saved = complaintService.createComplaint(complaint, 1L);

        assertNotNull(saved.getSlaDeadline());
        // HIGH priority -> +2 days
        LocalDateTime expectedDeadline = createdAt.plusDays(2);
        assertEquals(expectedDeadline, saved.getSlaDeadline(), "SLA deadline must equal createdAt + 2 days for HIGH priority");
    }

    @Test
    @DisplayName("9. Existing AI fallback still works")
    void testAiFallbackInComplaintCreation() {
        // Test with empty/null title/description
        Complaint complaint = Complaint.builder()
                .title("")
                .description("")
                .build();

        Complaint saved = complaintService.createComplaint(complaint, 1L);

        assertNotNull(saved);
        assertNotNull(saved.getSector());
        assertNotNull(saved.getDepartment());
        assertNotNull(saved.getPriority());
        assertNotNull(saved.getUrgency());
        assertNotNull(saved.getSlaDeadline());
    }
}
