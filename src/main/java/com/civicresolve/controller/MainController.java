package com.civicresolve.controller;

import com.civicresolve.ai.CivicSectorRegistry;
import com.civicresolve.dto.ComplaintSubmissionForm;
import com.civicresolve.dto.DepartmentSummary;
import com.civicresolve.dto.OfficerCreateRequest;
import com.civicresolve.dto.OtpSendRequest;
import com.civicresolve.dto.OtpSendResponse;
import com.civicresolve.dto.OtpVerifyResponse;
import com.civicresolve.entity.Complaint;
import com.civicresolve.entity.ComplaintStatus;
import com.civicresolve.entity.Role;
import com.civicresolve.entity.User;
import com.civicresolve.exception.BadRequestException;
import com.civicresolve.exception.ConflictException;
import com.civicresolve.exception.OtpException;
import com.civicresolve.exception.RateLimitException;
import com.civicresolve.exception.ResourceNotFoundException;
import com.civicresolve.exception.UnauthorizedException;
import com.civicresolve.repository.UserRepository;
import com.civicresolve.service.ComplaintService;
import com.civicresolve.service.CustomUserDetails;
import com.civicresolve.service.EscalationService;
import com.civicresolve.service.OtpService;
import com.civicresolve.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Locale;

@Controller
public class MainController {

    private static final String COMPLAINT_OTP_TOKEN = "complaintSubmissionOtpToken";
    private final UserRepository userRepository;
    private final ComplaintService complaintService;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;
    private final EscalationService escalationService;
    private final UserService userService;

    public MainController(UserRepository userRepository, ComplaintService complaintService, OtpService otpService,
                          PasswordEncoder passwordEncoder, EscalationService escalationService,
                          UserService userService) {
        this.userRepository = userRepository;
        this.complaintService = complaintService;
        this.otpService = otpService;
        this.passwordEncoder = passwordEncoder;
        this.escalationService = escalationService;
        this.userService = userService;
    }

    @GetMapping({"/", "/landing"})
    public String index() {
        return "landing/index";
    }

    @GetMapping({"/about", "/landing/about"})
    public String about() {
        return "landing/about";
    }

    @GetMapping({"/features", "/landing/features"})
    public String features() {
        return "landing/features";
    }

    @GetMapping({"/sectors", "/landing/sectors"})
    public String sectors() {
        return "landing/sectors";
    }

    @GetMapping("/track")
    public String trackComplaint(@RequestParam(required = false) String id, Model model) {
        model.addAttribute("trackingId", id);
        if (id != null && !id.isBlank()) {
            try {
                model.addAttribute("trackedComplaint", complaintService.getComplaintByComplaintId(id.trim()));
            } catch (com.civicresolve.exception.ResourceNotFoundException ex) {
                model.addAttribute("trackingError", "No complaint was found with that reference ID. Check it and try again.");
            }
        }
        return "complaint-tracking";
    }

    @GetMapping("/report")
    public String report(Model model,
                         @AuthenticationPrincipal CustomUserDetails currentUser,
                         HttpSession session,
                         @RequestParam(required = false) String successMsg,
                         @RequestParam(required = false) String errorMsg) {
        model.addAttribute("activePage", "report");
        ComplaintSubmissionForm form = new ComplaintSubmissionForm();
        model.addAttribute("complaintForm", form);
        populateReportOptions(model, currentUser, session);
        populateAlerts(model, successMsg, errorMsg);
        return "report/report";
    }

    @PostMapping("/report")
    public String submitComplaint(@Valid @ModelAttribute("complaintForm") ComplaintSubmissionForm form,
                                  BindingResult bindingResult,
                                  @AuthenticationPrincipal CustomUserDetails currentUser,
                                  HttpSession session,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        if (!isCitizen(currentUser)) {
            redirectAttributes.addFlashAttribute("errorMsg", "Sign in with a citizen account to file a complaint.");
            return "redirect:/login";
        }
        if ((form.getLatitude() == null) != (form.getLongitude() == null)) {
            bindingResult.rejectValue("latitude", "coordinates", "Select a complete location on the map.");
        } else if (form.getLatitude() != null
                && (!Double.isFinite(form.getLatitude()) || !Double.isFinite(form.getLongitude())
                || form.getLatitude() < -90 || form.getLatitude() > 90
                || form.getLongitude() < -180 || form.getLongitude() > 180)) {
            bindingResult.rejectValue("latitude", "coordinates", "Select a valid location on the map.");
        }
        if (bindingResult.hasErrors()) {
            populateReportOptions(model, currentUser, session);
            return "report/report";
        }

        String otpToken = (String) session.getAttribute(COMPLAINT_OTP_TOKEN);
        if (otpService.isOtpRequired() && (otpToken == null || otpToken.isBlank())) {
            model.addAttribute("errorMsg", "Verify your phone before submitting this complaint.");
            populateReportOptions(model, currentUser, session);
            return "report/report";
        }

        Complaint complaint = Complaint.builder()
                .title(form.getTitle().trim())
                .description(form.getDescription().trim())
                .location(form.getLocation().trim())
                .latitude(form.getLatitude())
                .longitude(form.getLongitude())
                .build();

        try {
            Complaint created = complaintService.createComplaint(complaint, currentUser.getId(), otpToken);
            session.removeAttribute(COMPLAINT_OTP_TOKEN);
            return "redirect:/report/submission-success?complaintId=" + created.getComplaintId();
        } catch (BadRequestException | ConflictException | OtpException | RateLimitException ex) {
            model.addAttribute("errorMsg", ex.getMessage());
            populateReportOptions(model, currentUser, session);
            return "report/report";
        }
    }

    @PostMapping("/report/otp/send")
    public String sendComplaintOtp(@AuthenticationPrincipal CustomUserDetails currentUser,
                                   @RequestParam(defaultValue = "BOTH") String channel,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        if (!isCitizen(currentUser)) {
            redirectAttributes.addFlashAttribute("errorMsg", "Sign in with a citizen account to verify your phone.");
            return "redirect:/login";
        }

        try {
            OtpSendResponse response = otpService.sendOtp(
                    currentUser.getUser(),
                    OtpSendRequest.builder().channel(channel).build());
            session.removeAttribute(COMPLAINT_OTP_TOKEN);
            redirectAttributes.addFlashAttribute("infoMsg",
                    response.getMessage() + " Verification code sent to " + response.getMaskedPhone() + ".");
        } catch (BadRequestException | OtpException | RateLimitException ex) {
            redirectAttributes.addFlashAttribute("errorMsg", ex.getMessage());
        }
        return "redirect:/report";
    }

    @PostMapping("/report/otp/verify")
    public String verifyComplaintOtp(@AuthenticationPrincipal CustomUserDetails currentUser,
                                     @RequestParam String otp,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        if (!isCitizen(currentUser)) {
            redirectAttributes.addFlashAttribute("errorMsg", "Sign in with a citizen account to verify your phone.");
            return "redirect:/login";
        }

        try {
            OtpVerifyResponse response = otpService.verifyOtp(currentUser.getUser(), otp);
            session.setAttribute(COMPLAINT_OTP_TOKEN, response.getOtpToken());
            redirectAttributes.addFlashAttribute("successMsg", "Phone verified. You can now submit your complaint.");
        } catch (BadRequestException | OtpException | RateLimitException ex) {
            redirectAttributes.addFlashAttribute("errorMsg", ex.getMessage());
        }
        return "redirect:/report";
    }

    @GetMapping("/report/submission-success")
    public String submissionSuccess(Model model,
                                    @RequestParam(required = false) String complaintId,
                                    RedirectAttributes redirectAttributes) {
        if (complaintId == null || complaintId.isBlank()) {
            return "redirect:/report";
        }
        try {
            model.addAttribute("complaint", complaintService.getComplaintByComplaintId(complaintId));
        } catch (com.civicresolve.exception.ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMsg", "That complaint receipt could not be found.");
            return "redirect:/report";
        }
        return "report/submission-success";
    }

    @GetMapping("/citizen/dashboard")
    public String citizenDashboard(Model model,
                                   @AuthenticationPrincipal CustomUserDetails currentUser) {
        model.addAttribute("activePage", "dashboard");
        model.addAttribute("complaints", complaintService.getComplaintsByCitizen(currentUser.getId()));
        return "citizen/dashboard";
    }

    @GetMapping("/citizen/complaints")
    public String citizenComplaints(Model model,
                                    @AuthenticationPrincipal CustomUserDetails currentUser,
                                    @RequestParam(required = false) String successMsg,
                                    @RequestParam(required = false) String errorMsg) {
        model.addAttribute("activePage", "complaints");
        model.addAttribute("complaints", complaintService.getComplaintsByCitizen(currentUser.getId()));
        populateAlerts(model, successMsg, errorMsg);
        return "citizen/complaints";
    }

    @GetMapping("/citizen/complaint-details")
    public String citizenComplaintDetails(Model model,
                                          @RequestParam(required = false) Long id,
                                          @AuthenticationPrincipal CustomUserDetails currentUser) {
        model.addAttribute("activePage", "complaints");
        if (id != null) {
            model.addAttribute("complaint",
                    complaintService.getComplaintByIdForCitizen(id, currentUser.getId()));
        }
        return "citizen/complaint-details";
    }

    @GetMapping("/citizen/notifications")
    public String citizenNotifications(Model model,
                                      @AuthenticationPrincipal CustomUserDetails currentUser) {
        model.addAttribute("activePage", "notifications");
        model.addAttribute("activityComplaints",
                complaintService.getComplaintsByCitizen(currentUser.getId()).stream().limit(20).toList());
        return "citizen/notifications";
    }

    @GetMapping("/citizen/profile")
    public String citizenProfile(Model model,
                                 @AuthenticationPrincipal CustomUserDetails currentUser,
                                 @RequestParam(required = false) String successMsg,
                                 @RequestParam(required = false) String errorMsg) {
        model.addAttribute("activePage", "profile");
        addCurrentUser(model, currentUser);
        populateAlerts(model, successMsg, errorMsg);
        return "citizen/profile";
    }

    @PostMapping("/citizen/profile")
    public String updateCitizenProfile(@AuthenticationPrincipal CustomUserDetails currentUser,
                                       @RequestParam String firstName,
                                       @RequestParam String lastName,
                                       RedirectAttributes redirectAttributes) {
        return updateProfile(currentUser, Role.ROLE_CITIZEN, firstName, lastName, redirectAttributes);
    }

    @PostMapping("/citizen/change-password")
    public String changeCitizenPassword(@AuthenticationPrincipal CustomUserDetails currentUser,
                                        @RequestParam String currentPassword,
                                        @RequestParam String newPassword,
                                        @RequestParam String confirmPassword,
                                        RedirectAttributes redirectAttributes) {
        return changePassword(currentUser, Role.ROLE_CITIZEN, currentPassword, newPassword,
                confirmPassword, redirectAttributes);
    }

    @GetMapping("/officer/dashboard")
    public String officerDashboard(Model model,
                                   @AuthenticationPrincipal CustomUserDetails currentUser,
                                   @RequestParam(required = false) String successMsg,
                                   @RequestParam(required = false) String errorMsg) {
        List<Complaint> complaints = complaintService.getComplaintsByOfficer(currentUser.getId());
        model.addAttribute("activePage", "dashboard");
        model.addAttribute("complaints", complaints.stream().limit(5).toList());
        model.addAttribute("totalComplaints", complaints.size());
        model.addAttribute("openComplaints", countNotResolved(complaints));
        model.addAttribute("resolvedComplaints", countResolved(complaints));
        populateAlerts(model, successMsg, errorMsg);
        return "officer/dashboard";
    }

    @GetMapping("/officer/complaints")
    public String officerComplaints(Model model,
                                    @AuthenticationPrincipal CustomUserDetails currentUser,
                                    @RequestParam(required = false, defaultValue = "") String search,
                                    @RequestParam(required = false, defaultValue = "") String status,
                                    @RequestParam(required = false) String successMsg,
                                    @RequestParam(required = false) String errorMsg) {
        model.addAttribute("activePage", "complaints");
        model.addAttribute("complaints", filterComplaints(
                complaintService.getComplaintsByOfficer(currentUser.getId()), search, status));
        model.addAttribute("search", search);
        model.addAttribute("statusFilter", status);
        model.addAttribute("statuses", List.of(
                ComplaintStatus.SUBMITTED, ComplaintStatus.IN_PROGRESS, ComplaintStatus.RESOLVED));
        populateAlerts(model, successMsg, errorMsg);
        return "officer/complaints";
    }

    @GetMapping("/officer/complaint-details")
    public String officerComplaintDetails(Model model,
                                          @AuthenticationPrincipal CustomUserDetails currentUser,
                                          @RequestParam(required = false) Long id) {
        model.addAttribute("activePage", "complaints");
        if (id != null) {
            model.addAttribute("complaint",
                    complaintService.getComplaintByIdForOfficer(id, currentUser.getId()));
        }
        model.addAttribute("statuses", List.of(
                ComplaintStatus.IN_PROGRESS, ComplaintStatus.RESOLVED));
        return "officer/complaint-details";
    }

    @PostMapping("/officer/complaint-details/status")
    public String updateOfficerComplaintStatus(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam Long id,
            @RequestParam ComplaintStatus status,
            @RequestParam(required = false) String remarks,
            @RequestParam(required = false) String proofUrl,
            RedirectAttributes redirectAttributes) {
        try {
            complaintService.updateComplaintStatus(
                    String.valueOf(id), status, remarks, proofUrl, currentUser.getId(), false);
            redirectAttributes.addFlashAttribute("successMsg", "Complaint status updated.");
        } catch (BadRequestException | ConflictException | ResourceNotFoundException | UnauthorizedException ex) {
            redirectAttributes.addFlashAttribute("errorMsg", ex.getMessage());
        }
        return "redirect:/officer/complaint-details?id=" + id;
    }

    @GetMapping("/officer/escalations")
    public String officerEscalations(Model model,
                                     @AuthenticationPrincipal CustomUserDetails currentUser) {
        model.addAttribute("activePage", "escalations");
        model.addAttribute("escalations", escalationService.getEscalationsForOfficer(currentUser.getId()));
        return "officer/escalations";
    }

    @GetMapping("/officer/notifications")
    public String officerNotifications(Model model,
                                       @AuthenticationPrincipal CustomUserDetails currentUser) {
        model.addAttribute("activePage", "notifications");
        model.addAttribute("activityComplaints",
                complaintService.getComplaintsByOfficer(currentUser.getId()).stream().limit(20).toList());
        return "officer/notifications";
    }

    @GetMapping("/officer/profile")
    public String officerProfile(Model model,
                                 @AuthenticationPrincipal CustomUserDetails currentUser,
                                 @RequestParam(required = false) String successMsg,
                                 @RequestParam(required = false) String errorMsg) {
        model.addAttribute("activePage", "profile");
        addCurrentUser(model, currentUser);
        populateAlerts(model, successMsg, errorMsg);
        return "officer/profile";
    }

    @PostMapping("/officer/profile")
    public String updateOfficerProfile(@AuthenticationPrincipal CustomUserDetails currentUser,
                                       @RequestParam String firstName,
                                       @RequestParam String lastName,
                                       RedirectAttributes redirectAttributes) {
        return updateProfile(currentUser, Role.ROLE_OFFICER, firstName, lastName, redirectAttributes);
    }

    @PostMapping("/officer/change-password")
    public String changeOfficerPassword(@AuthenticationPrincipal CustomUserDetails currentUser,
                                        @RequestParam String currentPassword,
                                        @RequestParam String newPassword,
                                        @RequestParam String confirmPassword,
                                        RedirectAttributes redirectAttributes) {
        return changePassword(currentUser, Role.ROLE_OFFICER, currentPassword, newPassword,
                confirmPassword, redirectAttributes);
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model,
                                 @RequestParam(required = false) String successMsg,
                                 @RequestParam(required = false) String errorMsg) {
        List<Complaint> complaints = complaintService.getAllComplaints();
        model.addAttribute("activePage", "dashboard");
        model.addAttribute("totalComplaints", complaints.size());
        model.addAttribute("openComplaints", countNotResolved(complaints));
        model.addAttribute("resolvedComplaints", countResolved(complaints));
        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("officerCount", userRepository.countByRole(Role.ROLE_OFFICER));
        model.addAttribute("pendingEscalations", escalationService.getAllEscalations().stream()
                .filter(escalation -> "PENDING".equalsIgnoreCase(escalation.getStatus()))
                .count());
        model.addAttribute("recentComplaints", complaints.stream().limit(5).toList());
        populateAlerts(model, successMsg, errorMsg);
        return "admin/dashboard";
    }

    @GetMapping("/admin/complaints")
    public String adminComplaints(Model model,
                                  @RequestParam(required = false, defaultValue = "") String search,
                                  @RequestParam(required = false, defaultValue = "") String status,
                                  @RequestParam(required = false) String successMsg,
                                  @RequestParam(required = false) String errorMsg) {
        model.addAttribute("activePage", "complaints");
        model.addAttribute("complaints", filterComplaints(complaintService.getAllComplaints(), search, status));
        model.addAttribute("search", search);
        model.addAttribute("statusFilter", status);
        model.addAttribute("statuses", List.of(
                ComplaintStatus.SUBMITTED, ComplaintStatus.IN_PROGRESS, ComplaintStatus.RESOLVED));
        model.addAttribute("officers", userRepository.findByRole(Role.ROLE_OFFICER));
        populateAlerts(model, successMsg, errorMsg);
        return "admin/complaints";
    }

    @PostMapping("/admin/complaints/assign")
    public String assignComplaintToOfficer(@RequestParam Long complaintId,
                                           @RequestParam Long officerId,
                                           RedirectAttributes redirectAttributes) {
        try {
            complaintService.assignOfficer(complaintId, officerId);
            redirectAttributes.addFlashAttribute("successMsg", "Complaint assigned to the officer.");
        } catch (BadRequestException | ConflictException | ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMsg", ex.getMessage());
        }
        return "redirect:/admin/complaints";
    }

    @PostMapping("/admin/complaints/status")
    public String updateAdminComplaintStatus(@RequestParam Long complaintId,
                                             @RequestParam ComplaintStatus status,
                                             @RequestParam(required = false) String remarks,
                                             RedirectAttributes redirectAttributes) {
        try {
            complaintService.updateComplaintStatus(
                    String.valueOf(complaintId), status, remarks, null, null, true);
            redirectAttributes.addFlashAttribute("successMsg", "Complaint status updated.");
        } catch (BadRequestException | ConflictException | ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMsg", ex.getMessage());
        }
        return "redirect:/admin/complaints";
    }

    @PostMapping("/admin/complaints/escalate")
    public String escalateComplaint(@AuthenticationPrincipal CustomUserDetails currentUser,
                                    @RequestParam Long complaintId,
                                    @RequestParam Long officerId,
                                    @RequestParam String reason,
                                    RedirectAttributes redirectAttributes) {
        try {
            escalationService.createEscalation(
                    complaintId, officerId, currentUser.getUser(), reason);
            redirectAttributes.addFlashAttribute("successMsg", "Complaint escalated and assigned.");
        } catch (BadRequestException | ConflictException | ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMsg", ex.getMessage());
        }
        return "redirect:/admin/complaints";
    }

    @PostMapping("/admin/complaints/delete")
    public String deleteAdminComplaint(@RequestParam Long complaintId,
                                       RedirectAttributes redirectAttributes) {
        try {
            complaintService.deleteComplaint(complaintId);
            redirectAttributes.addFlashAttribute("successMsg", "Complaint deleted.");
        } catch (ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMsg", ex.getMessage());
        }
        return "redirect:/admin/complaints";
    }

    @GetMapping("/admin/departments")
    public String adminDepartments(Model model,
                                   @RequestParam(required = false) String successMsg,
                                   @RequestParam(required = false) String errorMsg) {
        List<Complaint> complaints = complaintService.getAllComplaints();
        List<User> officers = userRepository.findByRole(Role.ROLE_OFFICER);
        List<DepartmentSummary> departments = CivicSectorRegistry.getSectorToDepartmentMap().entrySet().stream()
                .map(entry -> {
                    List<Complaint> departmentComplaints = complaints.stream()
                            .filter(complaint -> entry.getValue().equalsIgnoreCase(complaint.getDepartment()))
                            .toList();
                    long openCount = countNotResolved(departmentComplaints);
                    long officerCount = officers.stream()
                            .filter(officer -> entry.getValue().equalsIgnoreCase(officer.getDepartment()))
                            .count();
                    return new DepartmentSummary(
                            entry.getValue(), entry.getKey(), departmentComplaints.size(), openCount, officerCount);
                })
                .toList();
        model.addAttribute("activePage", "departments");
        model.addAttribute("departments", departments);
        model.addAttribute("totalDepartments", departments.size());
        model.addAttribute("totalComplaints", complaints.size());
        model.addAttribute("totalOfficers", officers.size());
        populateAlerts(model, successMsg, errorMsg);
        return "admin/departments";
    }

    @GetMapping("/admin/escalations")
    public String adminEscalations(Model model,
                                   @RequestParam(required = false) String successMsg,
                                   @RequestParam(required = false) String errorMsg) {
        model.addAttribute("activePage", "escalations");
        model.addAttribute("escalations", escalationService.getAllEscalations());
        populateAlerts(model, successMsg, errorMsg);
        return "admin/escalations";
    }

    @GetMapping("/admin/users")
    public String adminUsers(Model model,
                             @RequestParam(required = false, defaultValue = "") String search,
                             @RequestParam(required = false, defaultValue = "") String role,
                             @RequestParam(required = false, defaultValue = "") String status,
                             @RequestParam(required = false) String successMsg,
                             @RequestParam(required = false) String errorMsg) {
        model.addAttribute("activePage", "users");
        model.addAttribute("users", userRepository.findAll().stream()
                .filter(user -> search.isBlank()
                        || user.getFullName().toLowerCase().contains(search.trim().toLowerCase())
                        || user.getEmail().toLowerCase().contains(search.trim().toLowerCase()))
                .filter(user -> role.isBlank() || user.getRole().name().equals(role))
                .filter(user -> status.isBlank()
                        || Boolean.toString(user.isEnabled()).equals(status))
                .toList());
        model.addAttribute("search", search);
        model.addAttribute("roleFilter", role);
        model.addAttribute("statusFilter", status);
        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("officerCount", userRepository.countByRole(Role.ROLE_OFFICER));
        model.addAttribute("adminCount", userRepository.countByRole(Role.ROLE_ADMIN));
        model.addAttribute("officerDepartments", CivicSectorRegistry.getSectorToDepartmentMap().values());
        populateAlerts(model, successMsg, errorMsg);
        return "admin/users";
    }

    @PostMapping("/admin/users/officer")
    public String createOfficer(@Valid @ModelAttribute OfficerCreateRequest request,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMsg",
                    bindingResult.getAllErrors().stream()
                            .findFirst()
                            .map(error -> error.getDefaultMessage())
                            .orElse("Enter valid officer details."));
            return "redirect:/admin/users";
        }
        try {
            userService.createOfficer(request);
            redirectAttributes.addFlashAttribute("successMsg", "Officer account created.");
        } catch (ConflictException ex) {
            redirectAttributes.addFlashAttribute("errorMsg", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/admin/profile")
    public String adminProfile(Model model,
                               @AuthenticationPrincipal CustomUserDetails currentUser,
                               @RequestParam(required = false) String successMsg,
                               @RequestParam(required = false) String errorMsg) {
        model.addAttribute("activePage", "profile");
        addCurrentUser(model, currentUser);
        populateAlerts(model, successMsg, errorMsg);
        return "admin/profile";
    }

    @PostMapping("/admin/profile")
    public String updateAdminProfile(@AuthenticationPrincipal CustomUserDetails currentUser,
                                     @RequestParam String firstName,
                                     @RequestParam String lastName,
                                     RedirectAttributes redirectAttributes) {
        return updateProfile(currentUser, Role.ROLE_ADMIN, firstName, lastName, redirectAttributes);
    }

    @PostMapping("/admin/change-password")
    public String changeAdminPassword(@AuthenticationPrincipal CustomUserDetails currentUser,
                                      @RequestParam String currentPassword,
                                      @RequestParam String newPassword,
                                      @RequestParam String confirmPassword,
                                      RedirectAttributes redirectAttributes) {
        return changePassword(currentUser, Role.ROLE_ADMIN, currentPassword, newPassword,
                confirmPassword, redirectAttributes);
    }

    private String updateProfile(CustomUserDetails currentUser, Role requiredRole,
                                 String firstName, String lastName, RedirectAttributes redirectAttributes) {
        String profilePath = "/" + requiredRole.name().substring("ROLE_".length()).toLowerCase() + "/profile";
        if (currentUser.getRole() != requiredRole) {
            redirectAttributes.addFlashAttribute("errorMsg", "You cannot update this account profile.");
            return "redirect:/" + currentUser.getRole().name().substring("ROLE_".length()).toLowerCase() + "/profile";
        }
        String normalizedFirstName = firstName.trim();
        String normalizedLastName = lastName.trim();
        if (normalizedFirstName.isBlank() || normalizedLastName.isBlank()
                || normalizedFirstName.length() > 80 || normalizedLastName.length() > 80) {
            redirectAttributes.addFlashAttribute("errorMsg", "Enter a first and last name of 1 to 80 characters.");
            return "redirect:" + profilePath;
        }
        currentUser.getUser().setFirstName(normalizedFirstName);
        currentUser.getUser().setLastName(normalizedLastName);
        userRepository.save(currentUser.getUser());
        redirectAttributes.addFlashAttribute("successMsg", "Your profile has been updated.");
        return "redirect:" + profilePath;
    }

    private String changePassword(CustomUserDetails currentUser, Role requiredRole,
                                  String currentPassword, String newPassword, String confirmPassword,
                                  RedirectAttributes redirectAttributes) {
        String profilePath = "/" + requiredRole.name().substring("ROLE_".length()).toLowerCase() + "/profile";
        if (currentUser.getRole() != requiredRole) {
            redirectAttributes.addFlashAttribute("errorMsg", "You cannot change this account password.");
            return "redirect:/" + currentUser.getRole().name().substring("ROLE_".length()).toLowerCase() + "/profile";
        }
        if (!passwordEncoder.matches(currentPassword, currentUser.getPassword())) {
            redirectAttributes.addFlashAttribute("errorMsg", "The current password is incorrect.");
            return "redirect:" + profilePath;
        }
        if (newPassword.length() < 8 || !newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMsg",
                    "The new password must be at least 8 characters and match the confirmation.");
            return "redirect:" + profilePath;
        }
        currentUser.getUser().setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(currentUser.getUser());
        redirectAttributes.addFlashAttribute("successMsg", "Your password has been updated.");
        return "redirect:" + profilePath;
    }

    private void addCurrentUser(Model model, CustomUserDetails currentUser) {
        if (currentUser != null) {
            model.addAttribute("user", currentUser.getUser());
        }
    }

    private void populateReportOptions(Model model, CustomUserDetails currentUser, HttpSession session) {
        model.addAttribute("otpRequired", otpService.isOtpRequired());
        model.addAttribute("otpVerified", session.getAttribute(COMPLAINT_OTP_TOKEN) != null);
        model.addAttribute("isCitizen", isCitizen(currentUser));
    }

    private boolean isCitizen(CustomUserDetails currentUser) {
        return currentUser != null && currentUser.getRole() == Role.ROLE_CITIZEN;
    }

    private void populateAlerts(Model model, String successMsg, String errorMsg) {
        if (successMsg != null && !successMsg.isBlank() && !model.containsAttribute("successMsg")) {
            model.addAttribute("successMsg", successMsg);
        }
        if (errorMsg != null && !errorMsg.isBlank() && !model.containsAttribute("errorMsg")) {
            model.addAttribute("errorMsg", errorMsg);
        }
    }

    private List<Complaint> filterComplaints(List<Complaint> complaints, String search, String status) {
        String normalizedSearch = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        String normalizedStatus = status == null ? "" : status.trim();
        return complaints.stream()
                .filter(complaint -> normalizedSearch.isBlank()
                        || complaint.getComplaintId().toLowerCase(Locale.ROOT).contains(normalizedSearch)
                        || complaint.getTitle().toLowerCase(Locale.ROOT).contains(normalizedSearch)
                        || complaint.getSector().toLowerCase(Locale.ROOT).contains(normalizedSearch))
                .filter(complaint -> normalizedStatus.isBlank()
                        || complaint.getStatus().name().equals(normalizedStatus))
                .toList();
    }

    private long countNotResolved(List<Complaint> complaints) {
        return complaints.stream()
                .filter(complaint -> complaint.getStatus() != ComplaintStatus.RESOLVED)
                .count();
    }

    private long countResolved(List<Complaint> complaints) {
        return complaints.stream()
                .filter(complaint -> complaint.getStatus() == ComplaintStatus.RESOLVED)
                .count();
    }
}
