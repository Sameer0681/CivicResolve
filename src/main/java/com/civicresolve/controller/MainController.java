package com.civicresolve.controller;

import com.civicresolve.entity.Role;
import com.civicresolve.repository.UserRepository;
import com.civicresolve.service.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class MainController {

    private final UserRepository userRepository;

    public MainController(UserRepository userRepository) {
        this.userRepository = userRepository;
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

    @GetMapping("/report")
    public String report(Model model,
                         @RequestParam(required = false) String successMsg,
                         @RequestParam(required = false) String errorMsg) {
        model.addAttribute("activePage", "report");
        populateAlerts(model, successMsg, errorMsg);
        return "report/report";
    }

    @GetMapping("/report/submission-success")
    public String submissionSuccess(Model model,
                                    @RequestParam(required = false) String complaintId,
                                    @RequestParam(required = false) String title,
                                    @RequestParam(required = false) String description,
                                    @RequestParam(required = false) String sector,
                                    @RequestParam(required = false) String location,
                                    @RequestParam(required = false) String urgency,
                                    @RequestParam(required = false) String dept) {
        if (complaintId == null || complaintId.isBlank()) {
            return "redirect:/report";
        }
        model.addAttribute("complaintId", complaintId);
        model.addAttribute("title", title);
        model.addAttribute("description", description);
        model.addAttribute("sector", sector);
        model.addAttribute("location", location);
        model.addAttribute("urgency", urgency);
        model.addAttribute("dept", dept);
        return "report/submission-success";
    }

    @GetMapping("/citizen/dashboard")
    public String citizenDashboard(Model model) {
        model.addAttribute("activePage", "dashboard");
        return "citizen/dashboard";
    }

    @GetMapping("/citizen/complaints")
    public String citizenComplaints(Model model,
                                    @RequestParam(required = false) String successMsg,
                                    @RequestParam(required = false) String errorMsg) {
        model.addAttribute("activePage", "complaints");
        populateAlerts(model, successMsg, errorMsg);
        return "citizen/complaints";
    }

    @GetMapping("/citizen/complaint-details")
    public String citizenComplaintDetails(Model model,
                                          @RequestParam(required = false) String id,
                                          @RequestParam(required = false) String title,
                                          @RequestParam(required = false) String description,
                                          @RequestParam(required = false) String sector,
                                          @RequestParam(required = false) String location,
                                          @RequestParam(required = false) String urgency,
                                          @RequestParam(required = false) String dept) {
        model.addAttribute("activePage", "complaints");
        model.addAttribute("complaintId", id);
        model.addAttribute("title", title);
        model.addAttribute("description", description);
        model.addAttribute("sector", sector);
        model.addAttribute("location", location);
        model.addAttribute("urgency", urgency);
        model.addAttribute("dept", dept);
        return "citizen/complaint-details";
    }

    @GetMapping("/citizen/notifications")
    public String citizenNotifications(Model model) {
        model.addAttribute("activePage", "notifications");
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

    @GetMapping("/officer/dashboard")
    public String officerDashboard(Model model) {
        model.addAttribute("activePage", "dashboard");
        return "officer/dashboard";
    }

    @GetMapping("/officer/complaints")
    public String officerComplaints(Model model,
                                    @RequestParam(required = false) String successMsg,
                                    @RequestParam(required = false) String errorMsg) {
        model.addAttribute("activePage", "complaints");
        populateAlerts(model, successMsg, errorMsg);
        return "officer/complaints";
    }

    @GetMapping("/officer/complaint-details")
    public String officerComplaintDetails(Model model) {
        model.addAttribute("activePage", "complaints");
        return "officer/complaint-details";
    }

    @GetMapping("/officer/escalations")
    public String officerEscalations(Model model) {
        model.addAttribute("activePage", "escalations");
        return "officer/escalations";
    }

    @GetMapping("/officer/notifications")
    public String officerNotifications(Model model) {
        model.addAttribute("activePage", "notifications");
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

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {
        model.addAttribute("activePage", "dashboard");
        return "admin/dashboard";
    }

    @GetMapping("/admin/complaints")
    public String adminComplaints(Model model,
                                  @RequestParam(required = false) String successMsg,
                                  @RequestParam(required = false) String errorMsg) {
        model.addAttribute("activePage", "complaints");
        populateAlerts(model, successMsg, errorMsg);
        return "admin/complaints";
    }

    @GetMapping("/admin/departments")
    public String adminDepartments(Model model,
                                   @RequestParam(required = false) String successMsg,
                                   @RequestParam(required = false) String errorMsg) {
        model.addAttribute("activePage", "departments");
        populateAlerts(model, successMsg, errorMsg);
        return "admin/departments";
    }

    @GetMapping("/admin/escalations")
    public String adminEscalations(Model model) {
        model.addAttribute("activePage", "escalations");
        return "admin/escalations";
    }

    @GetMapping("/admin/users")
    public String adminUsers(Model model,
                             @RequestParam(required = false) String successMsg,
                             @RequestParam(required = false) String errorMsg) {
        model.addAttribute("activePage", "users");
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("officerCount", userRepository.countByRole(Role.ROLE_OFFICER));
        model.addAttribute("adminCount", userRepository.countByRole(Role.ROLE_ADMIN));
        populateAlerts(model, successMsg, errorMsg);
        return "admin/users";
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

    private void addCurrentUser(Model model, CustomUserDetails currentUser) {
        if (currentUser != null) {
            model.addAttribute("user", currentUser.getUser());
        }
    }

    private void populateAlerts(Model model, String successMsg, String errorMsg) {
        if (successMsg != null && !successMsg.isBlank()) {
            model.addAttribute("successMsg", successMsg);
        }
        if (errorMsg != null && !errorMsg.isBlank()) {
            model.addAttribute("errorMsg", errorMsg);
        }
    }
}
