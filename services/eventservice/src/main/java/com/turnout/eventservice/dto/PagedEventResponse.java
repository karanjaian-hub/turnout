package com.turnout.eventservice.dto;

import java.util.List;

public record PagedEventResponse(
        List<EventResponse> content,
        long totalElements,
        int totalPages,
        int currentPage,
        int pageSize
) {}
