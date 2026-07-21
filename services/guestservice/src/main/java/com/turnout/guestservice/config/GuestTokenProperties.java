package com.turnout.guestservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "turnout.guest")
public class GuestTokenProperties {

    private int tokenExpiryDays = 30;

    // How many guests to batch-insert in one DB round trip
    private int batchSize = 500;
}
