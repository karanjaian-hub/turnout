package com.turnout.emailservice.dto;

import lombok.Data;
import java.util.List;

// Outer ApiResponse envelope returned by guestservice after Patch 2
@Data
public class PagedGuestResponse {
    private int status;
    private String message;
    private PagedData data;

    @Data
    public static class PagedData {
        private List<GuestEmailPayload> content;
    }
}
