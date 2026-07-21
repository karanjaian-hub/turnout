package com.turnout.notificationservice.dto;

import java.util.Map;

public record SystemHealthResponse(
        Map<String, String> serviceStatuses,
        String overallStatus
) {}
