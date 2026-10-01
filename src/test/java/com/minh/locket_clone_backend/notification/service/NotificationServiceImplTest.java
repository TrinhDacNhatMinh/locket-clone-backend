package com.minh.locket_clone_backend.notification.service;

import com.minh.locket_clone_backend.common.dto.CursorPagedResponse;
import com.minh.locket_clone_backend.infrastructure.fcm.service.CloudMessagingService;
import com.minh.locket_clone_backend.notification.dto.NotificationResponse;
import com.minh.locket_clone_backend.notification.entity.Notification;
import com.minh.locket_clone_backend.notification.entity.NotificationType;
import com.minh.locket_clone_backend.notification.repository.NotificationRepository;
import com.minh.locket_clone_backend.user.entity.User;
import com.minh.locket_clone_backend.user.service.UserService;
import com.minh.locket_clone_backend.websocket.RealtimeEventPublisher;
import com.minh.locket_clone_backend.websocket.SessionRegistry;
import com.minh.locket_clone_backend.websocket.dto.RealtimeEventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private SessionRegistry sessionRegistry;

    @Mock
    private RealtimeEventPublisher realtimeEventPublisher;

    @Mock
    private CloudMessagingService cloudMessagingService;

    @Mock
    private UserService userService;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Nested
    @DisplayName("notify()")
    class NotifyTests {

        @Test
        void notify_friendRequest_savesToDbAndSendsRealtimeIfOnline() {
            // given
            UUID recipientId = UUID.randomUUID();
            Map<String, Object> payload = Map.of("actorDisplayName", "Alice");

            when(sessionRegistry.isOnline(recipientId)).thenReturn(true);

            // when
            notificationService.notify(recipientId, NotificationType.FRIEND_REQUEST, RealtimeEventType.NOTIFICATION, payload, null);

            // then
            ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).save(notificationCaptor.capture());
            assertThat(notificationCaptor.getValue().getType()).isEqualTo(NotificationType.FRIEND_REQUEST);
            
            verify(realtimeEventPublisher).publish(eq(recipientId), eq(RealtimeEventType.NOTIFICATION), eq(null));
            verify(userService, never()).getUserByIdIncludingDeleted(any());
        }

        @Test
        void notify_messageOffline_sendsVisibleFcm() {
            // given
            UUID recipientId = UUID.randomUUID();
            Map<String, Object> payload = Map.of("actorDisplayName", "Bob");
            User recipient = new User();
            recipient.setId(recipientId);
            recipient.setFcmToken("fcm-token-123");

            when(sessionRegistry.isOnline(recipientId)).thenReturn(false);
            when(userService.getUserByIdIncludingDeleted(recipientId)).thenReturn(recipient);

            // when
            notificationService.notify(recipientId, NotificationType.MESSAGE, RealtimeEventType.CHAT_MESSAGE, payload, null);

            // then
            verify(notificationRepository, never()).save(any()); // MESSAGE is not saved to DB in this logic
            verify(cloudMessagingService).sendVisibleNotification(eq("fcm-token-123"), eq("New Message"), eq("Bob sent you a message"));
        }

        @Test
        void notify_newPhotoOffline_sendsSilentFcm() {
            // given
            UUID recipientId = UUID.randomUUID();
            Map<String, Object> payload = Map.of("photoId", "photo-uuid");
            User recipient = new User();
            recipient.setId(recipientId);
            recipient.setFcmToken("fcm-token-456");

            when(sessionRegistry.isOnline(recipientId)).thenReturn(false);
            when(userService.getUserByIdIncludingDeleted(recipientId)).thenReturn(recipient);

            // when
            notificationService.notify(recipientId, NotificationType.NEW_PHOTO, RealtimeEventType.WIDGET_UPDATE, payload, null);

            // then
            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, String>> mapCaptor = ArgumentCaptor.forClass(Map.class);
            verify(cloudMessagingService).sendSilentDataPush(eq("fcm-token-456"), mapCaptor.capture());
            assertThat(mapCaptor.getValue().get("type")).isEqualTo("NEW_PHOTO");
            assertThat(mapCaptor.getValue().get("photoId")).isEqualTo("photo-uuid");
        }

        @Test
        void notify_offlineDeletedUser_doesNotSendFcm() {
            // given
            UUID recipientId = UUID.randomUUID();
            User recipient = new User();
            recipient.setId(recipientId);
            recipient.setFcmToken("fcm-token-789");
            recipient.setDeletedAt(Instant.now());

            when(sessionRegistry.isOnline(recipientId)).thenReturn(false);
            when(userService.getUserByIdIncludingDeleted(recipientId)).thenReturn(recipient);

            // when
            notificationService.notify(recipientId, NotificationType.COMMENT, RealtimeEventType.NOTIFICATION, Map.of(), null);

            // then
            verify(cloudMessagingService, never()).sendVisibleNotification(anyString(), anyString(), anyString());
            verify(cloudMessagingService, never()).sendSilentDataPush(anyString(), any());
        }
    }

    @Nested
    @DisplayName("getNotifications()")
    class GetNotificationsTests {
        @Test
        void getNotifications_noCursor_fetchesFirstPage() {
            // given
            UUID userId = UUID.randomUUID();
            Notification notif1 = Notification.builder().type(NotificationType.FRIEND_REQUEST).build();
            notif1.setCreatedAt(Instant.now());
            notif1.setId(UUID.randomUUID());
            
            when(notificationRepository.findByUserIdFirstPage(eq(userId), any(Pageable.class)))
                    .thenReturn(List.of(notif1));

            // when
            CursorPagedResponse<NotificationResponse> response = notificationService.getNotifications(userId, null, 10);

            // then
            verify(notificationRepository).findByUserIdFirstPage(eq(userId), any(Pageable.class));
            assertThat(response.data()).hasSize(1);
            assertThat(response.hasMore()).isFalse();
            assertThat(response.nextCursor()).isNull();
        }

        @Test
        void getNotifications_withCursor_fetchesNextPage() {
            // given
            UUID userId = UUID.randomUUID();
            UUID lastId = UUID.randomUUID();
            Instant now = Instant.now();
            String rawCursor = now.toString() + "_" + lastId;
            String encodedCursor = Base64.getUrlEncoder().withoutPadding().encodeToString(rawCursor.getBytes());

            Notification notif = Notification.builder().type(NotificationType.FRIEND_REQUEST).build();
            notif.setCreatedAt(Instant.now());
            notif.setId(UUID.randomUUID());
            Notification notif2 = Notification.builder().type(NotificationType.FRIEND_REQUEST).build();
            notif2.setCreatedAt(Instant.now());
            notif2.setId(UUID.randomUUID());

            // limit = 1, repo returns 2 (hasMore = true)
            when(notificationRepository.findByUserIdNextPage(eq(userId), any(Instant.class), eq(lastId), any(Pageable.class)))
                    .thenReturn(List.of(notif, notif2));

            // when
            CursorPagedResponse<NotificationResponse> response = notificationService.getNotifications(userId, encodedCursor, 1);

            // then
            verify(notificationRepository).findByUserIdNextPage(eq(userId), any(Instant.class), eq(lastId), any(Pageable.class));
            assertThat(response.data()).hasSize(1);
            assertThat(response.hasMore()).isTrue();
            assertThat(response.nextCursor()).isNotNull();
        }
    }

    @Nested
    @DisplayName("markAsRead() and markAllAsRead()")
    class MarkAsReadTests {
        @Test
        void markAsRead_callsRepository() {
            // given
            UUID userId = UUID.randomUUID();
            UUID notifId = UUID.randomUUID();

            // when
            notificationService.markAsRead(userId, notifId);

            // then
            verify(notificationRepository).markAsRead(notifId, userId);
        }

        @Test
        void markAllAsRead_callsRepository() {
            // given
            UUID userId = UUID.randomUUID();

            // when
            notificationService.markAllAsRead(userId);

            // then
            verify(notificationRepository).markAllAsRead(userId);
        }
    }
}
