package org.example.templets.conf;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic"); // 구독 경로
        config.setApplicationDestinationPrefixes("/app"); // 전송 경로
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Nginx가 앞단에 있으므로 allowedOriginPatterns("*") 필수
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }
}