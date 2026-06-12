package com.sensorhub.platform.config;

import com.sensorhub.platform.websocket.SensorRealtimeWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import javax.annotation.Resource;

@Configuration
@EnableWebSocket
public class SensorRealtimeWebSocketConfig implements WebSocketConfigurer {

    @Resource
    private SensorRealtimeWebSocketHandler sensorRealtimeWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(sensorRealtimeWebSocketHandler, "/ws/sensor/realtime")
                .setAllowedOriginPatterns("*");
    }
}
