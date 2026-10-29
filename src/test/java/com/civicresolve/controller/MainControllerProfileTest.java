package com.civicresolve.controller;

import com.civicresolve.entity.Role;
import com.civicresolve.entity.User;
import com.civicresolve.entity.Complaint;
import com.civicresolve.entity.ComplaintStatus;
import com.civicresolve.repository.UserRepository;
import com.civicresolve.service.ComplaintService;
import com.civicresolve.service.CustomUserDetails;
import com.civicresolve.service.EscalationService;
import com.civicresolve.service.OtpService;
import com.civicresolve.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import org.springframework.ui.ExtendedModelMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MainControllerProfileTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ComplaintService complaintService;

    @Mock
    private OtpService otpService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EscalationService escalationService;

    @Mock
    private UserService userService;

    private MainController controller;

    @BeforeEach
    void setUp() {
        controller = new MainController(
                userRepository, complaintService, otpService, passwordEncoder, escalationService, userService);
    }

    @Test
    void updatesProfileNamesForSignedInCitizen() {
        User user = User.builder().firstName("Old").lastName("Name").role(Role.ROLE_CITIZEN).build();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String result = controller.updateCitizenProfile(new CustomUserDetails(user),
                "  New  ", "  Citizen  ", redirectAttributes);

        assertEquals("redirect:/citizen/profile", result);
        assertEquals("New", user.getFirstName());
        assertEquals("Citizen", user.getLastName());
        verify(userRepository).save(user);
    }

    @Test
    void rejectsPasswordChangeWhenCurrentPasswordDoesNotMatch() {
        User user = User.builder().password("encoded-current").role(Role.ROLE_CITIZEN).build();
        when(passwordEncoder.matches("wrong", "encoded-current")).thenReturn(false);
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String result = controller.changeCitizenPassword(new CustomUserDetails(user),
                "wrong", "new-password", "new-password", redirectAttributes);

        assertEquals("redirect:/citizen/profile", result);
        verify(userRepository, never()).save(user);
    }

    @Test
    void savesEncodedPasswordAfterSuccessfulVerification() {
        User user = User.builder().password("encoded-current").role(Role.ROLE_CITIZEN).build();
        when(passwordEncoder.matches("current", "encoded-current")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-new");
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String result = controller.changeCitizenPassword(new CustomUserDetails(user),
                "current", "new-password", "new-password", redirectAttributes);

        assertEquals("redirect:/citizen/profile", result);
        assertEquals("encoded-new", user.getPassword());
        verify(userRepository).save(user);
    }

    @Test
    void officerDashboardShowsLiveComplaintCounts() {
        User user = User.builder().id(8L).role(Role.ROLE_OFFICER).build();
        Complaint open = Complaint.builder().status(ComplaintStatus.IN_PROGRESS).build();
        Complaint resolved = Complaint.builder().status(ComplaintStatus.RESOLVED).build();
        when(complaintService.getComplaintsByOfficer(8L)).thenReturn(java.util.List.of(open, resolved));
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.officerDashboard(model, new CustomUserDetails(user), null, null);

        assertEquals("officer/dashboard", view);
        assertEquals(2, model.getAttribute("totalComplaints"));
        assertEquals(1L, model.getAttribute("openComplaints"));
        assertEquals(1L, model.getAttribute("resolvedComplaints"));
    }

    @Test
    void adminAssignmentRouteUsesComplaintService() {
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.assignComplaintToOfficer(10L, 2L, redirectAttributes);

        assertEquals("redirect:/admin/complaints", view);
        verify(complaintService).assignOfficer(10L, 2L);
        assertTrue(((RedirectAttributesModelMap) redirectAttributes)
                .getFlashAttributes().containsKey("successMsg"));
    }
}
