package com.turnout.apigateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.turnout.common.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class JwtAuthenticationFilter
        extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Autowired
    public JwtAuthenticationFilter(ReactiveRedisTemplate<String, String> redisTemplate,
                                   JwtUtil jwtUtil,
                                   ObjectMapper objectMapper) {
        super(Config.class);
        this.redisTemplate = redisTemplate;
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest()
                    .getHeaders()
                    .getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED,
                        "Missing authorization token");
            }

            String token = authHeader.substring(7);

            if (!jwtUtil.validateToken(token)) {
                return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED,
                        "Invalid or expired token");
            }

            String jti = jwtUtil.extractJti(token);

            return redisTemplate.hasKey("blacklist:access:" + jti)
                    .flatMap(isBlacklisted -> {
                        if (Boolean.TRUE.equals(isBlacklisted)) {
                            return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED,
                                    "Invalid or expired token");
                        }

                        // extractUserId() returns UUID, extractRole() returns UserRole enum
                        // — both need toString()/name() to become header-safe strings
                        ServerHttpRequest mutatedRequest = exchange.getRequest()
                                .mutate()
                                .header("X-User-Id", jwtUtil.extractUserId(token).toString())
                                .header("X-User-Role", jwtUtil.extractRole(token).name())
                                .build();

                        return chain.filter(exchange.mutate()
                                .request(mutatedRequest)
                                .build());
                    });
        };
    }

    private Mono<Void> writeErrorResponse(ServerWebExchange exchange,
                                          HttpStatus status,
                                          String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", status.value(),
                "error", message,
                "path", exchange.getRequest().getPath().value()
        );

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            bytes = "{\"error\":\"Unauthorized\"}".getBytes();
        }

        return response.writeWith(
                Mono.just(response.bufferFactory().wrap(bytes))
        );
    }

    public static class Config {}
}
