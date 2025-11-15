package com.enterprise.scheduler.api.websocket;

import com.enterprise.scheduler.common.util.JsonUtil;
import com.enterprise.scheduler.core.event.JobEvent;
import com.enterprise.scheduler.core.spi.EventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket handler for real-time job event updates
 * Feature #44: WebSocket Support for Real-time Updates
 */
@Slf4j
@Component
public class JobEventWebSocketHandler extends TextWebSocketHandler {

    private final EventPublisher eventPublisher;
    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private final Map<String, Set<String>> sessionSubscriptions = new ConcurrentHashMap<>();

    public JobEventWebSocketHandler(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @PostConstruct
    public void init() {
        // Subscribe to job events and broadcast to connected clients
        eventPublisher.subscribe(event -> {
            if (event instanceof JobEvent) {
                broadcastEvent((JobEvent) event);
            }
        });
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket connection established: {}", session.getId());
        sessions.add(session);
        sessionSubscriptions.put(session.getId(), new HashSet<>());

        // Send welcome message
        Map<String, Object> welcome = new HashMap<>();
        welcome.put("type", "CONNECTED");
        welcome.put("sessionId", session.getId());
        welcome.put("timestamp", System.currentTimeMillis());

        session.sendMessage(new TextMessage(JsonUtil.toJson(welcome)));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.debug("Received WebSocket message: {}", message.getPayload());

        try {
            Map<String, Object> payload = JsonUtil.fromJson(message.getPayload(), Map.class);
            String action = (String) payload.get("action");

            if ("subscribe".equals(action)) {
                handleSubscribe(session, payload);
            } else if ("unsubscribe".equals(action)) {
                handleUnsubscribe(session, payload);
            } else if ("ping".equals(action)) {
                handlePing(session);
            }
        } catch (Exception e) {
            log.error("Error handling WebSocket message", e);
            sendError(session, "Invalid message format");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("WebSocket connection closed: {} with status: {}", session.getId(), status);
        sessions.remove(session);
        sessionSubscriptions.remove(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error for session: {}", session.getId(), exception);
        sessions.remove(session);
        sessionSubscriptions.remove(session.getId());
    }

    private void handleSubscribe(WebSocketSession session, Map<String, Object> payload) throws IOException {
        String jobId = (String) payload.get("jobId");
        String eventType = (String) payload.get("eventType");

        Set<String> subscriptions = sessionSubscriptions.get(session.getId());
        if (jobId != null) {
            subscriptions.add("job:" + jobId);
            log.info("Session {} subscribed to job: {}", session.getId(), jobId);
        }
        if (eventType != null) {
            subscriptions.add("event:" + eventType);
            log.info("Session {} subscribed to event type: {}", session.getId(), eventType);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("type", "SUBSCRIBED");
        response.put("jobId", jobId);
        response.put("eventType", eventType);

        session.sendMessage(new TextMessage(JsonUtil.toJson(response)));
    }

    private void handleUnsubscribe(WebSocketSession session, Map<String, Object> payload) throws IOException {
        String jobId = (String) payload.get("jobId");
        String eventType = (String) payload.get("eventType");

        Set<String> subscriptions = sessionSubscriptions.get(session.getId());
        if (jobId != null) {
            subscriptions.remove("job:" + jobId);
            log.info("Session {} unsubscribed from job: {}", session.getId(), jobId);
        }
        if (eventType != null) {
            subscriptions.remove("event:" + eventType);
            log.info("Session {} unsubscribed from event type: {}", session.getId(), eventType);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("type", "UNSUBSCRIBED");
        response.put("jobId", jobId);
        response.put("eventType", eventType);

        session.sendMessage(new TextMessage(JsonUtil.toJson(response)));
    }

    private void handlePing(WebSocketSession session) throws IOException {
        Map<String, Object> pong = new HashMap<>();
        pong.put("type", "PONG");
        pong.put("timestamp", System.currentTimeMillis());

        session.sendMessage(new TextMessage(JsonUtil.toJson(pong)));
    }

    private void broadcastEvent(JobEvent event) {
        String eventJson;
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("type", "JOB_EVENT");
            eventData.put("eventType", event.getType());
            eventData.put("jobId", event.getJobId());
            eventData.put("executionId", event.getExecutionId());
            eventData.put("tenantId", event.getTenantId());
            eventData.put("timestamp", event.getTimestamp());
            eventData.put("payload", event.getPayload());

            eventJson = JsonUtil.toJson(eventData);
        } catch (Exception e) {
            log.error("Error serializing job event", e);
            return;
        }

        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                sessions.remove(session);
                continue;
            }

            try {
                // Check if session is subscribed to this event
                Set<String> subscriptions = sessionSubscriptions.get(session.getId());
                if (subscriptions == null || subscriptions.isEmpty()) {
                    // No filters, send all events
                    session.sendMessage(new TextMessage(eventJson));
                } else {
                    // Check if event matches any subscription
                    boolean shouldSend = false;

                    if (event.getJobId() != null && subscriptions.contains("job:" + event.getJobId())) {
                        shouldSend = true;
                    }
                    if (event.getType() != null && subscriptions.contains("event:" + event.getType())) {
                        shouldSend = true;
                    }

                    if (shouldSend) {
                        session.sendMessage(new TextMessage(eventJson));
                    }
                }
            } catch (IOException e) {
                log.error("Error sending event to WebSocket session: {}", session.getId(), e);
                sessions.remove(session);
                sessionSubscriptions.remove(session.getId());
            }
        }
    }

    private void sendError(WebSocketSession session, String errorMessage) {
        try {
            Map<String, Object> error = new HashMap<>();
            error.put("type", "ERROR");
            error.put("message", errorMessage);
            error.put("timestamp", System.currentTimeMillis());

            session.sendMessage(new TextMessage(JsonUtil.toJson(error)));
        } catch (IOException e) {
            log.error("Error sending error message to WebSocket session", e);
        }
    }

    public int getActiveConnectionCount() {
        return sessions.size();
    }

    public Set<String> getSessionSubscriptions(String sessionId) {
        return sessionSubscriptions.getOrDefault(sessionId, Collections.emptySet());
    }
}
