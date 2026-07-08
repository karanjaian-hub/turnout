package com.turnout.authservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

// Sends transactional emails via Brevo HTTPS API.
// Uses WebClient (non-blocking HTTP) instead of JavaMailSender
@Slf4j
@Service
@RequiredArgsConstructor
public class BrevoEmailService {

    private final WebClient.Builder webClientBuilder;

    @Value("${turnout.brevo.api-key}")
    private String apiKey;

    @Value("${turnout.brevo.api-url}")
    private String apiUrl;

    @Value("${turnout.brevo.sender-email}")
    private String senderEmail;

    @Value("${turnout.brevo.sender-name}")
    private String senderName;

    @Value("${turnout.frontend.url}")
    private String frontendUrl;


    public void sendOtpEmail(String toEmail, String toName, String otp) {
        String subject = "Your Turnout verification code";
        String html = buildOtpEmailHtml(toName, otp);
        sendEmail(toEmail, toName, subject, html);
    }

    public void sendPasswordResetEmail(String toEmail, String toName, String resetToken) {
        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;
        String subject = "Reset your Turnout password";
        String html = buildPasswordResetHtml(toName, resetLink);
        sendEmail(toEmail, toName, subject, html);
    }

    public void sendWelcomeEmail(String toEmail, String toName) {
        String subject = "Welcome to Turnout";
        String html = buildWelcomeEmailHtml(toName);
        sendEmail(toEmail, toName, subject, html);
    }

// Core send method
    private void sendEmail(String toEmail, String toName, String subject, String html) {
        try {
            // Brevo expects this exact JSON structure
            Map<String, Object> payload = Map.of(
                "sender", Map.of("name", senderName, "email", senderEmail),
                "to", new Object[]{Map.of("email", toEmail, "name", toName)},
                "subject", subject,
                "htmlContent", html
            );

            webClientBuilder.build()
                    .post()
                    .uri(apiUrl)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header("api-key", apiKey)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(); // safe — virtual threads make blocking cheap

        } catch (Exception e) {
// Log but never propagate — email failure must not fail registration
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
        }
    }

// HTML templates
    private String buildOtpEmailHtml(String name, String otp) {
        return """
            <div style="font-family:Inter,sans-serif;max-width:600px;margin:0 auto;">
              <div style="background:#1E3A5F;padding:32px;text-align:center;">
                <h1 style="color:#fff;margin:0;font-size:28px;letter-spacing:2px;">TURNOUT</h1>
              </div>
              <div style="background:#fff;padding:40px;">
                <p style="font-size:16px;color:#0F172A;">Hi <strong>%s</strong>,</p>
                <p style="font-size:16px;color:#64748B;">
                  Use the code below to verify your email address.
                  This code expires in <strong>10 minutes</strong>.
                </p>
                <div style="text-align:center;margin:32px 0;">
                  <span style="font-size:48px;font-weight:700;letter-spacing:12px;
                               color:#1E3A5F;background:#EFF6FF;padding:16px 32px;
                               border-radius:12px;">%s</span>
                </div>
                <p style="font-size:14px;color:#94A3B8;">
                  If you didn't create a Turnout account, ignore this email.
                </p>
              </div>
              <div style="background:#F8FAFC;padding:16px;text-align:center;">
                <p style="font-size:12px;color:#94A3B8;margin:0;">
                  Turnout — No guest left behind.
                </p>
              </div>
            </div>
            """.formatted(name, otp);
    }

    private String buildPasswordResetHtml(String name, String resetLink) {
        return """
            <div style="font-family:Inter,sans-serif;max-width:600px;margin:0 auto;">
              <div style="background:#1E3A5F;padding:32px;text-align:center;">
                <h1 style="color:#fff;margin:0;font-size:28px;letter-spacing:2px;">TURNOUT</h1>
              </div>
              <div style="background:#fff;padding:40px;">
                <p style="font-size:16px;color:#0F172A;">Hi <strong>%s</strong>,</p>
                <p style="font-size:16px;color:#64748B;">
                  We received a request to reset your password.
                  This link expires in <strong>15 minutes</strong>.
                </p>
                <div style="text-align:center;margin:32px 0;">
                  <a href="%s"
                     style="background:#2563EB;color:#fff;padding:14px 32px;
                            border-radius:8px;text-decoration:none;font-weight:600;
                            font-size:16px;">
                    Reset Password
                  </a>
                </div>
                <p style="font-size:14px;color:#94A3B8;">
                  If you didn't request a password reset, ignore this email.
                  Your password will not change.
                </p>
              </div>
              <div style="background:#F8FAFC;padding:16px;text-align:center;">
                <p style="font-size:12px;color:#94A3B8;margin:0;">
                  Turnout — No guest left behind.
                </p>
              </div>
            </div>
            """.formatted(name, resetLink);
    }

    private String buildWelcomeEmailHtml(String name) {
        return """
            <div style="font-family:Inter,sans-serif;max-width:600px;margin:0 auto;">
              <div style="background:#1E3A5F;padding:32px;text-align:center;">
                <h1 style="color:#fff;margin:0;font-size:28px;letter-spacing:2px;">TURNOUT</h1>
              </div>
              <div style="background:#fff;padding:40px;">
                <p style="font-size:16px;color:#0F172A;">Hi <strong>%s</strong>,</p>
                <p style="font-size:16px;color:#64748B;">
                  Welcome to Turnout. You're all set to start managing events.
                </p>
                <ul style="font-size:15px;color:#64748B;line-height:2;">
                  <li>Create your first event</li>
                  <li>Import your guest list via CSV</li>
                  <li>Send personalised invitations</li>
                  <li>Track RSVPs in real time</li>
                </ul>
                <p style="font-size:14px;color:#94A3B8;">
                  No guest left behind.
                </p>
              </div>
              <div style="background:#F8FAFC;padding:16px;text-align:center;">
                <p style="font-size:12px;color:#94A3B8;margin:0;">
                  Turnout — No guest left behind.
                </p>
              </div>
            </div>
            """.formatted(name);
    }
}
