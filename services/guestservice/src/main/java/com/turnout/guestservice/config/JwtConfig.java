package com.turnout.guestservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// Separate from common-dto's JwtProperties — that one binds to "jwt",
// but guestservice stores the secret under "turnout.jwt" to match the monorepo convention
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "turnout.jwt")
public class JwtConfig {

    private String secret;
}
