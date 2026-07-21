package com.turnout.emailservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

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
