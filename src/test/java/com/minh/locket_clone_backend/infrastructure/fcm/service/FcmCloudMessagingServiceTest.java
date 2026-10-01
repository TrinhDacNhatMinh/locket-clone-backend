package com.minh.locket_clone_backend.infrastructure.fcm.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FcmCloudMessagingServiceTest {

    @Mock
    private FirebaseMessaging firebaseMessaging;

    @InjectMocks
    private FcmCloudMessagingService fcmService;

    @Nested
    @DisplayName("sendVisibleNotification()")
    class SendVisibleNotificationTests {

        @Test
        void sendVisibleNotification_nullOrEmptyToken_doesNothing() throws FirebaseMessagingException {
            // given
            String token = "   ";

            // when
            fcmService.sendVisibleNotification(token, "Title", "Body");

            // then
            verify(firebaseMessaging, never()).send(any(Message.class));
        }

        @Test
        void sendVisibleNotification_validToken_sendsMessage() throws FirebaseMessagingException {
            // given
            String token = "valid-fcm-token";
            String title = "Test Title";
            String body = "Test Body";

            when(firebaseMessaging.send(any(Message.class))).thenReturn("message-id");

            // when
            fcmService.sendVisibleNotification(token, title, body);

            // then
            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
            verify(firebaseMessaging, times(1)).send(messageCaptor.capture());
            // It's hard to verify Message contents deeply because Google's Message class hides its fields.
            // We just verify it was called once.
            assertThat(messageCaptor.getValue()).isNotNull();
        }

        @Test
        void sendVisibleNotification_firebaseThrowsException_catchesException() throws FirebaseMessagingException {
            // given
            String token = "valid-fcm-token";
            when(firebaseMessaging.send(any(Message.class)))
                    .thenThrow(mock(FirebaseMessagingException.class));

            // when
            // Should not throw exception, just log it
            fcmService.sendVisibleNotification(token, "Title", "Body");

            // then
            verify(firebaseMessaging, times(1)).send(any(Message.class));
        }
    }

    @Nested
    @DisplayName("sendSilentDataPush()")
    class SendSilentDataPushTests {

        @Test
        void sendSilentDataPush_nullOrEmptyToken_doesNothing() throws FirebaseMessagingException {
            // given
            String token = "   ";

            // when
            fcmService.sendSilentDataPush(token, Map.of("key", "value"));

            // then
            verify(firebaseMessaging, never()).send(any(Message.class));
        }

        @Test
        void sendSilentDataPush_validToken_sendsMessage() throws FirebaseMessagingException {
            // given
            String token = "valid-fcm-token";
            Map<String, String> data = Map.of("action", "refresh_widget");

            when(firebaseMessaging.send(any(Message.class))).thenReturn("message-id");

            // when
            fcmService.sendSilentDataPush(token, data);

            // then
            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
            verify(firebaseMessaging, times(1)).send(messageCaptor.capture());
            assertThat(messageCaptor.getValue()).isNotNull();
        }
    }
}
