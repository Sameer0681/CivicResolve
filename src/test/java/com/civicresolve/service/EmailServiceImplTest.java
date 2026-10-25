package com.civicresolve.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EmailServiceImplTest {

    @Test
    void sendsPasswordResetOtpThroughConfiguredSmtp() throws Exception {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);
        EmailServiceImpl emailService = new EmailServiceImpl(
                mailSender, "noreply@civicresolve.gov", "smtp-user", "smtp-password");

        emailService.sendPasswordResetOtp(
                "citizen@example.com", "123456", "https://civicresolve.example/reset");

        verify(mailSender).send(message);
        assertEquals("CivicResolve Password Reset Request", message.getSubject());
        assertEquals("citizen@example.com", message.getAllRecipients()[0].toString());
    }

    @Test
    void rejectsEmailDeliveryWhenSmtpCredentialsAreMissing() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailServiceImpl emailService = new EmailServiceImpl(
                mailSender, "noreply@civicresolve.gov", "", "");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> emailService.sendPasswordResetOtp(
                        "citizen@example.com", "123456", "https://civicresolve.example/reset"));

        assertEquals(
                "Email delivery is not configured. Set SPRING_MAIL_USERNAME and SPRING_MAIL_PASSWORD.",
                exception.getMessage());
        verifyNoInteractions(mailSender);
    }

    @Test
    void surfacesSmtpDeliveryFailureInsteadOfReportingSuccess() throws Exception {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);
        doThrow(new MailSendException("SMTP rejected message")).when(mailSender).send(message);
        EmailServiceImpl emailService = new EmailServiceImpl(
                mailSender, "noreply@civicresolve.gov", "smtp-user", "smtp-password");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> emailService.sendPasswordResetOtp(
                        "citizen@example.com", "123456", "https://civicresolve.example/reset"));

        assertEquals("Could not send email. Please try again later.", exception.getMessage());
    }
}
