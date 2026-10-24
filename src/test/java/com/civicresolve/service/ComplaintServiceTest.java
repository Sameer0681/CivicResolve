package com.civicresolve.service;

import com.civicresolve.ai.AiClassificationResult;
import com.civicresolve.ai.GeminiAiProvider;
import com.civicresolve.entity.Complaint;
import com.civicresolve.entity.ComplaintStatus;
import com.civicresolve.entity.Role;
import com.civicresolve.entity.User;
import com.civicresolve.exception.BadRequestException;
import com.civicresolve.exception.ConflictException;
import com.civicresolve.exception.UnauthorizedException;
import com.civicresolve.repository.ComplaintRepository;
import com.civicresolve.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplaintServiceTest {

    @Mock
    private ComplaintRepository complaintRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GeminiAiProvider geminiAiProvider;

    @Mock
    private OtpService otpService;

    @InjectMocks
    private ComplaintServiceImpl complaintService;

    private User citizen;
    private User officer;
    private User admin;
    private Complaint complaint;

    @BeforeEach
    void setUp() {
        citizen = User.builder()
                .id(1L)
                .firstName("Aarav")
                .lastName("Sharma")
                .email("citizen@civicresolve.in")
                .role(Role.ROLE_CITIZEN)
                .build();

        officer = User.builder()
                .id(2L)
                .firstName("Ramesh")
                .lastName("Verma")
                .email("officer.verma@civicresolve.gov")
                .role(Role.ROLE_OFFICER)
                .department("Roads & Transport")
                .build();

        admin = User.builder()
                .id(3L)
                .firstName("Admin")
                .lastName("User")
                .email("admin@civicresolve.gov")
                .role(Role.ROLE_ADMIN)
                .build();

        complaint = Complaint.builder()
                .id(10L)
                .complaintId("CR-2025-001")
                .title("Pothole on Main Road")
                .description("Dangerous crater on road")
                .sector("Roads & Transport")
                .department("Roads & Transport")
                .status(ComplaintStatus.SUBMITTED)
                .citizen(citizen)
                .build();
    }

    @Test
    void testGetComplaintsByCitizen() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(citizen));
        when(complaintRepository.findByCitizenOrderByCreatedAtDesc(citizen)).thenReturn(List.of(complaint));

        List<Complaint> result = complaintService.getComplaintsByCitizen(1L);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("CR-2025-001", result.get(0).getComplaintId());
    }

    @Test
    void testGetComplaintsByOfficerAssigned() {
        complaint.setAssignedOfficer(officer);
        when(userRepository.findById(2L)).thenReturn(Optional.of(officer));
        when(complaintRepository.findByAssignedOfficerOrderByCreatedAtDesc(officer)).thenReturn(List.of(complaint));

        List<Complaint> result = complaintService.getComplaintsByOfficer(2L);
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testUpdateComplaintStatusByAssignedOfficer() {
        complaint.setAssignedOfficer(officer);
        when(complaintRepository.findByComplaintId("10")).thenReturn(Optional.empty());
        when(complaintRepository.findById(10L)).thenReturn(Optional.of(complaint));
        when(userRepository.findById(2L)).thenReturn(Optional.of(officer));
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Complaint updated = complaintService.updateComplaintStatus(
                10L, ComplaintStatus.RESOLVED, "Repaired asphalt surface", 2L, false);

        assertEquals(ComplaintStatus.RESOLVED, updated.getStatus());
        assertEquals("Repaired asphalt surface", updated.getOfficerNotes());
        assertNotNull(updated.getResolvedAt());
    }

    @Test
    void testUpdateComplaintStatusByAdmin() {
        when(complaintRepository.findByComplaintId("10")).thenReturn(Optional.empty());
        when(complaintRepository.findById(10L)).thenReturn(Optional.of(complaint));
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Complaint updated = complaintService.updateComplaintStatus(
                10L, ComplaintStatus.IN_PROGRESS, "Work initiated by admin", 3L, true);

        assertEquals(ComplaintStatus.IN_PROGRESS, updated.getStatus());
        assertEquals("Work initiated by admin", updated.getOfficerNotes());
    }

    @Test
    void testAssignOfficerByAdmin() {
        when(complaintRepository.findById(10L)).thenReturn(Optional.of(complaint));
        when(userRepository.findById(2L)).thenReturn(Optional.of(officer));
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Complaint assigned = complaintService.assignOfficer(10L, 2L);

        assertEquals(officer, assigned.getAssignedOfficer());
        assertEquals(ComplaintStatus.IN_PROGRESS, assigned.getStatus());
    }

    @Test
    void testPublicTrackingByComplaintId() {
        when(complaintRepository.findByComplaintId("CR-2025-001")).thenReturn(Optional.of(complaint));

        Complaint result = complaintService.getComplaintByComplaintId("CR-2025-001");
        assertNotNull(result);
        assertEquals("CR-2025-001", result.getComplaintId());
    }

    @Test
    void testCreateComplaintAutoGeneratesYearlyTicketId() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(citizen));
        AiClassificationResult aiResult = new AiClassificationResult();
        aiResult.setSector("Roads & Transport");
        aiResult.setDepartment("Public Works Department");
        when(geminiAiProvider.classify(any(String.class))).thenReturn(aiResult);

        Complaint prevComplaint = Complaint.builder().complaintId("CR-" + LocalDateTime.now().getYear() + "-0005").build();
        when(complaintRepository.findTopByComplaintIdStartingWithOrderByComplaintIdDesc(any(String.class)))
                .thenReturn(Optional.of(prevComplaint));
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(inv -> inv.getArgument(0));

        Complaint newComplaint = Complaint.builder()
                .title("Street Light Broken")
                .description("Light pole #4 is dark")
                .build();

        Complaint created = complaintService.createComplaint(newComplaint, 1L, null);

        assertNotNull(created.getComplaintId());
        String expectedId = "CR-" + LocalDateTime.now().getYear() + "-0006";
        assertEquals(expectedId, created.getComplaintId());
        assertEquals("Roads & Transport", created.getSector());
    }

    @Test
    void testCreateComplaintDuplicateClientComplaintIdThrowsConflict() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(citizen));
        when(complaintRepository.existsByComplaintId("CR-CUSTOM-999")).thenReturn(true);

        Complaint clientComplaint = Complaint.builder()
                .title("Duplicate ticket")
                .description("Testing conflict detection")
                .sector("Roads & Transport")
                .complaintId("CR-CUSTOM-999")
                .build();

        assertThrows(ConflictException.class, () -> complaintService.createComplaint(clientComplaint, 1L, null));
    }

    @Test
    void testUpdateComplaintStatusWithProofAndTicketString() {
        when(complaintRepository.findByComplaintId("CR-2025-001")).thenReturn(Optional.of(complaint));
        when(userRepository.findById(2L)).thenReturn(Optional.of(officer));
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(inv -> inv.getArgument(0));

        Complaint updated = complaintService.updateComplaintStatus(
                "CR-2025-001",
                ComplaintStatus.RESOLVED,
                "Repaired and inspected",
                "https://storage.civicresolve.in/proofs/101.jpg",
                2L,
                false);

        assertEquals(ComplaintStatus.RESOLVED, updated.getStatus());
        assertEquals("Repaired and inspected", updated.getOfficerNotes());
        assertEquals("https://storage.civicresolve.in/proofs/101.jpg", updated.getProofUrl());
        assertNotNull(updated.getResolvedAt());
    }

    @Test
    void testUnauthorizedOfficerCannotUpdateDifferentDepartment() {
        User otherOfficer = User.builder()
                .id(4L)
                .department("Water Works")
                .role(Role.ROLE_OFFICER)
                .build();

        when(complaintRepository.findByComplaintId("CR-2025-001")).thenReturn(Optional.of(complaint));
        when(userRepository.findById(4L)).thenReturn(Optional.of(otherOfficer));

        assertThrows(UnauthorizedException.class, () ->
                complaintService.updateComplaintStatus("CR-2025-001", ComplaintStatus.IN_PROGRESS, "trying", null, 4L, false));
    }
}
