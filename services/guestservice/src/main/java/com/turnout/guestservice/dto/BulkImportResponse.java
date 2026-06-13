package com.turnout.guestservice.dto;

import java.util.List;

public record BulkImportResponse(
        int totalRows,
        int successCount,
        int failureCount,
        List<FailedGuestRecord> failedRows
) {}
