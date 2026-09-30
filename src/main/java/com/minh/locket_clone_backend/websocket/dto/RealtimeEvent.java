package com.minh.locket_clone_backend.websocket.dto;

public record RealtimeEvent(
        RealtimeEventType type,
        Object payload
) {}
