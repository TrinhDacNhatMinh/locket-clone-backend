package com.minh.locket_clone_backend.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SessionRegistry {

    // Maps userId to a set of active websocket session IDs (to support multiple devices)
    private final ConcurrentHashMap<UUID, Set<String>> activeSessions = new ConcurrentHashMap<>();

    public void markOnline(UUID userId, String sessionId) {
        activeSessions.compute(userId, (key, sessionIds) -> {
            if (sessionIds == null) {
                sessionIds = ConcurrentHashMap.newKeySet();
            }
            sessionIds.add(sessionId);
            return sessionIds;
        });
        log.debug("User {} marked online. Active sessions: {}", userId, activeSessions.get(userId).size());
    }

    public void markOffline(UUID userId, String sessionId) {
        activeSessions.computeIfPresent(userId, (key, sessionIds) -> {
            sessionIds.remove(sessionId);
            if (sessionIds.isEmpty()) {
                log.debug("User {} marked offline (no active sessions).", userId);
                return null; // Remove the key from the map
            }
            log.debug("User {} closed a session. Remaining active sessions: {}", userId, sessionIds.size());
            return sessionIds;
        });
    }

    public boolean isOnline(UUID userId) {
        return activeSessions.containsKey(userId);
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal userPrincipal = headerAccessor.getUser();
        String sessionId = headerAccessor.getSessionId();

        if (userPrincipal != null && sessionId != null) {
            try {
                UUID userId = UUID.fromString(userPrincipal.getName());
                markOnline(userId, sessionId);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid userId format in Principal name upon connect: {}", userPrincipal.getName());
            }
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal userPrincipal = headerAccessor.getUser();
        String sessionId = headerAccessor.getSessionId();

        if (userPrincipal != null && sessionId != null) {
            try {
                UUID userId = UUID.fromString(userPrincipal.getName());
                markOffline(userId, sessionId);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid userId format in Principal name upon disconnect: {}", userPrincipal.getName());
            }
        }
    }
}
