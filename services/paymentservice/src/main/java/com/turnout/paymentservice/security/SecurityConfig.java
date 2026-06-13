package com.turnout.paymentservice.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Webhook endpoints — called by Safaricom and Stripe servers directly,
                // no JWT involved. Signature verification is done inside the service layer.
                .requestMatchers(
                    "/api/payments/mpesa/callback",
                    "/api/payments/stripe/webhook"
                ).permitAll()
                // Actuator health — needed by Docker and the gateway
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // Everything else requires the gateway to have forwarded X-User-Id
                .anyRequest().authenticated()
            );

        return http.build();
    }
}
