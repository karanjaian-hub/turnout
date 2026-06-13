package com.turnout.emailservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// Binds every key under "brevo:" in application.yml to this class.
// This way no class ever reads @Value("${brevo.api.key}") directly —
// they just inject BrevoProperties and call getApi().getKey().
@Component
@ConfigurationProperties(prefix = "brevo")
@Data
public class BrevoProperties {

    private Api api = new Api();
    private Sender sender = new Sender();

    @Data
    public static class Api {
        private String key;
        private String baseUrl;
    }

    @Data
    public static class Sender {
        private String email;
        private String name;
    }
}
