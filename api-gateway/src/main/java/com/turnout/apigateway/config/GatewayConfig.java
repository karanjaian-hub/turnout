package com.turnout.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Configuration
public class GatewayConfig {

    // IpKeyResolver: extracts the caller's IP address for rate limiting.
    // X-Forwarded-For is checked first because in Docker/behind a proxy,
    // remoteAddress is the proxy IP, not the real client IP.
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String forwardedFor = exchange.getRequest()
                    .getHeaders()
                    .getFirst("X-Forwarded-For");

            if (forwardedFor != null && !forwardedFor.isBlank()) {
                // X-Forwarded-For can be a comma-separated chain — take the first (original client)
                return Mono.just(forwardedFor.split(",")[0].trim());
            }

            return Mono.justOrEmpty(exchange.getRequest().getRemoteAddress())
                    .map(InetSocketAddress::getHostString)
                    .defaultIfEmpty("unknown");
        };
    }

    // Reactive RedisTemplate for String keys and values.
    // The JWT blacklist check in JwtAuthenticationFilter uses this.
    // Must be reactive (not the blocking RedisTemplate) because WebFlux
    // cannot block a Reactor thread — it would deadlock under load.
    @Bean
    public ReactiveRedisTemplate<String, String> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory factory) {
        StringRedisSerializer serializer = new StringRedisSerializer();
        RedisSerializationContext<String, String> context =
                RedisSerializationContext.<String, String>newSerializationContext(serializer)
                        .value(serializer)
                        .build();
        return new ReactiveRedisTemplate<>(factory, context);
    }
}
