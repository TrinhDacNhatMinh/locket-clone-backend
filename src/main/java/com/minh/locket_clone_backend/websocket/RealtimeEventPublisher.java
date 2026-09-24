package com.minh.locket_clone_backend.websocket;

import java.util.UUID;

public interface RealtimeEventPublisher {
    void publish(UUID userId, String eventType, Object payload);
}
