package com.turnout.guestservice.service;

import com.turnout.common.kafka.KafkaEvents;
import com.turnout.common.kafka.KafkaEvents.GuestImportedEvent;
import com.turnout.guestservice.config.EventServiceClient;
import com.turnout.guestservice.config.GuestTokenProperties;
import com.turnout.guestservice.config.JwtConfig;
import com.turnout.guestservice.dto.BulkImportResponse;
import com.turnout.guestservice.dto.FailedGuestRecord;
import com.turnout.guestservice.entity.Guest;
import com.turnout.guestservice.repository.GuestRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.SecretKey;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class BulkImportService {

    private final GuestRepository guestRepository;
    private final GuestTokenProperties tokenProperties;
    private final JwtConfig jwtConfig;
    private final EventServiceClient eventServiceClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    // ── Public API ────────────────────────────────────────────────────────────

    @Transactional
    public BulkImportResponse importGuests(UUID eventId, MultipartFile csvFile, UUID organizerId) {
        List<FailedGuestRecord> failedRows = new ArrayList<>();
        List<Guest> currentBatch = new ArrayList<>();
        Set<String> emailsSeen = new HashSet<>();

        int rowNumber = 0;
        int successCount = 0;

        LocalDateTime eventDate = eventServiceClient.getEventDate(eventId);

        try (
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(csvFile.getInputStream(), StandardCharsets.UTF_8)
            );
            CSVParser parser = CSVFormat.RFC4180.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .build()
                .parse(reader)
        ) {
            for (CSVRecord record : parser) {
                rowNumber++;

                String fullName = record.isMapped("full_name") ? record.get("full_name") : "";
                String email    = record.isMapped("email")     ? record.get("email")     : "";

                String validationError = validate(fullName, email);
                if (validationError != null) {
                    failedRows.add(new FailedGuestRecord(rowNumber, email, fullName, validationError));
                    continue;
                }

                String normalizedEmail = email.toLowerCase();

                if (emailsSeen.contains(normalizedEmail)) {
                    failedRows.add(new FailedGuestRecord(rowNumber, email, fullName, "Duplicate in file"));
                    continue;
                }

                if (guestRepository.existsByEventIdAndEmail(eventId, normalizedEmail)) {
                    failedRows.add(new FailedGuestRecord(rowNumber, email, fullName, "Already imported"));
                    continue;
                }

                emailsSeen.add(normalizedEmail);

                // Token is built with a placeholder — guest doesn't have a DB id yet.
                // We patch it in right after the batch flush, once Hibernate assigns real ids.
                Guest guest = new Guest(eventId, fullName.trim(), normalizedEmail, "PENDING-" + UUID.randomUUID());
                currentBatch.add(guest);
                successCount++;

                if (currentBatch.size() >= tokenProperties.getBatchSize()) {
                    flushBatchWithTokens(currentBatch, eventId, eventDate);
                    log.debug("Flushed batch at row {}", rowNumber);
                }
            }

            if (!currentBatch.isEmpty()) {
                flushBatchWithTokens(currentBatch, eventId, eventDate);
            }

        } catch (Exception e) {
            log.error("CSV import failed for event {} at row {}: {}", eventId, rowNumber, e.getMessage());
            throw new RuntimeException("CSV processing failed: " + e.getMessage(), e);
        }

        publishImportEvent(eventId, organizerId, successCount, failedRows.size());

        log.info("Import complete for event {}: {} succeeded, {} failed out of {} rows",
                eventId, successCount, failedRows.size(), rowNumber);

        return new BulkImportResponse(rowNumber, successCount, failedRows.size(), failedRows);
    }

    public byte[] generateSampleCsv() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(out)) {
            writer.println("full_name,email");
            writer.println("Alice Kamau,alice.kamau@example.com");
            writer.println("Bob Otieno,bob.otieno@example.com");
            writer.println("Carol Wanjiru,carol.wanjiru@example.com");
        }
        return out.toByteArray();
    }

    public byte[] exportGuestsCsv(UUID eventId) {
        List<Guest> guests = guestRepository.findByEventId(eventId);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(out)) {
            writer.println("id,full_name,email,rsvp_status,rsvp_date,token_used");
            for (Guest g : guests) {
                writer.printf("%s,%s,%s,%s,%s,%s%n",
                        g.getId(),
                        escapeCsv(g.getFullName()),
                        escapeCsv(g.getEmail()),
                        g.getRsvpStatus(),
                        g.getRsvpDate() != null ? g.getRsvpDate() : "",
                        g.isTokenUsed()
                );
            }
        }
        return out.toByteArray();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String validate(String fullName, String email) {
        if (fullName == null || fullName.isBlank()) {
            return "Missing full_name";
        }
        if (email == null || email.isBlank()) {
            return "Missing email";
        }
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            return "Invalid email format";
        }
        return null;
    }

    // Saves the batch first so Hibernate assigns real DB ids, then builds each
    // guest's real JWT token (which embeds that id) and updates the row.
    // Two round trips per batch instead of one, but this sidesteps every
    // Hibernate persist/merge ambiguity around manually-assigned UUIDs.
    private void flushBatchWithTokens(List<Guest> batch, UUID eventId, LocalDateTime eventDate) {
        guestRepository.saveAll(batch);
        guestRepository.flush();

        for (Guest guest : batch) {
            String token = generateGuestToken(
                    guest.getId(), eventId, guest.getEmail(), guest.getFullName(), eventDate
            );
            guest.setToken(token);
        }

        guestRepository.saveAll(batch);
        batch.clear();
    }

    private String generateGuestToken(UUID guestId, UUID eventId, String email, String fullName, LocalDateTime eventDate) {
        Date expiry = Date.from(
                eventDate.plusDays(tokenProperties.getTokenExpiryDays())
                         .atZone(ZoneId.systemDefault())
                         .toInstant()
        );

        return Jwts.builder()
                .subject(guestId.toString())
                .claim("eventId", eventId.toString())
                .claim("email", email)
                .claim("guestId", guestId.toString())
                .claim("guestName", fullName)
                .claim("type", "GUEST_RSVP")
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(
                jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8)
        );
    }

    private void publishImportEvent(UUID eventId, UUID organizerId, int successCount, int failureCount) {
        GuestImportedEvent event = new GuestImportedEvent(
                eventId,
                organizerId,
                successCount + failureCount,
                successCount,
                failureCount
        );
        kafkaTemplate.send(KafkaEvents.GUEST_IMPORTED, eventId.toString(), event);
        log.debug("Published GuestImportedEvent for event {}", eventId);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
