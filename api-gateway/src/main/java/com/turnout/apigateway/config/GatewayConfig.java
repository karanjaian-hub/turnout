package com.turnout.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
import com.turnout.common.security.JwtProperties;
import com.turnout.common.security.JwtUtil;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Configuration
public class GatewayConfig {
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

// helps in getting the ip address stamped onto the packet from the sender
            return Mono.justOrEmpty(exchange.getRequest().getRemoteAddress())
                    .map(InetSocketAddress::getHostString)
                    .defaultIfEmpty("unknown");
        };
    }

    @Primary
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

    @Bean
    public JwtProperties jwtProperties(@Value("${turnout.jwt.secret}") String secret) {
        JwtProperties props = new JwtProperties();
        props.setSecret(secret);
        return props;
    }

    @Bean
    public JwtUtil jwtUtil(JwtProperties jwtProperties) {
        return new JwtUtil(jwtProperties);
    }
}
