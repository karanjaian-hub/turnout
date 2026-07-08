package com.turnout.guestservice.controller;

import com.turnout.guestservice.dto.BulkImportResponse;
import com.turnout.guestservice.dto.EventGuestStatsResponse;
import com.turnout.guestservice.dto.GuestResponse;
import com.turnout.guestservice.service.BulkImportService;
import com.turnout.guestservice.service.GuestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.Valid;
import com.turnout.guestservice.dto.UpdateGuestRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/guests")
@RequiredArgsConstructor
public class GuestController {

    private final GuestService guestService;
    private final BulkImportService bulkImportService;

    // ── Import & export ───────────────────────────────────────────────────────

    /**
     * POST /api/guests/bulk-import?eventId=&organizerId=
     * Streams the CSV through Commons CSV — never held fully in memory.
     * Returns detailed results including every failed row with its reason.
     */
    @PostMapping(value = "/bulk-import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BulkImportResponse> bulkImport(
            @RequestParam UUID eventId,
            @RequestParam UUID organizerId,
            @RequestPart("file") MultipartFile file
    ) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        log.info("Bulk import started — event: {}, organizer: {}, file: {} ({} bytes)",
                eventId, organizerId, file.getOriginalFilename(), file.getSize());

        BulkImportResponse result = bulkImportService.importGuests(eventId, file, organizerId);

        // 207 Multi-Status — the request succeeded but individual rows may have failed
        // More honest than 200 when failureCount > 0
        return ResponseEntity.status(207).body(result);
    }

    /**
     * GET /api/guests/sample-csv
     * Returns a downloadable CSV template so organizers know the expected columns.
     */
    @GetMapping("/sample-template")
    public ResponseEntity<byte[]> downloadSampleCsv() {
        byte[] csv = bulkImportService.generateSampleCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"guest-import-template.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    /**
     * GET /api/guests/event/{eventId}/export
     * Exports all guests for an event as a CSV download.
     */
    @GetMapping("/event/{eventId}/export")
    public ResponseEntity<byte[]> exportGuests(@PathVariable UUID eventId) {
        byte[] csv = bulkImportService.exportGuestsCsv(eventId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"guests-" + eventId + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /**
     * GET /api/guests/event/{eventId}?page=0&size=50&sort=fullName,asc
     * Paginated — never returns the full list in one shot.
     */
    @GetMapping("/event/{eventId}")
    public ResponseEntity<Page<GuestResponse>> listGuests(
            @PathVariable UUID eventId,
            @PageableDefault(size = 50, sort = "fullName") Pageable pageable
    ) {
        return ResponseEntity.ok(guestService.getGuestsByEvent(eventId, pageable));
    }

    /**
     * GET /api/guests/event/{eventId}/stats
     * Returns RSVP counts per status — used by the dashboard and AI service.
     */
    @GetMapping("/event/{eventId}/stats")
    public ResponseEntity<EventGuestStatsResponse> getStats(@PathVariable UUID eventId) {
        return ResponseEntity.ok(guestService.getEventStats(eventId));
    }

    @GetMapping("/{guestId}")
    public ResponseEntity<GuestResponse> getGuest(@PathVariable UUID guestId) {
        return ResponseEntity.ok(guestService.getGuest(guestId));
    }

    @PutMapping("/{guestId}")
    public ResponseEntity<GuestResponse> updateGuest(
            @PathVariable UUID guestId,
            @RequestBody @Valid UpdateGuestRequest request
    ) {
        return ResponseEntity.ok(guestService.updateGuest(guestId, request));
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    /**
     * DELETE /api/guests/{guestId}
     * Only allowed if the guest hasn't submitted an RSVP yet.
     */
    @DeleteMapping("/{guestId}")
    public ResponseEntity<Void> deleteGuest(@PathVariable UUID guestId) {
        guestService.deleteGuest(guestId);
        return ResponseEntity.noContent().build();
    }
}
