package com.civicresolve.controller;

import com.civicresolve.entity.Complaint;
import com.civicresolve.entity.ComplaintStatus;
import com.civicresolve.entity.Escalation;
import com.civicresolve.repository.ComplaintRepository;
import com.civicresolve.repository.EscalationRepository;
import com.civicresolve.service.ComplaintService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;
import java.util.List;

@Controller
public class MainController {

    private final ComplaintService complaintService;
    private final ComplaintRepository complaintRepository;
    private final EscalationRepository escalationRepository;

    public MainController(
            ComplaintService complaintService,
            ComplaintRepository complaintRepository,
            EscalationRepository escalationRepository) {
        this.complaintService = complaintService;
        this.complaintRepository = complaintRepository;
        this.escalationRepository = escalationRepository;
    }

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
                                <p class="mb-0 text-white-50 small">Quickly render and test all frontend views with live backend integration.</p>
                            </div>
                        </div>
                    </div>
                </header>
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
        model.addAttribute("complaints", complaintRepository.findAll());
        return "citizen/dashboard";
    }

    @GetMapping("/citizen/complaints")
    public String citizenComplaints(Model model,
                                    @RequestParam(required = false) String successMsg,
                                    @RequestParam(required = false) String errorMsg) {
        model.addAttribute("activePage", "complaints");
        populateAlerts(model, successMsg, errorMsg);
        model.addAttribute("complaints", complaintRepository.findAll());
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

        Complaint complaint = findComplaintByParam(id);

        if (complaint != null) {
            model.addAttribute("complaint", complaint);
            model.addAttribute("complaintId", complaint.getComplaintId());
            model.addAttribute("title", complaint.getTitle());
            model.addAttribute("description", complaint.getDescription());
            model.addAttribute("sector", complaint.getSector());
            model.addAttribute("location", complaint.getLocation());
            model.addAttribute("urgency", complaint.getUrgency());
            model.addAttribute("dept", complaint.getDepartment());

            boolean isOverdue = complaint.getSlaDeadline() != null &&
                    LocalDateTime.now().isAfter(complaint.getSlaDeadline()) &&
                    complaint.getStatus() != ComplaintStatus.RESOLVED;
            model.addAttribute("isOverdue", isOverdue);

            List<Escalation> escalations = escalationRepository.findByComplaintOrderByEscalationLevelAscIdAsc(complaint);
            model.addAttribute("escalations", escalations);
        } else {
            model.addAttribute("complaintId", id);
            model.addAttribute("title", title);
            model.addAttribute("description", description);
            model.addAttribute("sector", sector);
            model.addAttribute("location", location);
            model.addAttribute("urgency", urgency);
            model.addAttribute("portal", portal);
            model.addAttribute("dept", dept);
        }
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
        model.addAttribute("complaints", complaintRepository.findAll());
        return "officer/dashboard";
    }

    @GetMapping("/officer/complaints")
    public String officerComplaints(Model model,
                                    @RequestParam(required = false) String successMsg,
                                    @RequestParam(required = false) String errorMsg) {
        model.addAttribute("activePage", "complaints");
        populateAlerts(model, successMsg, errorMsg);
        model.addAttribute("complaints", complaintRepository.findAll());
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
        return citizenComplaintDetails(model, id, title, description, sector, location, urgency, portal, dept);
    }

    @GetMapping("/officer/escalations")
    public String officerEscalations(Model model) {
        model.addAttribute("activePage", "escalations");
        List<Escalation> escalations = escalationRepository.findAll();
        model.addAttribute("escalations", escalations);
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
        model.addAttribute("complaints", complaintRepository.findAll());
        return "admin/dashboard";
    }

    @GetMapping("/admin/complaints")
    public String adminComplaints(Model model,
                                  @RequestParam(required = false) String successMsg,
                                  @RequestParam(required = false) String errorMsg) {
        model.addAttribute("activePage", "complaints");
        populateAlerts(model, successMsg, errorMsg);
        model.addAttribute("complaints", complaintRepository.findAll());
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
        List<Escalation> escalations = escalationRepository.findAll();
        model.addAttribute("escalations", escalations);

        long criticalCount = escalations.stream()
                .filter(e -> "CRITICAL".equalsIgnoreCase(e.getPriorityLevel()) || Integer.valueOf(3).equals(e.getEscalationLevel()))
                .count();
        model.addAttribute("criticalCount", criticalCount);
        model.addAttribute("totalEscalations", escalations.size());

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
    private Complaint findComplaintByParam(String param) {
        return complaintService.findComplaintByParam(param);
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
