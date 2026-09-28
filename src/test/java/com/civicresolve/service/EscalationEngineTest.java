package com.civicresolve.service;

import com.civicresolve.entity.*;
import com.civicresolve.repository.ComplaintRepository;
import com.civicresolve.repository.EscalationRepository;
import com.civicresolve.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class EscalationEngineTest {

    private ComplaintRepository complaintRepository;
    private EscalationRepository escalationRepository;
    private UserRepository userRepository;
    private EscalationEngineImpl escalationEngine;

    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        complaintRepository = mock(ComplaintRepository.class);
        escalationRepository = mock(EscalationRepository.class);
        userRepository = mock(UserRepository.class);

        escalationEngine = new EscalationEngineImpl(complaintRepository, escalationRepository, userRepository);
        now = LocalDateTime.of(2026, 10, 1, 12, 0, 0);

        // Mock save calls to return passed argument
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(escalationRepository.save(any(Escalation.class))).thenAnswer(invocation -> {
            Escalation esc = invocation.getArgument(0);
            if (esc.getId() == null) {
                esc.setId((long) (Math.random() * 1000 + 1));
            }
            return esc;
        });
    }

    @Test
    @DisplayName("Test 1 — First SLA breach creates LEVEL_1 escalation")
    void testFirstSlaBreach() {
        Complaint complaint = Complaint.builder()
                .id(101L)
                .complaintId("CR-2025-1001")
                .title("Broken Pipe")
                .status(ComplaintStatus.SUBMITTED)
                .currentEscalationLevel(null)
                .slaDeadline(now.minusHours(5))
                .build();

        when(complaintRepository.findBySlaDeadlineBeforeAndStatusNot(now, ComplaintStatus.RESOLVED))
                .thenReturn(List.of(complaint));
        when(escalationRepository.existsByComplaintAndEscalationLevel(complaint, 1)).thenReturn(false);

        List<Escalation> result = escalationEngine.processEscalations(now);

        assertEquals(1, result.size());
        Escalation escalation = result.get(0);
        assertEquals(1, escalation.getEscalationLevel());
        assertNull(escalation.getEscalatedBy());
        assertEquals(ComplaintStatus.ESCALATED, complaint.getStatus());
        assertEquals(EscalationLevel.LEVEL_1, complaint.getCurrentEscalationLevel());
    }

    @Test
    @DisplayName("Test 2 — Duplicate prevention prevents duplicate LEVEL_1 record")
    void testDuplicatePrevention() {
        Complaint complaint = Complaint.builder()
                .id(102L)
                .complaintId("CR-2025-1002")
                .status(ComplaintStatus.ESCALATED)
                .currentEscalationLevel(EscalationLevel.LEVEL_1)
                .slaDeadline(now.minusHours(5))
                .build();

        when(complaintRepository.findBySlaDeadlineBeforeAndStatusNot(now, ComplaintStatus.RESOLVED))
                .thenReturn(List.of(complaint));
        // Mock that level 2 record already exists (simulating second run after level 2)
        when(escalationRepository.existsByComplaintAndEscalationLevel(complaint, 2)).thenReturn(true);

        List<Escalation> result = escalationEngine.processEscalations(now);

        assertTrue(result.isEmpty(), "Duplicate escalation record must not be created");
        verify(escalationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test 3 — LEVEL_1 -> LEVEL_2 escalation")
    void testLevel1ToLevel2Escalation() {
        Complaint complaint = Complaint.builder()
                .id(103L)
                .complaintId("CR-2025-1003")
                .status(ComplaintStatus.ESCALATED)
                .currentEscalationLevel(EscalationLevel.LEVEL_1)
                .slaDeadline(now.minusHours(12))
                .build();

        when(complaintRepository.findBySlaDeadlineBeforeAndStatusNot(now, ComplaintStatus.RESOLVED))
                .thenReturn(List.of(complaint));
        when(escalationRepository.existsByComplaintAndEscalationLevel(complaint, 2)).thenReturn(false);

        List<Escalation> result = escalationEngine.processEscalations(now);

        assertEquals(1, result.size());
        Escalation escalation = result.get(0);
        assertEquals(2, escalation.getEscalationLevel());
        assertEquals(EscalationLevel.LEVEL_2, complaint.getCurrentEscalationLevel());
        assertEquals(ComplaintStatus.ESCALATED, complaint.getStatus());
    }

    @Test
    @DisplayName("Test 4 — LEVEL_2 -> LEVEL_3 escalation")
    void testLevel2ToLevel3Escalation() {
        Complaint complaint = Complaint.builder()
                .id(104L)
                .complaintId("CR-2025-1004")
                .status(ComplaintStatus.ESCALATED)
                .currentEscalationLevel(EscalationLevel.LEVEL_2)
                .slaDeadline(now.minusHours(24))
                .build();

        when(complaintRepository.findBySlaDeadlineBeforeAndStatusNot(now, ComplaintStatus.RESOLVED))
                .thenReturn(List.of(complaint));
        when(escalationRepository.existsByComplaintAndEscalationLevel(complaint, 3)).thenReturn(false);

        List<Escalation> result = escalationEngine.processEscalations(now);

        assertEquals(1, result.size());
        Escalation escalation = result.get(0);
        assertEquals(3, escalation.getEscalationLevel());
        assertEquals(EscalationLevel.LEVEL_3, complaint.getCurrentEscalationLevel());
    }

    @Test
    @DisplayName("Test 5 — LEVEL_3 stops escalation (no LEVEL_4)")
    void testLevel3StopsEscalation() {
        Complaint complaint = Complaint.builder()
                .id(105L)
                .complaintId("CR-2025-1005")
                .status(ComplaintStatus.ESCALATED)
                .currentEscalationLevel(EscalationLevel.LEVEL_3)
                .slaDeadline(now.minusHours(48))
                .build();

        when(complaintRepository.findBySlaDeadlineBeforeAndStatusNot(now, ComplaintStatus.RESOLVED))
                .thenReturn(List.of(complaint));

        List<Escalation> result = escalationEngine.processEscalations(now);

        assertTrue(result.isEmpty(), "No escalation record should be created beyond LEVEL_3");
        verify(escalationRepository, never()).save(any());
        assertEquals(EscalationLevel.LEVEL_3, complaint.getCurrentEscalationLevel());
    }

    @Test
    @DisplayName("Test 6 — Resolved complaint is NOT escalated")
    void testResolvedComplaintNotEscalated() {
        // Query returns empty list for RESOLVED status
        when(complaintRepository.findBySlaDeadlineBeforeAndStatusNot(now, ComplaintStatus.RESOLVED))
                .thenReturn(Collections.emptyList());

        List<Escalation> result = escalationEngine.processEscalations(now);

        assertTrue(result.isEmpty());
        verify(escalationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test 7 — SLA not breached is NOT escalated")
    void testSlaNotBreachedNotEscalated() {
        // Query returns empty list when deadline is in future (> now)
        when(complaintRepository.findBySlaDeadlineBeforeAndStatusNot(now, ComplaintStatus.RESOLVED))
                .thenReturn(Collections.emptyList());

        List<Escalation> result = escalationEngine.processEscalations(now);

        assertTrue(result.isEmpty());
        verify(escalationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test 8 — History preservation across LEVEL_1 -> LEVEL_2 -> LEVEL_3")
    void testHistoryPreservation() {
        Complaint complaint = Complaint.builder()
                .id(108L)
                .complaintId("CR-2025-1008")
                .status(ComplaintStatus.SUBMITTED)
                .currentEscalationLevel(null)
                .slaDeadline(now.minusHours(72))
                .build();

        List<Escalation> history = new ArrayList<>();

        // Run 1: null -> LEVEL_1
        when(complaintRepository.findBySlaDeadlineBeforeAndStatusNot(now, ComplaintStatus.RESOLVED))
                .thenReturn(List.of(complaint));
        when(escalationRepository.existsByComplaintAndEscalationLevel(complaint, 1)).thenReturn(false);

        List<Escalation> run1 = escalationEngine.processEscalations(now);
        history.addAll(run1);

        assertEquals(EscalationLevel.LEVEL_1, complaint.getCurrentEscalationLevel());

        // Run 2: LEVEL_1 -> LEVEL_2
        when(escalationRepository.existsByComplaintAndEscalationLevel(complaint, 1)).thenReturn(true);
        when(escalationRepository.existsByComplaintAndEscalationLevel(complaint, 2)).thenReturn(false);

        List<Escalation> run2 = escalationEngine.processEscalations(now);
        history.addAll(run2);

        assertEquals(EscalationLevel.LEVEL_2, complaint.getCurrentEscalationLevel());

        // Run 3: LEVEL_2 -> LEVEL_3
        when(escalationRepository.existsByComplaintAndEscalationLevel(complaint, 2)).thenReturn(true);
        when(escalationRepository.existsByComplaintAndEscalationLevel(complaint, 3)).thenReturn(false);

        List<Escalation> run3 = escalationEngine.processEscalations(now);
        history.addAll(run3);

        assertEquals(EscalationLevel.LEVEL_3, complaint.getCurrentEscalationLevel());

        // Verify history contains 3 distinct historical records (level 1, 2, 3)
        assertEquals(3, history.size());
        assertEquals(1, history.get(0).getEscalationLevel());
        assertEquals(2, history.get(1).getEscalationLevel());
        assertEquals(3, history.get(2).getEscalationLevel());
    }

    @Test
    @DisplayName("Test 9 — Transaction/business consistency after escalation")
    void testTransactionBusinessConsistency() {
        Complaint complaint = Complaint.builder()
                .id(109L)
                .complaintId("CR-2025-1009")
                .status(ComplaintStatus.SUBMITTED)
                .currentEscalationLevel(null)
                .slaDeadline(now.minusHours(10))
                .priority(ComplaintPriority.CRITICAL)
                .build();

        when(complaintRepository.findBySlaDeadlineBeforeAndStatusNot(now, ComplaintStatus.RESOLVED))
                .thenReturn(List.of(complaint));

        List<Escalation> result = escalationEngine.processEscalations(now);

        assertEquals(1, result.size());
        assertNotNull(result.get(0).getId());
        assertEquals(EscalationLevel.LEVEL_1, complaint.getCurrentEscalationLevel());
        assertEquals(ComplaintStatus.ESCALATED, complaint.getStatus());
        verify(complaintRepository).save(complaint);
        verify(escalationRepository).save(any(Escalation.class));
    }
}
