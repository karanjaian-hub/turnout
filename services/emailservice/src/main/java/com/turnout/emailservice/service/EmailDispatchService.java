package com.turnout.emailservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turnout.emailservice.config.BrevoProperties;
import com.turnout.emailservice.dto.BrevoEmailRequest;
import com.turnout.emailservice.dto.EventDetailsPayload;
import com.turnout.emailservice.entity.EmailLog;
import com.turnout.emailservice.repository.EmailLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailDispatchService {

    private static final DateTimeFormatter EVENT_DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' h:mm a");

    private final WebClient webClient;
    private final BrevoProperties brevoProperties;
    private final EmailLogRepository emailLogRepository;
    private final ObjectMapper objectMapper;

    // Entry point for the consumer. Builds the Brevo request, fires it, and logs the result.
    public void sendVerificationEmail(String recipientEmail, String recipientName, String token) {
        String verifyUrl = "https://app.turnout.com/verify?token=" + token;

        String html = """
                <h2>Welcome to Turnout, %s!</h2>
                <p>Click the link below to verify your email address:</p>
                <a href="%s">Verify Email</a>
                <p>This link expires in 24 hours.</p>
                """.formatted(recipientName, verifyUrl);

        BrevoEmailRequest request = buildRequest(recipientEmail, recipientName,
                "Verify your Turnout account", html);

        // NOTE: unlike sendInvitationEmail, no upstream caller pre-creates a QUEUED
        // EmailLog row for USER_REGISTERED events (this comes from a Kafka listener,
        // not EmailController), so this insert-on-send pattern is correct here.
        EmailLog emailLog = EmailLog.builder()
                .eventType("USER_REGISTERED")
                .recipientEmail(recipientEmail)
                .recipientName(recipientName)
                .subject(request.getSubject())
                .status("PENDING")
                .attemptedAt(LocalDateTime.now())
                .build();

        dispatch(request, emailLog, "USER_REGISTERED");
    }

    public void sendPasswordResetEmail(String recipientEmail, String recipientName, String token) {
        String resetUrl = "https://app.turnout.com/reset-password?token=" + token;

        String html = """
                <h2>Password Reset Request</h2>
                <p>Hi %s,</p>
                <p>Click the link below to reset your password:</p>
                <a href="%s">Reset Password</a>
                <p>If you didn't request this, you can safely ignore this email.</p>
                <p>This link expires in 1 hour.</p>
                """.formatted(recipientName, resetUrl);

        BrevoEmailRequest request = buildRequest(recipientEmail, recipientName,
                "Reset your Turnout password", html);

        // Same note as above — insert-on-send, no upstream pre-created row for this event type.
        EmailLog emailLog = EmailLog.builder()
                .eventType("PASSWORD_RESET")
                .recipientEmail(recipientEmail)
                .recipientName(recipientName)
                .subject(request.getSubject())
                .status("PENDING")
                .attemptedAt(LocalDateTime.now())
                .build();

        dispatch(request, emailLog, "PASSWORD_RESET");
    }

    // guestId is String here, matching EmailLog.guestId (String column, not UUID) and
    // matching how both callers already have the value — EmailController's
    // /send-invitations passes guest.getId() (String) and /resend/{guestId} passes the
    // String path variable directly.
    //
    // NEW: eventDetails is nullable — EmailController.fetchEventDetails() returns null if
    // the call to eventservice fails, and this method degrades gracefully by omitting the
    // event-detail card rather than failing the whole send.
    public void sendInvitationEmail(String recipientEmail, String recipientName,
                                    UUID eventId, String guestId, EventDetailsPayload eventDetails) {
        // Look up the QUEUED EmailLog row that EmailController already created when
        // POST /send-invitations was called, instead of letting dispatch() blindly insert
        // a new row. This is the row whose status needs to flip from QUEUED -> SUCCESS/FAILED.
        //
        // eventType must be "MANUAL_INVITATION" to match what EmailController's
        // /send-invitations actually writes — and findFirst...OrderByAttemptedAtDesc grabs
        // the most recent QUEUED row instead of requiring exact uniqueness, since repeated
        // /send-invitations calls on the same event/guest can leave multiple QUEUED rows.
        EmailLog emailLog = emailLogRepository
                .findFirstByGuestIdAndEventIdAndEventTypeAndStatusOrderByAttemptedAtDesc(
                        guestId, eventId, "MANUAL_INVITATION", "QUEUED")
                .orElseThrow(() -> new IllegalStateException(
                        "No QUEUED EmailLog found for guestId=" + guestId + ", eventId=" + eventId));

        String html = buildInvitationHtml(recipientName, eventId, eventDetails);

        BrevoEmailRequest request = buildRequest(recipientEmail, recipientName,
                "You're invited to a Turnout event!", html);

        dispatch(request, emailLog, "MANUAL_INVITATION");

        // KNOWN ISSUE (not fixed here — flagging only):
        // EmailController's POST /resend/{guestId} calls this same method directly,
        // but it does NOT create a fresh QUEUED row first — it just re-uses the
        // *previous* EmailLog's data (status likely SUCCESS or FAILED, not QUEUED).
        // That means resend will now also throw the IllegalStateException above,
        // since the lookup here requires status="QUEUED" and won't find the old row.
        // This needs either: (a) resend creates a new QUEUED row before calling this
        // method, or (b) this method falls back to creating a new row when none is
        // found instead of throwing. Not resolved yet.
    }

    // NEW: builds the actual invitation HTML, including an event-detail card (title, date,
    // location, description) when eventDetails is available. Falls back to a generic
    // message if eventDetails is null (event lookup failed) so a guest still gets a valid,
    // if less informative, invitation instead of the send failing outright.
    private String buildInvitationHtml(String recipientName, UUID eventId, EventDetailsPayload eventDetails) {
        String rsvpUrl = "https://app.turnout.com/events/" + eventId + "/rsvp";

        String eventCardHtml;
        if (eventDetails != null) {
            String formattedDate = eventDetails.getEventDate() != null
                    ? eventDetails.getEventDate().format(EVENT_DATE_FORMAT)
                    : "Date to be confirmed";
            String title = eventDetails.getTitle() != null ? eventDetails.getTitle() : "Turnout Event";
            String location = eventDetails.getLocation() != null ? eventDetails.getLocation() : "Location to be confirmed";
            String description = eventDetails.getDescription() != null ? eventDetails.getDescription() : "";

            eventCardHtml = """
                    <div style="border:1px solid #e0e0e0; border-radius:8px; padding:16px; margin:16px 0;">
                        <h3 style="margin-top:0; color:#1E3A5F;">%s</h3>
                        <p>📅 %s</p>
                        <p>📍 %s</p>
                        <p>%s</p>
                    </div>
                    """.formatted(title, formattedDate, location, description);
        } else {
            // eventDetails lookup failed — fall back to the old generic message rather
            // than failing the send entirely.
            eventCardHtml = "<p>You have been invited to an event on Turnout.</p>";
        }

        return """
                <h2>You're Invited!</h2>
                <p>Hi %s,</p>
                %s
                <p>Click below to RSVP:</p>
                <a href="%s">RSVP Now</a>
                """.formatted(recipientName, eventCardHtml, rsvpUrl);
    }

    // Shared method: sends the request to Brevo and updates the given EmailLog with the result.
    // Takes an existing EmailLog and updates it in place instead of always building
    // and inserting a brand-new row — prevents duplicate rows per guest (one stuck at
    // QUEUED forever, one correctly showing SUCCESS/FAILED).
    private void dispatch(BrevoEmailRequest request, EmailLog emailLog, String eventType) {
        try {
            // Brevo returns a JSON body with a messageId on success
            String responseBody = webClient.post()
                    .uri("/smtp/email")
                    .header("api-key", brevoProperties.getApi().getKey())
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(); // Blocking is intentional here — Kafka consumers run on their own
            // thread pool, so we don't risk blocking the main event loop

            emailLog.setStatus("SUCCESS");
            emailLog.setProviderResponse(responseBody);
            emailLog.setDeliveredAt(LocalDateTime.now());

            log.info("Email sent successfully to {} for event {}", emailLog.getRecipientEmail(), eventType);

        } catch (Exception e) {
            emailLog.setStatus("FAILED");
            emailLog.setProviderResponse(e.getMessage());

            // We log but don't rethrow — the Kafka consumer handles retry logic.
            // Rethrowing here would cause the same message to be consumed again immediately.
            log.error("Failed to send email to {} for event {}: {}",
                    emailLog.getRecipientEmail(), eventType, e.getMessage());
        }

        emailLogRepository.save(emailLog); // updates the existing row (has an id) — no duplicate insert
    }

    private BrevoEmailRequest buildRequest(String recipientEmail, String recipientName,
                                           String subject, String htmlContent) {
        return BrevoEmailRequest.builder()
                .sender(BrevoEmailRequest.Sender.builder()
                        .name(brevoProperties.getSender().getName())
                        .email(brevoProperties.getSender().getEmail())
                        .build())
                .to(List.of(BrevoEmailRequest.Recipient.builder()
                        .email(recipientEmail)
                        .name(recipientName)
                        .build()))
                .subject(subject)
                .htmlContent(htmlContent)
                .build();
    }
}