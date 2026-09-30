package com.minh.locket_clone_backend.websocket;

import com.minh.locket_clone_backend.websocket.dto.RealtimeEventType;
import com.minh.locket_clone_backend.websocket.dto.RealtimeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimeEventPublisherImpl implements RealtimeEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;
    private final SessionRegistry sessionRegistry;

    @Override
    public void publish(UUID recipientId, RealtimeEventType eventType, Object payload) {
        if (!sessionRegistry.isOnline(recipientId)) {
            log.debug("User {} is offline. Realtime event {} will not be sent via WebSocket.", recipientId, eventType);
            return;
        }

        RealtimeEvent event = new RealtimeEvent(eventType, payload);
        // Spring will route this to /user/{recipientId}/queue/events
        messagingTemplate.convertAndSendToUser(recipientId.toString(), "/queue/events", event);
        
        log.debug("Published realtime event {} to user {}", eventType, recipientId);
    }
}
