package com.turnout.notificationservice.dto;

import java.util.Map;

/**
 * Returned by GET /api/admin/system-health.
 * overallStatus is HEALTHY only when every service reports UP.
 */
public record SystemHealthResponse(
        Map<String, String> serviceStatuses,
        String overallStatus
) {}
