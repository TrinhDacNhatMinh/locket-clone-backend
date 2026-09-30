package com.minh.locket_clone_backend.websocket;

import java.util.UUID;

import com.minh.locket_clone_backend.websocket.dto.RealtimeEventType;

public interface RealtimeEventPublisher {
    void publish(UUID recipientId, RealtimeEventType eventType, Object payload);
}
