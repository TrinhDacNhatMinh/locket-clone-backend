package com.minh.locket_clone_backend.notification.dto;

import com.minh.locket_clone_backend.notification.entity.Notification;
import com.minh.locket_clone_backend.notification.entity.NotificationType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        Map<String, Object> payload,
        boolean isRead,
        Instant createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getPayload(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
