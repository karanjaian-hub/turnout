package com.turnout.notificationservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${turnout.ws.allowed-origins}")
    private String allowedOrigins;

    /**
     * Configures the in-memory message broker.
     *
     * /topic  → pub/sub (one message → all subscribers). Used for broadcast updates.
     * /app    → prefix for messages sent FROM the client TO the server (we don't use
     *           this direction, but it's required by STOMP spec to have one).
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Registers the WebSocket handshake endpoint.
     *
     * - /ws          → the URL clients connect to
     * - withSockJS() → fallback for browsers that don't support native WebSocket
     *                  (long-polling, iframe, etc.). Keeps the same API on the client.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins)
                .withSockJS();
    }
}
