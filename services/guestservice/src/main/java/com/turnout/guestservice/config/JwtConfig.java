package com.turnout.guestservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "turnout.jwt")
public class JwtConfig {

    private String secret;
}
