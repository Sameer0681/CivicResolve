package com.civicresolve.controller;

import com.civicresolve.entity.Complaint;
import com.civicresolve.entity.ComplaintStatus;
import com.civicresolve.entity.Escalation;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.StringTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CitizenDashboardTemplateTest {

    @Test
    void rendersDynamicClassesForPortalPages() throws IOException {
        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);

        Complaint complaint = Complaint.builder()
                .id(1L)
                .complaintId("CR-123")
                .title("Pothole")
                .sector("Roads & Transport")
                .status(ComplaintStatus.SUBMITTED)
                .build();
        Escalation escalation = Escalation.builder()
                .complaint(complaint)
                .status("PENDING")
                .build();
        Context context = new Context();
        context.setVariable("complaint", complaint);
        context.setVariable("escalation", escalation);
        context.setVariable("activePage", "dashboard");

        List<String> templates = List.of(
                "templates/citizen/dashboard.html",
                "templates/citizen/complaints.html",
                "templates/citizen/complaint-details.html",
                "templates/citizen/notifications.html",
                "templates/officer/dashboard.html",
                "templates/officer/complaints.html",
                "templates/officer/complaint-details.html",
                "templates/officer/notifications.html",
                "templates/officer/escalations.html",
                "templates/admin/dashboard.html",
                "templates/admin/complaints.html",
                "templates/admin/escalations.html",
                "templates/fragments/citizen-sidebar.html",
                "templates/fragments/officer-sidebar.html",
                "templates/fragments/admin-sidebar.html");
        Pattern pattern = Pattern.compile("th:classappend=\"([^\"]+)\"");
        int checkedExpressions = 0;

        for (String template : templates) {
            var input = getClass().getClassLoader().getResourceAsStream(template);
            assertNotNull(input, template);
            String source;
            try (input) {
                source = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }
            Matcher classAppend = pattern.matcher(source);
            while (classAppend.find()) {
                String rendered = templateEngine.process(
                        "<span th:classappend=\"" + classAppend.group(1) + "\"></span>", context);
                assertFalse(rendered.contains("th:classappend"), template);
                if (classAppend.group(1).contains("cr-status--")) {
                    assertTrue(rendered.contains("cr-status--submitted"), template);
                }
                checkedExpressions++;
            }
        }

        assertTrue(checkedExpressions >= 20);
    }
}
