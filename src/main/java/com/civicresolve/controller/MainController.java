package com.civicresolve.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * =============================================================================
 * CIVICRESOLVE - FRONTEND PREVIEW CONTROLLER
 * =============================================================================
 * PURPOSE:
 * This controller allows you to easily render, visually inspect, and verify
 * all 27 frontend templates across Landing, Auth, Citizen, Officer, Admin,
 * and Report sections without needing database records or active logins.
 *
 * QUICK PREVIEW DIRECTORY:
 * Open your browser and navigate to:
 *   http://localhost:8080/pages  (or http://localhost:8080/preview)
 * This shows an interactive dashboard listing every single page in the project
 * with direct clickable links.
 *
 * TO DELETE AFTER VERIFICATION:
 * When you finish verifying all frontend alignments and pages:
 * 1. You can simply delete this file (MainController.java).
 * 2. Replace it with your actual feature/business logic controllers
 *    (e.g., AuthController, CitizenController, OfficerController, AdminController).
 * =============================================================================
 */
@Controller
public class MainController {

    // =========================================================================
    // 0. INTERACTIVE FRONTEND PAGE DIRECTORY & VERIFICATION HUB
    // =========================================================================
    @GetMapping({"/pages", "/preview", "/dev/pages"})
    @ResponseBody
    public String previewHub() {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8"/>
                <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
                <title>Frontend Page Verification Hub | CivicResolve</title>
                <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet"/>
                <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"/>
                <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.0/css/all.min.css" rel="stylesheet"/>
                <style>
                    body { font-family: 'Inter', sans-serif; background-color: #f8fafc; color: #1e293b; padding-bottom: 3rem; }
                    .hub-header { background: linear-gradient(135deg, #0f172a, #1e3a5f); color: #fff; padding: 2.5rem 0; margin-bottom: 2rem; border-bottom: 3px solid #2563eb; }
                    .hub-card { background: #fff; border: 1px solid #e2e8f0; border-radius: 12px; transition: transform .2s, box-shadow .2s; height: 100%; display: flex; flex-direction: column; overflow: hidden; }
                    .hub-card:hover { transform: translateY(-3px); box-shadow: 0 10px 25px rgba(0,0,0,0.06); }
                    .hub-card-header { padding: 1rem 1.25rem; font-weight: 700; border-bottom: 1px solid #f1f5f9; display: flex; align-items: center; justify-content: space-between; }
                    .hub-list { list-style: none; padding: 0; margin: 0; flex: 1; }
                    .hub-item { border-bottom: 1px solid #f8fafc; }
                    .hub-item:last-child { border-bottom: none; }
                    .hub-link { display: flex; align-items: center; justify-content: space-between; padding: .75rem 1.25rem; text-decoration: none; color: #334155; font-size: .9rem; transition: background .15s; }
                    .hub-link:hover { background-color: #f1f5f9; color: #2563eb; }
                    .hub-badge { font-size: .75rem; padding: .25rem .5rem; border-radius: 6px; font-weight: 600; font-family: monospace; background: #e2e8f0; color: #475569; }
                    .badge-pill-total { background: rgba(37,99,235,0.1); color: #2563eb; font-weight: 700; }
                </style>
            </head>
            <body>
                <header class="hub-header">
                    <div class="container">
                        <div class="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3">
                            <div>
                                <div class="d-flex align-items-center gap-2 mb-1">
                                    <span class="badge bg-primary px-2.5 py-1.5"><i class="fa-solid fa-landmark me-1"></i> CivicResolve</span>
                                    <span class="badge bg-success">Frontend Ready</span>
                                </div>
                                <h1 class="h2 fw-800 mb-1">Frontend Page Verification Hub</h1>
                                <p class="mb-0 text-white-50 small">Quickly render and test all 27 frontend views. Delete <code>MainController.java</code> when finished.</p>
                            </div>
                            <div class="text-md-end">
                                <span class="badge bg-light text-dark p-2 fs-6">27 Pages Available</span>
                            </div>
                        </div>
                    </div>
                </header>

                <main class="container">
                    <div class="alert alert-info d-flex align-items-center gap-3 mb-4 shadow-sm border-0">
                        <i class="fa-solid fa-circle-info fa-2x text-primary flex-shrink-0"></i>
                        <div>
                            <strong>Preview Mode Active:</strong> Click any link below to inspect its layout, alignment, responsive drawer, or component styling. Each page renders instantly without requiring database logins.
                        </div>
                    </div>

                    <div class="row g-4">
                        <!-- 1. Landing & Public -->
                        <div class="col-md-6 col-lg-4">
                            <div class="hub-card">
                                <div class="hub-card-header bg-light">
                                    <span class="text-primary"><i class="fa-solid fa-globe me-2"></i>GrievanceProof AI &amp; Landing</span>
                                    <span class="badge badge-pill-total">4 Key Flow Pages</span>
                                </div>
                                <ul class="hub-list">
                                    <li class="hub-item"><a href="/" target="_blank" class="hub-link"><span><i class="fa-solid fa-house me-2 text-primary"></i>Home (Full Flow &amp; CR-2048 Demo)</span><span class="hub-badge">/</span></a></li>
                                    <li class="hub-item"><a href="/features" target="_blank" class="hub-link"><span><i class="fa-solid fa-microchip me-2 text-info"></i>9 Capabilities &amp; 7-Step AI</span><span class="hub-badge">/features</span></a></li>
                                    <li class="hub-item"><a href="/sectors" target="_blank" class="hub-link"><span><i class="fa-solid fa-building-columns me-2 text-warning"></i>14 Government Sectors</span><span class="hub-badge">/sectors</span></a></li>
                                    <li class="hub-item"><a href="/about" target="_blank" class="hub-link"><span><i class="fa-solid fa-graduation-cap me-2 text-success"></i>Architecture, Roadmap &amp; Team</span><span class="hub-badge">/about</span></a></li>
                                </ul>
                            </div>
                        </div>

                        <!-- 2. Authentication -->
                        <div class="col-md-6 col-lg-4">
                            <div class="hub-card">
                                <div class="hub-card-header bg-light">
                                    <span class="text-success"><i class="fa-solid fa-shield-halved me-2"></i>Authentication</span>
                                    <span class="badge badge-pill-total">4 Pages</span>
                                </div>
                                <ul class="hub-list">
                                    <li class="hub-item"><a href="/login" target="_blank" class="hub-link"><span><i class="fa-solid fa-right-to-bracket me-2 text-muted"></i>Login</span><span class="hub-badge">/login</span></a></li>
                                    <li class="hub-item"><a href="/register" target="_blank" class="hub-link"><span><i class="fa-solid fa-user-plus me-2 text-muted"></i>Register</span><span class="hub-badge">/register</span></a></li>
                                    <li class="hub-item"><a href="/forgot-password" target="_blank" class="hub-link"><span><i class="fa-solid fa-key me-2 text-muted"></i>Forgot Password</span><span class="hub-badge">/forgot-password</span></a></li>
                                    <li class="hub-item"><a href="/reset-password" target="_blank" class="hub-link"><span><i class="fa-solid fa-lock-open me-2 text-muted"></i>Reset Password</span><span class="hub-badge">/reset-password</span></a></li>
                                </ul>
                            </div>
                        </div>

                        <!-- 3. Report Flow -->
                        <div class="col-md-6 col-lg-4">
                            <div class="hub-card">
                                <div class="hub-card-header bg-light">
                                    <span class="text-danger"><i class="fa-solid fa-file-circle-plus me-2"></i>Report Flow</span>
                                    <span class="badge badge-pill-total">2 Pages</span>
                                </div>
                                <ul class="hub-list">
                                    <li class="hub-item"><a href="/report" target="_blank" class="hub-link"><span><i class="fa-solid fa-microphone-lines me-2 text-primary"></i>Multimodal AI Report (Text/Voice/Photo)</span><span class="hub-badge">/report</span></a></li>
                                    <li class="hub-item"><a href="/report/submission-success" target="_blank" class="hub-link"><span><i class="fa-solid fa-circle-check me-2 text-success"></i>Dossier Submission Success</span><span class="hub-badge">/report/submission-success</span></a></li>
                                </ul>
                            </div>
                        </div>

                        <!-- 4. Citizen Portal -->
                        <div class="col-md-6 col-lg-4">
                            <div class="hub-card">
                                <div class="hub-card-header bg-light">
                                    <span class="text-info"><i class="fa-solid fa-user me-2"></i>Citizen Portal</span>
                                    <span class="badge badge-pill-total">5 Pages</span>
                                </div>
                                <ul class="hub-list">
                                    <li class="hub-item"><a href="/citizen/dashboard" target="_blank" class="hub-link"><span><i class="fa-solid fa-gauge-high me-2 text-muted"></i>Dashboard</span><span class="hub-badge">/citizen/dashboard</span></a></li>
                                    <li class="hub-item"><a href="/citizen/complaints" target="_blank" class="hub-link"><span><i class="fa-solid fa-file-lines me-2 text-muted"></i>My Complaints</span><span class="hub-badge">/citizen/complaints</span></a></li>
                                    <li class="hub-item"><a href="/citizen/complaint-details" target="_blank" class="hub-link"><span><i class="fa-solid fa-route me-2 text-info"></i>Track Live CR-2048 (Slide 11/12)</span><span class="hub-badge">/citizen/complaint-details</span></a></li>
                                    <li class="hub-item"><a href="/citizen/notifications" target="_blank" class="hub-link"><span><i class="fa-solid fa-bell me-2 text-muted"></i>Notifications</span><span class="hub-badge">/citizen/notifications</span></a></li>
                                    <li class="hub-item"><a href="/citizen/profile" target="_blank" class="hub-link"><span><i class="fa-solid fa-circle-user me-2 text-muted"></i>Profile</span><span class="hub-badge">/citizen/profile</span></a></li>
                                </ul>
                            </div>
                        </div>

                        <!-- 5. Officer Workspace -->
                        <div class="col-md-6 col-lg-4">
                            <div class="hub-card">
                                <div class="hub-card-header bg-light">
                                    <span class="text-warning"><i class="fa-solid fa-user-shield me-2"></i>Officer Workspace</span>
                                    <span class="badge badge-pill-total">6 Pages</span>
                                </div>
                                <ul class="hub-list">
                                    <li class="hub-item"><a href="/officer/dashboard" target="_blank" class="hub-link"><span><i class="fa-solid fa-gauge-high me-2 text-muted"></i>Dashboard</span><span class="hub-badge">/officer/dashboard</span></a></li>
                                    <li class="hub-item"><a href="/officer/complaints" target="_blank" class="hub-link"><span><i class="fa-solid fa-clipboard-list me-2 text-muted"></i>Assigned Complaints</span><span class="hub-badge">/officer/complaints</span></a></li>
                                    <li class="hub-item"><a href="/officer/complaint-details" target="_blank" class="hub-link"><span><i class="fa-solid fa-receipt me-2 text-muted"></i>Complaint Details</span><span class="hub-badge">/officer/complaint-details</span></a></li>
                                    <li class="hub-item"><a href="/officer/escalations" target="_blank" class="hub-link"><span><i class="fa-solid fa-arrow-up-right-dots me-2 text-muted"></i>Escalations</span><span class="hub-badge">/officer/escalations</span></a></li>
                                    <li class="hub-item"><a href="/officer/notifications" target="_blank" class="hub-link"><span><i class="fa-solid fa-bell me-2 text-muted"></i>Notifications</span><span class="hub-badge">/officer/notifications</span></a></li>
                                    <li class="hub-item"><a href="/officer/profile" target="_blank" class="hub-link"><span><i class="fa-solid fa-circle-user me-2 text-muted"></i>Profile</span><span class="hub-badge">/officer/profile</span></a></li>
                                </ul>
                            </div>
                        </div>

                        <!-- 6. Admin Management -->
                        <div class="col-md-6 col-lg-4">
                            <div class="hub-card">
                                <div class="hub-card-header bg-light">
                                    <span class="text-purple" style="color:#7c3aed;"><i class="fa-solid fa-user-tie me-2"></i>Admin Management</span>
                                    <span class="badge badge-pill-total">6 Pages</span>
                                </div>
                                <ul class="hub-list">
                                    <li class="hub-item"><a href="/admin/dashboard" target="_blank" class="hub-link"><span><i class="fa-solid fa-gauge-high me-2 text-muted"></i>Dashboard</span><span class="hub-badge">/admin/dashboard</span></a></li>
                                    <li class="hub-item"><a href="/admin/complaints" target="_blank" class="hub-link"><span><i class="fa-solid fa-clipboard-list me-2 text-muted"></i>All Complaints</span><span class="hub-badge">/admin/complaints</span></a></li>
                                    <li class="hub-item"><a href="/admin/departments" target="_blank" class="hub-link"><span><i class="fa-solid fa-building me-2 text-muted"></i>Departments</span><span class="hub-badge">/admin/departments</span></a></li>
                                    <li class="hub-item"><a href="/admin/escalations" target="_blank" class="hub-link"><span><i class="fa-solid fa-arrow-up-right-dots me-2 text-muted"></i>Escalations</span><span class="hub-badge">/admin/escalations</span></a></li>
                                    <li class="hub-item"><a href="/admin/users" target="_blank" class="hub-link"><span><i class="fa-solid fa-users me-2 text-muted"></i>Users</span><span class="hub-badge">/admin/users</span></a></li>
                                    <li class="hub-item"><a href="/admin/profile" target="_blank" class="hub-link"><span><i class="fa-solid fa-circle-user me-2 text-muted"></i>Profile</span><span class="hub-badge">/admin/profile</span></a></li>
                                </ul>
                            </div>
                        </div>
                    </div>
                </main>
            </body>
            </html>
            """;
    }

    // =========================================================================
    // 1. PUBLIC & LANDING PAGES
    // =========================================================================

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

    // Authentication routes are handled by AuthController

    // =========================================================================
    // 3. REPORTING FLOW
    // =========================================================================

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
                                    @RequestParam(required = false) String portal,
                                    @RequestParam(required = false) String dept) {
        model.addAttribute("complaintId", complaintId);
        model.addAttribute("title", title);
        model.addAttribute("description", description);
        model.addAttribute("sector", sector);
        model.addAttribute("location", location);
        model.addAttribute("urgency", urgency);
        model.addAttribute("portal", portal);
        model.addAttribute("dept", dept);
        return "report/submission-success";
    }

    // =========================================================================
    // 4. CITIZEN PORTAL
    // =========================================================================

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
                                          @RequestParam(required = false) String portal,
                                          @RequestParam(required = false) String dept) {
        model.addAttribute("activePage", "complaints");
        model.addAttribute("complaintId", id);
        model.addAttribute("title", title);
        model.addAttribute("description", description);
        model.addAttribute("sector", sector);
        model.addAttribute("location", location);
        model.addAttribute("urgency", urgency);
        model.addAttribute("portal", portal);
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
                                 @RequestParam(required = false) String successMsg,
                                 @RequestParam(required = false) String errorMsg) {
        model.addAttribute("activePage", "profile");
        populateAlerts(model, successMsg, errorMsg);
        return "citizen/profile";
    }

    // =========================================================================
    // 5. OFFICER WORKSPACE
    // =========================================================================

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
    public String officerComplaintDetails(Model model,
                                          @RequestParam(required = false) String id,
                                          @RequestParam(required = false) String title,
                                          @RequestParam(required = false) String description,
                                          @RequestParam(required = false) String sector,
                                          @RequestParam(required = false) String location,
                                          @RequestParam(required = false) String urgency,
                                          @RequestParam(required = false) String portal,
                                          @RequestParam(required = false) String dept) {
        model.addAttribute("activePage", "complaints");
        model.addAttribute("complaintId", id);
        model.addAttribute("title", title);
        model.addAttribute("description", description);
        model.addAttribute("sector", sector);
        model.addAttribute("location", location);
        model.addAttribute("urgency", urgency);
        model.addAttribute("portal", portal);
        model.addAttribute("dept", dept);
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
                                 @RequestParam(required = false) String successMsg,
                                 @RequestParam(required = false) String errorMsg) {
        model.addAttribute("activePage", "profile");
        populateAlerts(model, successMsg, errorMsg);
        return "officer/profile";
    }

    // =========================================================================
    // 6. ADMIN MANAGEMENT
    // =========================================================================

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
        populateAlerts(model, successMsg, errorMsg);
        return "admin/users";
    }

    @GetMapping("/admin/profile")
    public String adminProfile(Model model,
                               @RequestParam(required = false) String successMsg,
                               @RequestParam(required = false) String errorMsg) {
        model.addAttribute("activePage", "profile");
        populateAlerts(model, successMsg, errorMsg);
        return "admin/profile";
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================
    private void populateAlerts(Model model, String successMsg, String errorMsg) {
        if (successMsg != null && !successMsg.isBlank()) {
            model.addAttribute("successMsg", successMsg);
        }
        if (errorMsg != null && !errorMsg.isBlank()) {
            model.addAttribute("errorMsg", errorMsg);
        }
    }
}
