package com.turnout.emailservice.dto;

import lombok.Data;

import java.util.List;

@Data
public class PagedGuestResponse {
    private List<GuestEmailPayload> content;
}
