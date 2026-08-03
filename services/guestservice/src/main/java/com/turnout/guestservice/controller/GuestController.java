package com.turnout.guestservice.controller;

import com.turnout.common.dto.ApiResponse;
import com.turnout.guestservice.dto.BulkImportResponse;
import com.turnout.guestservice.dto.EventGuestStatsResponse;
import com.turnout.guestservice.dto.GuestResponse;
import com.turnout.guestservice.dto.RsvpUpdateRequest;
import com.turnout.guestservice.dto.UpdateGuestRequest;
import com.turnout.guestservice.service.BulkImportService;
import com.turnout.guestservice.service.GuestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    // ── Import ────────────────────────────────────────────────────────────────

    @PostMapping(value = "/bulk-import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<BulkImportResponse>> bulkImport(
            @RequestParam UUID eventId,
            @RequestParam UUID organizerId,
            @RequestPart("file") MultipartFile file
    ) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Uploaded file is empty"));
        }

        log.info("Bulk import started — event: {}, organizer: {}, file: {} ({} bytes)",
                eventId, organizerId, file.getOriginalFilename(), file.getSize());

        BulkImportResponse result = bulkImportService.importGuests(eventId, file, organizerId);
        return ResponseEntity.status(201)
                .body(ApiResponse.success("Guests imported successfully.", result));
    }

    // File download — NOT wrapped in ApiResponse (raw bytes with Content-Disposition)
    @GetMapping("/sample-template")
    public ResponseEntity<byte[]> downloadSampleCsv() {
        byte[] csv = bulkImportService.generateSampleCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"guest-import-template.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    // File download — NOT wrapped in ApiResponse (raw bytes with Content-Disposition)
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

    @GetMapping("/event/{eventId}")
    public ResponseEntity<ApiResponse<Page<GuestResponse>>> listGuests(
            @PathVariable UUID eventId,
            @PageableDefault(size = 50, sort = "fullName") Pageable pageable
    ) {
        return ResponseEntity.ok(
                ApiResponse.success("Guests retrieved.", guestService.getGuestsByEvent(eventId, pageable))
        );
    }

    @GetMapping("/event/{eventId}/stats")
    public ResponseEntity<ApiResponse<EventGuestStatsResponse>> getStats(@PathVariable UUID eventId) {
        return ResponseEntity.ok(
                ApiResponse.success("Guest stats retrieved.", guestService.getEventStats(eventId))
        );
    }

    @GetMapping("/{guestId}")
    public ResponseEntity<ApiResponse<GuestResponse>> getGuest(@PathVariable UUID guestId) {
        return ResponseEntity.ok(
                ApiResponse.success("Guest retrieved.", guestService.getGuest(guestId))
        );
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    @PutMapping("/{guestId}")
    public ResponseEntity<ApiResponse<GuestResponse>> updateGuest(
            @PathVariable UUID guestId,
            @RequestBody @Valid UpdateGuestRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success("Guest updated.", guestService.updateGuest(guestId, request))
        );
    }

    @PatchMapping("/{guestId}/rsvp")
    public ResponseEntity<ApiResponse<GuestResponse>> updateRsvp(
            @PathVariable UUID guestId,
            @RequestBody @Valid RsvpUpdateRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success("RSVP status updated.", guestService.updateRsvp(guestId, request))
        );
    }

    @DeleteMapping("/{guestId}")
    public ResponseEntity<ApiResponse<Void>> deleteGuest(@PathVariable UUID guestId) {
        guestService.deleteGuest(guestId);
        return ResponseEntity.status(204)
                .body(ApiResponse.success("Guest removed."));
    }
}
