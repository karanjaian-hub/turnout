package com.turnout.paymentservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds turnout.mpesa.* from application.yml into a typed bean.
 * WHY a separate class: keeps MpesaService clean — no @Value scattered everywhere.
 */
@Data
@ConfigurationProperties(prefix = "turnout.mpesa")
public class MpesaProperties {
    private String consumerKey;
    private String consumerSecret;
    private String shortcode;
    private String passkey;
    private String callbackUrl;
    private String env;
    private String apiBaseUrl;
}
