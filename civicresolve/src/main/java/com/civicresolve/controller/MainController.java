package com.civicresolve.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    // Landing
    @GetMapping({"/", "/landing"})
    public String index() { return "landing/index"; }

    @GetMapping("/landing/about")
    public String about() { return "landing/about"; }

    @GetMapping("/landing/features")
    public String features() { return "landing/features"; }

    @GetMapping("/landing/sectors")
    public String sectors() { return "landing/sectors"; }

    // Auth
    @GetMapping({"/login", "/auth/login"})
    public String login() { return "auth/login"; }

    @GetMapping({"/register", "/auth/register"})
    public String register() { return "auth/register"; }

    @GetMapping("/forgot-password")
    public String forgotPassword() { return "auth/forgot-password"; }

    @GetMapping("/reset-password")
    public String resetPassword() { return "auth/reset-password"; }

    // Report
    @GetMapping("/report")
    public String report() { return "report/report"; }

    @GetMapping("/report/submission-success")
    public String submissionSuccess() { return "report/submission-success"; }

    // Citizen
    @GetMapping("/citizen/dashboard")
    public String citizenDashboard() { return "citizen/dashboard"; }

    @GetMapping("/citizen/complaints")
    public String citizenComplaints() { return "citizen/complaints"; }

    @GetMapping("/citizen/complaint-details")
    public String citizenComplaintDetails() { return "citizen/complaint-details"; }

    @GetMapping("/citizen/notifications")
    public String citizenNotifications() { return "citizen/notifications"; }

    @GetMapping("/citizen/profile")
    public String citizenProfile() { return "citizen/profile"; }

    // Officer
    @GetMapping("/officer/dashboard")
    public String officerDashboard() { return "officer/dashboard"; }

    @GetMapping("/officer/complaints")
    public String officerComplaints() { return "officer/complaints"; }

    @GetMapping("/officer/complaint-details")
    public String officerComplaintDetails() { return "officer/complaint-details"; }

    @GetMapping("/officer/escalations")
    public String officerEscalations() { return "officer/escalations"; }

    @GetMapping("/officer/notifications")
    public String officerNotifications() { return "officer/notifications"; }

    @GetMapping("/officer/profile")
    public String officerProfile() { return "officer/profile"; }

    // Admin
    @GetMapping("/admin/dashboard")
    public String adminDashboard() { return "admin/dashboard"; }

    @GetMapping("/admin/complaints")
    public String adminComplaints() { return "admin/complaints"; }

    @GetMapping("/admin/departments")
    public String adminDepartments() { return "admin/departments"; }

    @GetMapping("/admin/escalations")
    public String adminEscalations() { return "admin/escalations"; }

    @GetMapping("/admin/profile")
    public String adminProfile() { return "admin/profile"; }

    @GetMapping("/admin/users")
    public String adminUsers() { return "admin/users"; }
}
