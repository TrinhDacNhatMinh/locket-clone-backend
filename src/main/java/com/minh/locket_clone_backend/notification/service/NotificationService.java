package com.minh.locket_clone_backend.notification.service;

import com.minh.locket_clone_backend.common.dto.CursorPagedResponse;
import com.minh.locket_clone_backend.notification.dto.NotificationResponse;
import com.minh.locket_clone_backend.notification.entity.NotificationType;

import java.util.Map;
import java.util.UUID;

import com.minh.locket_clone_backend.websocket.dto.RealtimeEventType;

public interface NotificationService {

    void notify(UUID recipientId, NotificationType notificationType, RealtimeEventType realtimeEventType, Map<String, Object> payload, Object realtimePayload);

    CursorPagedResponse<NotificationResponse> getNotifications(UUID userId, String cursor, int limit);

    void markAsRead(UUID userId, UUID notificationId);

    void markAllAsRead(UUID userId);
}
