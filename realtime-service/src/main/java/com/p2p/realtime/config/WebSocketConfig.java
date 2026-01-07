package com.p2p.realtime.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.initialize();
        config.enableSimpleBroker("/topic", "/queue")
                .setTaskScheduler(taskScheduler)
                .setHeartbeatValue(new long[]{10000, 10000}); // Server sends heartbeat every 10s, expects client heartbeat every 10s
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user"); // For user-specific messages (e.g., /user/{userId}/queue/trips)
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("https://yourfrontend.com", "http://localhost:3000") // Replace with your frontend domains
                .withSockJS();
        registry.addEndpoint("/ws").setAllowedOrigins("https://yourfrontend.com", "http://localhost:3000"); // Non-SockJS endpoint
    }
}