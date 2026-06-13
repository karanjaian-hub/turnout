package com.turnout.paymentservice.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({MpesaProperties.class, StripeProperties.class})
public class PaymentServiceConfig {
    // Activates @ConfigurationProperties binding for MpesaProperties and StripeProperties
}
