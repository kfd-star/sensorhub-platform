package com.sensorhub.platform.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sensorhub.platform.model.vo.SensorRealtimeMessageVO;
import com.sensorhub.platform.service.SensorRealtimeStreamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class SensorRealtimeWebSocketHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions =
            Collections.newSetFromMap(new ConcurrentHashMap<WebSocketSession, Boolean>());

    private final Map<String, String> lastMessageSignatureMap = new ConcurrentHashMap<String, String>();

    @Resource
    private SensorRealtimeStreamService sensorRealtimeStreamService;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        sendSnapshot(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        sessions.remove(session);
        try {
            if (session.isOpen()) {
                session.close(CloseStatus.SERVER_ERROR);
            }
        } catch (IOException ignored) {
            // ignore close exception
        }
    }

    public boolean hasOpenSessions() {
        return !sessions.isEmpty();
    }

    public void broadcastLatestMessages() {
        if (sessions.isEmpty()) {
            return;
        }
        List<SensorRealtimeMessageVO> messages = sensorRealtimeStreamService.listLatestMessages();
        for (SensorRealtimeMessageVO message : messages) {
            String deviceKey = message.getDevice();
            String signature = buildSignature(message);
            String oldSignature = lastMessageSignatureMap.put(deviceKey, signature);
            if (!signature.equals(oldSignature)) {
                broadcastMessage(message);
            }
        }
    }

    private void sendSnapshot(WebSocketSession session) {
        List<SensorRealtimeMessageVO> messages = sensorRealtimeStreamService.listLatestMessages();
        for (SensorRealtimeMessageVO message : messages) {
            sendMessage(session, message);
        }
    }

    private void broadcastMessage(SensorRealtimeMessageVO message) {
        for (WebSocketSession session : sessions) {
            sendMessage(session, message);
        }
    }

    private void sendMessage(WebSocketSession session, SensorRealtimeMessageVO message) {
        if (session == null || !session.isOpen() || message == null) {
            return;
        }
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        } catch (Exception e) {
            log.warn("send realtime websocket message failed, sessionId={}, reason={}", session.getId(), e.getMessage());
            sessions.remove(session);
            try {
                if (session.isOpen()) {
                    session.close(CloseStatus.SERVER_ERROR);
                }
            } catch (IOException ignored) {
                // ignore close exception
            }
        }
    }

    private String buildSignature(SensorRealtimeMessageVO message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            return String.valueOf(message.getTimestamp());
        }
    }
}
