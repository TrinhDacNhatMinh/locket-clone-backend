package com.minh.locket_clone_backend.notification.service;

import com.minh.locket_clone_backend.common.dto.CursorPagedResponse;
import com.minh.locket_clone_backend.common.utils.CursorPaginationHelper;
import com.minh.locket_clone_backend.notification.dto.NotificationResponse;
import com.minh.locket_clone_backend.notification.entity.Notification;
import com.minh.locket_clone_backend.notification.entity.NotificationType;
import com.minh.locket_clone_backend.notification.repository.NotificationRepository;
import com.minh.locket_clone_backend.user.entity.User;
import com.minh.locket_clone_backend.user.service.UserService;
import com.minh.locket_clone_backend.websocket.RealtimeEventPublisher;
import com.minh.locket_clone_backend.websocket.SessionRegistry;
import com.minh.locket_clone_backend.websocket.dto.RealtimeEventType;
import com.minh.locket_clone_backend.infrastructure.fcm.service.CloudMessagingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final SessionRegistry sessionRegistry;
    private final RealtimeEventPublisher realtimeEventPublisher;
    private final CloudMessagingService cloudMessagingService;
    private final UserService userService;

    @Override
    @Transactional
    public void notify(UUID recipientId, NotificationType notificationType, RealtimeEventType realtimeEventType, Map<String, Object> payload, Object realtimePayload) {

        if (notificationType == NotificationType.FRIEND_REQUEST || notificationType == NotificationType.FRIEND_ACCEPTED) {
            notificationRepository.save(Notification.builder()
                    .userId(recipientId)
                    .type(notificationType)
                    .payload(payload)
                    .isRead(false)
                    .build());
        }


        if (sessionRegistry.isOnline(recipientId)) {
            realtimeEventPublisher.publish(recipientId, realtimeEventType, realtimePayload);
        } else {
            User recipientUser = userService.getUserByIdIncludingDeleted(recipientId);

            if (recipientUser.getDeletedAt() == null && recipientUser.getFcmToken() != null) {
                String fcmToken = recipientUser.getFcmToken();
                String actorName = (String) payload.getOrDefault("actorDisplayName", "Someone");

                if (notificationType == NotificationType.MESSAGE || notificationType == NotificationType.COMMENT || notificationType == NotificationType.FRIEND_REQUEST) {
                    String title, body;
                    switch (notificationType) {
                        case MESSAGE -> {
                            title = "New Message";
                            body = actorName + " sent you a message";
                        }
                        case COMMENT -> {
                            title = "New Comment";
                            body = actorName + " commented on your photo";
                        }
                        case FRIEND_REQUEST -> {
                            title = "New Friend Request";
                            body = actorName + " sent you a friend request";
                        }
                        default -> {
                            title = "";
                            body = "";
                        }
                    }

                    cloudMessagingService.sendVisibleNotification(fcmToken, title, body);
                } else if (notificationType == NotificationType.NEW_PHOTO) {
                    Map<String, String> data = new HashMap<>();
                    data.put("type", "NEW_PHOTO");
                    if (payload.containsKey("photoId")) {
                        data.put("photoId", String.valueOf(payload.get("photoId")));
                    }
                    cloudMessagingService.sendSilentDataPush(fcmToken, data);
                }
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPagedResponse<NotificationResponse> getNotifications(UUID userId, String cursor, int limit) {
        Pageable pageable = PageRequest.of(0, limit + 1);
        List<Notification> notifications;

        if (cursor == null) {
            notifications = notificationRepository.findByUserIdFirstPage(userId, pageable);
        } else {
            var decoded = CursorPaginationHelper.decodeCursor(cursor);
            notifications = notificationRepository.findByUserIdNextPage(userId, decoded.createdAt(), decoded.id(), pageable);
        }

        boolean hasMore = notifications.size() > limit;
        List<Notification> page = hasMore ? notifications.subList(0, limit) : notifications;

        List<NotificationResponse> responses = page.stream()
                .map(NotificationResponse::from)
                .toList();

        String nextCursor = null;
        if (hasMore && !page.isEmpty()) {
            Notification last = page.get(page.size() - 1);
            nextCursor = CursorPaginationHelper.encodeCursor(last.getCreatedAt(), last.getId());
        }

        return new CursorPagedResponse<>(responses, nextCursor, hasMore);
    }

    @Override
    @Transactional
    public void markAsRead(UUID userId, UUID notificationId) {
        notificationRepository.markAsRead(notificationId, userId);
    }

    @Override
    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsRead(userId);
    }
}
