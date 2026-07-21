package com.turnout.emailservice.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EventDetailsPayload {
    private String id;
    private String title;
    private LocalDateTime eventDate;
    private String location;
    private String description;
}
