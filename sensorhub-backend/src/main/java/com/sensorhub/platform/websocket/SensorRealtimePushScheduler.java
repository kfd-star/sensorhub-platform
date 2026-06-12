package com.sensorhub.platform.websocket;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class SensorRealtimePushScheduler {

    @Resource
    private SensorRealtimeWebSocketHandler sensorRealtimeWebSocketHandler;

    @Scheduled(fixedDelayString = "${sensor.realtime.push-interval-ms:3000}")
    public void pushLatestMessages() {
        sensorRealtimeWebSocketHandler.broadcastLatestMessages();
    }
}
