package com.turnout.emailservice.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class EmailProgressResponse {
    private UUID eventId;
    private long queued;
    private long sent;
    private long failed;
    private long total;
}
