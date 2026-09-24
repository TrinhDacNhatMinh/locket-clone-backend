package com.minh.locket_clone_backend.websocket;

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
    public void publish(UUID userId, String eventType, Object payload) {
        if (!sessionRegistry.isOnline(userId)) {
            log.debug("User {} is offline. Realtime event {} will not be sent via WebSocket.", userId, eventType);
            return;
        }

        RealtimeEvent event = new RealtimeEvent(eventType, payload);
        // We use convertAndSendToUser. Since we set the Principal name to userId.toString(), 
        // Spring will route this to /user/{userId}/queue/events
        messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/events", event);
        
        log.debug("Published realtime event {} to user {}", eventType, userId);
    }
}
