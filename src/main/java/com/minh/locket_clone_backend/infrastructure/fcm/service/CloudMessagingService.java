package com.minh.locket_clone_backend.infrastructure.fcm.service;

import java.util.Map;

public interface CloudMessagingService {
    
    /**
     * Sends a standard visible notification with a title and body.
     */
    void sendVisibleNotification(String fcmToken, String title, String body);

    /**
     * Sends a silent data-only push without a visible Notification object.
     */
    void sendSilentDataPush(String fcmToken, Map<String, String> data);
}
