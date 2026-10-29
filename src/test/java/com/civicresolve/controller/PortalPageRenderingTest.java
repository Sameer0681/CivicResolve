package com.civicresolve.controller;

import com.civicresolve.entity.Complaint;
import com.civicresolve.entity.ComplaintStatus;
import com.civicresolve.entity.Escalation;
import com.civicresolve.entity.Role;
import com.civicresolve.entity.User;
import com.civicresolve.repository.UserRepository;
import com.civicresolve.service.ComplaintService;
import com.civicresolve.service.CustomUserDetails;
import com.civicresolve.service.EscalationService;
import com.civicresolve.service.OtpService;
import com.civicresolve.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class PortalPageRenderingTest {

    @Autowired
    private WebApplicationContext context;
    private MockMvc mockMvc;

    @MockitoBean
    private ComplaintService complaintService;

    @MockitoBean
    private EscalationService escalationService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private OtpService otpService;

    private User citizen;
    private User officer;
    private User admin;
    private Complaint complaint;
    private Escalation escalation;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        citizen = User.builder().id(1L).firstName("Casey").lastName("Citizen")
                .email("citizen@example.test").role(Role.ROLE_CITIZEN).build();
        officer = User.builder().id(2L).firstName("Ravi").lastName("Officer")
                .email("officer@example.test").role(Role.ROLE_OFFICER)
                .department("Public Works Department (PWD)").build();
        admin = User.builder().id(3L).firstName("Alex").lastName("Admin")
                .email("admin@example.test").role(Role.ROLE_ADMIN).build();
        LocalDateTime now = LocalDateTime.now();
        complaint = Complaint.builder()
                .id(10L)
                .complaintId("CR-2026-001")
                .title("Pothole on Main Road")
                .description("A road defect needs repair.")
                .sector("Roads & Transport")
                .department("Public Works Department (PWD)")
                .status(ComplaintStatus.ESCALATED)
                .citizen(citizen)
                .assignedOfficer(officer)
                .createdAt(now)
                .updatedAt(now)
                .build();
        escalation = Escalation.builder()
                .id(5L)
                .complaint(complaint)
                .escalatedBy(admin)
                .assignedOfficer(officer)
                .reason("Past due")
                .status("PENDING")
                .createdAt(now)
                .build();

        when(complaintService.getComplaintsByCitizen(1L)).thenReturn(List.of(complaint));
        when(complaintService.getComplaintsByOfficer(2L)).thenReturn(List.of(complaint));
        when(complaintService.getComplaintByIdForCitizen(10L, 1L)).thenReturn(complaint);
        when(complaintService.getComplaintByIdForOfficer(10L, 2L)).thenReturn(complaint);
        when(complaintService.getAllComplaints()).thenReturn(List.of(complaint));
        when(escalationService.getEscalationsForOfficer(2L)).thenReturn(List.of(escalation));
        when(escalationService.getAllEscalations()).thenReturn(List.of(escalation));
        when(userRepository.findByRole(Role.ROLE_OFFICER)).thenReturn(List.of(officer));
        when(userRepository.findAll()).thenReturn(List.of(citizen, officer, admin));
        when(userRepository.count()).thenReturn(3L);
        when(userRepository.countByRole(Role.ROLE_OFFICER)).thenReturn(1L);
        when(userRepository.countByRole(Role.ROLE_ADMIN)).thenReturn(1L);
        when(otpService.isOtpRequired()).thenReturn(false);
    }

    @Test
    void rendersCitizenPagesWithLiveComplaintData() throws Exception {
        var principal = user(new CustomUserDetails(citizen));
        mockMvc.perform(get("/citizen/dashboard").with(principal))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("CR-2026-001")));
        mockMvc.perform(get("/citizen/complaints").with(principal))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Pothole on Main Road")));
        mockMvc.perform(get("/citizen/complaint-details").param("id", "10").with(principal))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("A road defect needs repair.")));
        mockMvc.perform(get("/citizen/notifications").with(principal))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Complaint activity")));
        mockMvc.perform(get("/citizen/profile").with(principal)).andExpect(status().isOk());
    }

    @Test
    void rendersOfficerPagesWithComplaintAndEscalationData() throws Exception {
        var principal = user(new CustomUserDetails(officer));
        mockMvc.perform(get("/officer/dashboard").with(principal))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Pothole on Main Road")));
        mockMvc.perform(get("/officer/complaints").with(principal))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("CR-2026-001")));
        mockMvc.perform(get("/officer/complaint-details").param("id", "10").with(principal))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Save update")));
        mockMvc.perform(get("/officer/escalations").with(principal))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Past due")));
        mockMvc.perform(get("/officer/notifications").with(principal))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Complaint activity")));
        mockMvc.perform(get("/officer/profile").with(principal)).andExpect(status().isOk());
    }

    @Test
    void rendersAdminPagesWithLivePlatformData() throws Exception {
        var principal = user(new CustomUserDetails(admin));
        mockMvc.perform(get("/admin/dashboard").with(principal))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Total complaints")));
        mockMvc.perform(get("/admin/complaints").with(principal))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Pothole on Main Road")));
        mockMvc.perform(get("/admin/departments").with(principal))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Public Works Department (PWD)")));
        mockMvc.perform(get("/admin/escalations").with(principal))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Past due")));
        mockMvc.perform(get("/admin/users").with(principal))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Create officer account")));
        mockMvc.perform(get("/admin/profile").with(principal)).andExpect(status().isOk());
    }

    @Test
    void submitsOfficerAndAdminComplaintActions() throws Exception {
        mockMvc.perform(post("/officer/complaint-details/status")
                        .param("id", "10")
                        .param("status", "RESOLVED")
                        .param("remarks", "Repaired")
                        .param("proofUrl", "https://example.test/proof")
                        .with(user(new CustomUserDetails(officer)))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
        verify(complaintService).updateComplaintStatus(
                "10", ComplaintStatus.RESOLVED, "Repaired",
                "https://example.test/proof", 2L, false);

        mockMvc.perform(post("/admin/complaints/assign")
                        .param("complaintId", "10")
                        .param("officerId", "2")
                        .with(user(new CustomUserDetails(admin)))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
        verify(complaintService).assignOfficer(10L, 2L);

        mockMvc.perform(post("/admin/complaints/status")
                        .param("complaintId", "10")
                        .param("status", "RESOLVED")
                        .param("remarks", "Verified")
                        .with(user(new CustomUserDetails(admin)))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
        verify(complaintService).updateComplaintStatus(
                "10", ComplaintStatus.RESOLVED, "Verified", null, null, true);

        mockMvc.perform(post("/admin/complaints/escalate")
                        .param("complaintId", "10")
                        .param("officerId", "2")
                        .param("reason", "Past due")
                        .with(user(new CustomUserDetails(admin)))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
        verify(escalationService).createEscalation(10L, 2L, admin, "Past due");

        mockMvc.perform(post("/admin/complaints/delete")
                        .param("complaintId", "10")
                        .with(user(new CustomUserDetails(admin)))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
        verify(complaintService).deleteComplaint(10L);
    }

    @Test
    void submitsComplaintWithoutSectorAndCarriesMapCoordinates() throws Exception {
        when(complaintService.createComplaint(
                org.mockito.ArgumentMatchers.any(Complaint.class), eq(1L), isNull()))
                .thenReturn(Complaint.builder().complaintId("CR-2026-002").build());

        mockMvc.perform(post("/report")
                        .param("title", "Broken water pipe")
                        .param("description", "Water is leaking into the street.")
                        .param("location", "Main Street")
                        .param("latitude", "19.076000")
                        .param("longitude", "72.877700")
                        .with(user(new CustomUserDetails(citizen)))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        var submitted = org.mockito.ArgumentCaptor.forClass(Complaint.class);
        verify(complaintService).createComplaint(submitted.capture(), eq(1L), isNull());
        assertEquals("Main Street", submitted.getValue().getLocation());
        assertEquals(19.076, submitted.getValue().getLatitude());
        assertEquals(72.8777, submitted.getValue().getLongitude());
        assertNull(submitted.getValue().getSector());
    }

    @Test
    void rejectsIncompleteOrOutOfRangeMapCoordinates() throws Exception {
        for (var coordinates : List.of(
                new String[]{"19.076000", "", "Select a complete location on the map."},
                new String[]{"95", "220", "Latitude must be between -90 and 90."})) {
            mockMvc.perform(post("/report")
                            .param("title", "Broken water pipe")
                            .param("description", "Water is leaking into the street.")
                            .param("location", "Main Street")
                            .param("latitude", coordinates[0])
                            .param("longitude", coordinates[1])
                            .with(user(new CustomUserDetails(citizen)))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString(coordinates[2])));
        }
        org.mockito.Mockito.verifyNoInteractions(complaintService);
    }
}
