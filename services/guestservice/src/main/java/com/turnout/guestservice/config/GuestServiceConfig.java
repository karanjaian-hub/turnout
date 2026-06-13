package com.turnout.guestservice.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({GuestTokenProperties.class, JwtConfig.class})
public class GuestServiceConfig {}
