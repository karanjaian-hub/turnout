package com.turnout.guestservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "turnout.guest")
public class GuestTokenProperties {

    // How many days until the guest's RSVP link expires — from application.yml
    private int tokenExpiryDays = 30;

    // How many guests to batch-insert in one DB round trip
    private int batchSize = 500;
}
