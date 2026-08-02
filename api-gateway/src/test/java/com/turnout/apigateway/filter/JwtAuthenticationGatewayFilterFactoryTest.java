package com.turnout.apigateway.filter;

import com.turnout.common.enums.UserRole;
import com.turnout.common.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration test for JwtAuthenticationGatewayFilterFactory.
 *
 * Strategy: we build the filter manually, then invoke it directly against a
 * MockServerWebExchange. This is faster than booting the full gateway context
 * and avoids needing a live Redis or downstream service.
 *
 * We test the filter's apply() method — the GatewayFilter it returns —
 * by calling it with a mock exchange and a mock chain.
 */
class JwtAuthenticationGatewayFilterFactoryTest {

    // Real collaborators — we construct them directly, no Spring context needed
    private JwtUtil jwtUtil;
    private ReactiveRedisTemplate<String, String> redisTemplate;
    private ReactiveValueOperations<String, String> valueOps;
    private JwtAuthenticationGatewayFilterFactory filterFactory;
    private GatewayFilterChain chain;

    private static final String VALID_TOKEN   = "eyJ.valid.token";
    private static final String EXPIRED_TOKEN = "eyJ.expired.token";
    private static final UUID   USER_ID       = UUID.randomUUID();
    private static final String JTI           = UUID.randomUUID().toString();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        jwtUtil       = mock(JwtUtil.class);
        redisTemplate = mock(ReactiveRedisTemplate.class);
        valueOps      = mock(ReactiveValueOperations.class);
        chain         = mock(GatewayFilterChain.class);

        filterFactory = new JwtAuthenticationGatewayFilterFactory(
            redisTemplate, jwtUtil
        );

        // Chain just returns empty Mono — simulates downstream passing through
        when(chain.filter(any())).thenReturn(Mono.empty());
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    // ════════════════════════════════════════════════════════════════════════
    // MISSING / MALFORMED AUTHORIZATION HEADER
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void request_withNoAuthHeader_returns401() {
        var exchange = exchangeWithoutToken();

        invokeFilter(exchange);

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        // Chain must NOT be called — request is rejected before reaching downstream
        verify(chain, never()).filter(any());
    }

    @Test
    void request_withMalformedHeader_notBearerPrefix_returns401() {
        // "Token abc" instead of "Bearer abc"
        var exchange = exchangeWithHeader("Token " + VALID_TOKEN);

        invokeFilter(exchange);

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    // ════════════════════════════════════════════════════════════════════════
    // INVALID / EXPIRED TOKEN
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void request_withExpiredToken_returns401() {
        when(jwtUtil.validateToken(EXPIRED_TOKEN)).thenReturn(false);
        var exchange = exchangeWithBearer(EXPIRED_TOKEN);

        invokeFilter(exchange);

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        // Redis must NOT be checked if the token itself is invalid
        verify(redisTemplate, never()).opsForValue();
        verify(chain, never()).filter(any());
    }

    @Test
    void request_withTamperedToken_returns401() {
        when(jwtUtil.validateToken("bad.token")).thenReturn(false);
        var exchange = exchangeWithBearer("bad.token");

        invokeFilter(exchange);

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    // ════════════════════════════════════════════════════════════════════════
    // BLACKLISTED TOKEN (logged-out user replaying an old access token)
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void request_withBlacklistedToken_returns401() {
        when(jwtUtil.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtUtil.extractJti(VALID_TOKEN)).thenReturn(JTI);
        // Redis says this JTI was blacklisted on logout
        when(redisTemplate.hasKey("blacklist:access:" + JTI)).thenReturn(Mono.just(true));

        var exchange = exchangeWithBearer(VALID_TOKEN);

        invokeFilter(exchange);

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    // ════════════════════════════════════════════════════════════════════════
    // VALID TOKEN — HAPPY PATH
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void request_withValidToken_passesThrough_withUserHeaders() {
        when(jwtUtil.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtUtil.extractJti(VALID_TOKEN)).thenReturn(JTI);
        when(jwtUtil.extractUserId(VALID_TOKEN)).thenReturn(USER_ID);
        when(jwtUtil.extractRole(VALID_TOKEN)).thenReturn(UserRole.EVENT_ORGANIZER);
        // Token is NOT blacklisted
        when(redisTemplate.hasKey("blacklist:access:" + JTI)).thenReturn(Mono.just(false));

        var exchange = exchangeWithBearer(VALID_TOKEN);

        invokeFilter(exchange);

        // Response must NOT be set — filter passes through to chain
        assertThat(exchange.getResponse().getStatusCode()).isNull();
        verify(chain).filter(argThat(mutatedExchange -> {
            // The mutated request must carry both identity headers
            var headers = mutatedExchange.getRequest().getHeaders();
            return USER_ID.toString().equals(headers.getFirst("X-User-Id")) &&
                   "EVENT_ORGANIZER".equals(headers.getFirst("X-User-Role"));
        }));
    }

    @Test
    void request_withValidToken_superAdmin_passesWithCorrectRoleHeader() {
        when(jwtUtil.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtUtil.extractJti(VALID_TOKEN)).thenReturn(JTI);
        when(jwtUtil.extractUserId(VALID_TOKEN)).thenReturn(USER_ID);
        when(jwtUtil.extractRole(VALID_TOKEN)).thenReturn(UserRole.SUPER_ADMIN);
        when(redisTemplate.hasKey("blacklist:access:" + JTI)).thenReturn(Mono.just(false));

        var exchange = exchangeWithBearer(VALID_TOKEN);

        invokeFilter(exchange);

        verify(chain).filter(argThat(mutatedExchange ->
            "SUPER_ADMIN".equals(mutatedExchange.getRequest().getHeaders().getFirst("X-User-Role"))
        ));
    }

    @Test
    void request_withValidToken_redisKeyAbsent_passesThrough() {
        // Redis returns false (not null) when a key doesn't exist — same as not blacklisted
        when(jwtUtil.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtUtil.extractJti(VALID_TOKEN)).thenReturn(JTI);
        when(jwtUtil.extractUserId(VALID_TOKEN)).thenReturn(USER_ID);
        when(jwtUtil.extractRole(VALID_TOKEN)).thenReturn(UserRole.EVENT_ORGANIZER);
        when(redisTemplate.hasKey("blacklist:access:" + JTI)).thenReturn(Mono.just(false));

        var exchange = exchangeWithBearer(VALID_TOKEN);

        invokeFilter(exchange);

        verify(chain).filter(any());
    }

    // ════════════════════════════════════════════════════════════════════════
    // HEADER FORMAT EDGE CASES
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void request_withEmptyBearerToken_returns401() {
        // "Bearer " with nothing after it — token is empty string
        when(jwtUtil.validateToken("")).thenReturn(false);
        var exchange = exchangeWithHeader("Bearer ");

        invokeFilter(exchange);

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    // ════════════════════════════════════════════════════════════════════════
    // Private helpers
    // ════════════════════════════════════════════════════════════════════════

    private MockServerWebExchange exchangeWithoutToken() {
        return MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/events/123").build()
        );
    }

    private MockServerWebExchange exchangeWithBearer(String token) {
        return exchangeWithHeader("Bearer " + token);
    }

    private MockServerWebExchange exchangeWithHeader(String headerValue) {
        return MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/events/123")
                .header(HttpHeaders.AUTHORIZATION, headerValue)
                .build()
        );
    }

    /**
     * Invokes the filter synchronously by blocking on the reactive Mono.
     * Safe in tests — we're not on a Reactor scheduler thread.
     */
    private void invokeFilter(MockServerWebExchange exchange) {
        var filter = filterFactory.apply(new JwtAuthenticationGatewayFilterFactory.Config());
        filter.filter(exchange, chain).block();
    }
}
