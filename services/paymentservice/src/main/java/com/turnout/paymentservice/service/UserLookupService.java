package com.turnout.paymentservice.service;

import com.turnout.paymentservice.dto.UserLookupResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.UUID;

@Slf4j
@Service
public class UserLookupService {

    private final WebClient authServiceWebClient;

    // Manual constructor required — @Qualifier is ignored by Lombok's @RequiredArgsConstructor
    public UserLookupService(@Qualifier("authServiceWebClient") WebClient authServiceWebClient) {
        this.authServiceWebClient = authServiceWebClient;
    }

    /**
     * Calls auth-service GET /api/auth/users/{id} to resolve a userId to
     * human-readable fields. Returns "unknown" fallback if auth-service is unreachable
     * so a network blip never breaks the admin transaction list.
     */
    public UserLookupResponse lookup(UUID userId) {
        try {
            return authServiceWebClient.get()
                    .uri("/api/auth/users/" + userId)
                    .retrieve()
                    .bodyToMono(UserLookupResponse.class)
                    .block();
        } catch (Exception e) {
            log.warn("Could not resolve user={} from auth-service: {}", userId, e.getMessage());
            return new UserLookupResponse(userId, "unknown", "unknown", "unknown");
        }
    }
}
