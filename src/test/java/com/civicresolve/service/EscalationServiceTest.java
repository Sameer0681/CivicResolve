package com.civicresolve.service;

import com.civicresolve.entity.Complaint;
import com.civicresolve.entity.ComplaintStatus;
import com.civicresolve.entity.Escalation;
import com.civicresolve.entity.Role;
import com.civicresolve.entity.User;
import com.civicresolve.exception.BadRequestException;
import com.civicresolve.repository.ComplaintRepository;
import com.civicresolve.repository.EscalationRepository;
import com.civicresolve.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EscalationServiceTest {

    @Mock
    private EscalationRepository escalationRepository;

    @Mock
    private ComplaintRepository complaintRepository;

    @Mock
    private UserRepository userRepository;

    private EscalationService escalationService;
    private Complaint complaint;
    private User officer;
    private User admin;

    @BeforeEach
    void setUp() {
        escalationService = new EscalationService(escalationRepository, complaintRepository, userRepository);
        complaint = Complaint.builder()
                .id(10L)
                .department("Public Works Department (PWD)")
                .status(ComplaintStatus.IN_PROGRESS)
                .build();
        officer = User.builder()
                .id(2L)
                .role(Role.ROLE_OFFICER)
                .department("Public Works Department (PWD)")
                .build();
        admin = User.builder().id(1L).role(Role.ROLE_ADMIN).build();
    }

    @Test
    void escalatesComplaintAndAssignsOfficer() {
        when(complaintRepository.findById(10L)).thenReturn(Optional.of(complaint));
        when(userRepository.findById(2L)).thenReturn(Optional.of(officer));
        when(complaintRepository.save(complaint)).thenReturn(complaint);
        when(escalationRepository.save(any(Escalation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Escalation escalation = escalationService.createEscalation(10L, 2L, admin, "Past due");

        assertEquals(ComplaintStatus.ESCALATED, complaint.getStatus());
        assertEquals(officer, complaint.getAssignedOfficer());
        assertEquals("Past due", escalation.getReason());
        assertEquals(admin, escalation.getEscalatedBy());
        assertEquals(officer, escalation.getAssignedOfficer());
    }

    @Test
    void rejectsEscalationToOfficerOutsideComplaintDepartment() {
        officer.setDepartment("Water Works");
        when(complaintRepository.findById(10L)).thenReturn(Optional.of(complaint));
        when(userRepository.findById(2L)).thenReturn(Optional.of(officer));

        assertThrows(BadRequestException.class,
                () -> escalationService.createEscalation(10L, 2L, admin, "Past due"));
        verify(complaintRepository, never()).save(any(Complaint.class));
        verify(escalationRepository, never()).save(any(Escalation.class));
    }

    @Test
    void resolvesAllEscalationsForResolvedComplaint() {
        Escalation escalation = Escalation.builder()
                .complaint(complaint)
                .status("PENDING")
                .build();
        when(escalationRepository.findByComplaint_Id(10L)).thenReturn(List.of(escalation));

        escalationService.resolveForComplaint(10L);

        assertEquals("RESOLVED", escalation.getStatus());
        verify(escalationRepository).saveAll(List.of(escalation));
    }

    @Test
    void cancelsOpenEscalationsWhenComplaintLeavesEscalatedStatus() {
        Escalation escalation = Escalation.builder()
                .complaint(complaint)
                .status("PENDING")
                .build();
        when(escalationRepository.findByComplaint_Id(10L)).thenReturn(List.of(escalation));

        escalationService.cancelForComplaint(10L);

        assertEquals("CANCELLED", escalation.getStatus());
        verify(escalationRepository).saveAll(List.of(escalation));
    }
}
