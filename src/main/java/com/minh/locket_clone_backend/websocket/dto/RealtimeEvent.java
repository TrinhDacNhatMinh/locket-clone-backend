package com.minh.locket_clone_backend.websocket.dto;

public record RealtimeEvent(
        String type,
        Object payload
) {}
