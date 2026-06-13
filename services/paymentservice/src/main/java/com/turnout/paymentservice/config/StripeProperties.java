package com.turnout.paymentservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "turnout.stripe")
public class StripeProperties {
    private String secretKey;
    private String webhookSecret;
}
