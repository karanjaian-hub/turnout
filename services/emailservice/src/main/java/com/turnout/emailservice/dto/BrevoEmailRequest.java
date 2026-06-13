package com.turnout.emailservice.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

// The JSON body shape that Brevo's /v3/smtp/email endpoint expects.
// We build this inside EmailDispatchService and send it via WebClient.
@Data
@Builder
public class BrevoEmailRequest {

    private Sender sender;
    private List<Recipient> to;
    private String subject;
    private String htmlContent;

    // Optional: Brevo template ID — use either htmlContent OR templateId, not both
    private Integer templateId;
    private Map<String, Object> params;

    @Data
    @Builder
    public static class Sender {
        private String name;
        private String email;
    }

    @Data
    @Builder
    public static class Recipient {
        private String email;
        private String name;
    }
}
