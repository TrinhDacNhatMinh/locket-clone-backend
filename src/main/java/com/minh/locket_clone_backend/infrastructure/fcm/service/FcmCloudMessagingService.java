package com.minh.locket_clone_backend.infrastructure.fcm.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmCloudMessagingService implements CloudMessagingService {

    private final FirebaseMessaging firebaseMessaging;

    @Override
    public void sendVisibleNotification(String fcmToken, String title, String body) {
        if (fcmToken == null || fcmToken.trim().isEmpty()) {
            return;
        }

        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

        Message message = Message.builder()
                .setToken(fcmToken)
                .setNotification(notification)
                .build();

        sendFireAndForget(message, "Visible Notification");
    }

    @Override
    public void sendSilentDataPush(String fcmToken, Map<String, String> data) {
        if (fcmToken == null || fcmToken.trim().isEmpty()) {
            return;
        }

        Message message = Message.builder()
                .setToken(fcmToken)
                .putAllData(data)
                .build();

        sendFireAndForget(message, "Silent Data Push");
    }

    private void sendFireAndForget(Message message, String type) {
        try {
            String response = firebaseMessaging.send(message);
            log.debug("Successfully sent {} via FCM. Response: {}", type, response);
        } catch (FirebaseMessagingException e) {
            log.warn("Failed to send {} via FCM: {}", type, e.getMessage());
        } catch (Exception e) {
            log.warn("Unexpected error while sending {} via FCM: {}", type, e.getMessage());
        }
    }
}
