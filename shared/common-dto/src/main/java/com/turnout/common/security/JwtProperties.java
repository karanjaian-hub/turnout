package com.turnout.common.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secret;
    private long accessTokenExpiryMs  = 900_000;      // 15 minutes default
    private long refreshTokenExpiryMs = 604_800_000;   // 7 days default
}
