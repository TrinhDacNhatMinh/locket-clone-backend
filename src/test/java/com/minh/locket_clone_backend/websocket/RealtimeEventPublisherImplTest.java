package com.minh.locket_clone_backend.websocket;

import com.minh.locket_clone_backend.websocket.dto.RealtimeEvent;
import com.minh.locket_clone_backend.websocket.dto.RealtimeEventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RealtimeEventPublisherImplTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private SessionRegistry sessionRegistry;

    @InjectMocks
    private RealtimeEventPublisherImpl realtimeEventPublisher;

    @Nested
    @DisplayName("publish()")
    class PublishTests {

        @Test
        void publish_userIsOffline_doesNotSendMessage() {
            // given
            UUID recipientId = UUID.randomUUID();
            RealtimeEventType eventType = RealtimeEventType.CHAT_MESSAGE;
            Object payload = "test-payload";

            when(sessionRegistry.isOnline(recipientId)).thenReturn(false);

            // when
            realtimeEventPublisher.publish(recipientId, eventType, payload);

            // then
            verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
        }

        @Test
        void publish_userIsOnline_sendsMessageWithCorrectPayload() {
            // given
            UUID recipientId = UUID.randomUUID();
            RealtimeEventType eventType = RealtimeEventType.CHAT_MESSAGE;
            Object payload = "test-payload";

            when(sessionRegistry.isOnline(recipientId)).thenReturn(true);

            // when
            realtimeEventPublisher.publish(recipientId, eventType, payload);

            // then
            ArgumentCaptor<RealtimeEvent> eventCaptor = ArgumentCaptor.forClass(RealtimeEvent.class);
            verify(messagingTemplate, times(1)).convertAndSendToUser(
                    eq(recipientId.toString()),
                    eq("/queue/events"),
                    eventCaptor.capture()
            );

            RealtimeEvent capturedEvent = eventCaptor.getValue();
            assertThat(capturedEvent.type()).isEqualTo(eventType);
            assertThat(capturedEvent.payload()).isEqualTo(payload);
        }
    }
}
