package com.civicresolve.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;
    private final String fromEmail;
    private final String mailUsername;

    public EmailServiceImpl(
            @Autowired(required = false) JavaMailSender mailSender,
            @Value("${app.mail.from:noreply@civicresolve.gov}") String fromEmail,
            @Value("${spring.mail.username:}") String mailUsername) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
        this.mailUsername = mailUsername;
    }

    @Override
    public void sendRegistrationOtp(String email, String otp, String name) {
        String subject = "Your CivicResolve Verification Code: " + otp;
        String displayName = (name != null && !name.isBlank()) ? name : "Citizen";

        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f8fafc; color: #1e293b; margin: 0; padding: 20px; }
                    .card { max-width: 520px; margin: 0 auto; background: #ffffff; border: 1px solid #e2e8f0; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.05); }
                    .header { background: #0f172a; color: #ffffff; padding: 24px; text-align: center; border-bottom: 3px solid #2563eb; }
                    .content { padding: 30px; }
                    .otp-box { background: #eff6ff; border: 2px dashed #2563eb; border-radius: 8px; text-align: center; padding: 18px; margin: 24px 0; }
                    .otp-code { font-size: 32px; font-weight: 800; letter-spacing: 8px; color: #1d4ed8; font-family: monospace; }
                    .footer { background: #f8fafc; border-top: 1px solid #f1f5f9; padding: 16px; text-align: center; font-size: 12px; color: #64748b; }
                </style>
            </head>
            <body>
                <div class="card">
                    <div class="header">
                        <h2 style="margin:0; font-size: 22px;">CivicResolve</h2>
                        <p style="margin:4px 0 0; font-size: 13px; color: #94a3b8;">Citizen & Governance Resolution Platform</p>
                    </div>
                    <div class="content">
                        <h3 style="margin-top:0;">Verify Your Email Address</h3>
                        <p>Hello <b>%s</b>,</p>
                        <p>Thank you for creating an account with CivicResolve. To complete your registration, please enter the one-time verification code below:</p>
                        <div class="otp-box">
                            <div style="font-size: 12px; color: #475569; text-transform: uppercase; font-weight: 600; margin-bottom: 6px;">Your One-Time Password (OTP)</div>
                            <div class="otp-code">%s</div>
                            <div style="font-size: 12px; color: #64748b; margin-top: 6px;">Valid for 10 minutes</div>
                        </div>
                        <p style="font-size: 13px; color: #64748b;">If you did not initiate this registration request, please disregard this email.</p>
                    </div>
                    <div class="footer">
                        &copy; 2026 CivicResolve Grievance Redressal System. All rights reserved.
                    </div>
                </div>
            </body>
            </html>
            """.formatted(displayName, otp);

        sendEmailOrLog(email, subject, htmlContent, "REGISTRATION OTP", otp);
    }

    @Override
    public void sendPasswordResetOtp(String email, String otp, String resetLink) {
        String subject = "CivicResolve Password Reset Request";

        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f8fafc; color: #1e293b; margin: 0; padding: 20px; }
                    .card { max-width: 520px; margin: 0 auto; background: #ffffff; border: 1px solid #e2e8f0; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.05); }
                    .header { background: #0f172a; color: #ffffff; padding: 24px; text-align: center; border-bottom: 3px solid #2563eb; }
                    .content { padding: 30px; }
                    .otp-box { background: #fef3c7; border: 2px dashed #d97706; border-radius: 8px; text-align: center; padding: 18px; margin: 24px 0; }
                    .otp-code { font-size: 32px; font-weight: 800; letter-spacing: 8px; color: #b45309; font-family: monospace; }
                    .btn { display: inline-block; background: #2563eb; color: #ffffff; padding: 12px 24px; border-radius: 6px; text-decoration: none; font-weight: bold; margin: 15px 0; }
                    .footer { background: #f8fafc; border-top: 1px solid #f1f5f9; padding: 16px; text-align: center; font-size: 12px; color: #64748b; }
                </style>
            </head>
            <body>
                <div class="card">
                    <div class="header">
                        <h2 style="margin:0; font-size: 22px;">CivicResolve</h2>
                        <p style="margin:4px 0 0; font-size: 13px; color: #94a3b8;">Security &amp; Account Protection</p>
                    </div>
                    <div class="content">
                        <h3 style="margin-top:0;">Password Reset Verification</h3>
                        <p>We received a request to reset your password for your CivicResolve account associated with <b>%s</b>.</p>
                        <div class="otp-box">
                            <div style="font-size: 12px; color: #78350f; text-transform: uppercase; font-weight: 600; margin-bottom: 6px;">Your Password Reset Code</div>
                            <div class="otp-code">%s</div>
                            <div style="font-size: 12px; color: #92400e; margin-top: 6px;">Valid for 15 minutes</div>
                        </div>
                        <p style="text-align: center;">
                            <a href="%s" class="btn">Proceed to Reset Password &rarr;</a>
                        </p>
                        <p style="font-size: 12px; color: #64748b; word-break: break-all;">Or click directly: <a href="%s">%s</a></p>
                        <p style="font-size: 13px; color: #64748b;">If you did not request this, you can safely ignore this email. Your password will remain unchanged.</p>
                    </div>
                    <div class="footer">
                        &copy; 2026 CivicResolve Grievance Redressal System. All rights reserved.
                    </div>
                </div>
            </body>
            </html>
            """.formatted(email, otp, resetLink, resetLink, resetLink);

        sendEmailOrLog(email, subject, htmlContent, "PASSWORD RESET CODE", otp + " | Link: " + resetLink);
    }

    private void sendEmailOrLog(String toEmail, String subject, String htmlContent, String flowType, String details) {
        boolean sent = false;
        if (mailSender != null && mailUsername != null && !mailUsername.isBlank()) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(fromEmail);
                helper.setTo(toEmail);
                helper.setSubject(subject);
                helper.setText(htmlContent, true);
                mailSender.send(message);
                sent = true;
                log.info(">>> Sent {} email to: {}", flowType, toEmail);
            } catch (Exception ex) {
                log.warn(">>> Could not dispatch live SMTP email to {} (falling back to console log): {}", toEmail, ex.getMessage());
            }
        }

        // Always print to console / logs for instant local development & verification
        System.out.println("================================================================================");
        System.out.printf("  [CIVICRESOLVE EMAIL DISPATCH - %s]%n", flowType);
        System.out.printf("  TO:      %s%n", toEmail);
        System.out.printf("  SUBJECT: %s%n", subject);
        System.out.printf("  DETAILS: %s%n", details);
        System.out.printf("  STATUS:  %s%n", sent ? "DELIVERED VIA SMTP" : "DEV CONSOLE SIMULATION (ACTIVE)");
        System.out.println("================================================================================");
    }
}
