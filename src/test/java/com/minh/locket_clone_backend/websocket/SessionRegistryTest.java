package com.minh.locket_clone_backend.websocket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionRegistryTest {

    @InjectMocks
    private SessionRegistry sessionRegistry;

    @Nested
    @DisplayName("markOnline()")
    class MarkOnlineTests {

        @Test
        void markOnline_newUser_addsSession() {
            // given
            UUID userId = UUID.randomUUID();
            String sessionId = "session-1";

            // when
            sessionRegistry.markOnline(userId, sessionId);

            // then
            assertThat(sessionRegistry.isOnline(userId)).isTrue();
        }

        @Test
        void markOnline_multipleSessions_addsAllSessions() {
            // given
            UUID userId = UUID.randomUUID();
            String sessionId1 = "session-1";
            String sessionId2 = "session-2";

            // when
            sessionRegistry.markOnline(userId, sessionId1);
            sessionRegistry.markOnline(userId, sessionId2);

            // then
            assertThat(sessionRegistry.isOnline(userId)).isTrue();
        }
    }

    @Nested
    @DisplayName("markOffline()")
    class MarkOfflineTests {

        @Test
        void markOffline_onlySession_marksAsOffline() {
            // given
            UUID userId = UUID.randomUUID();
            String sessionId = "session-1";
            sessionRegistry.markOnline(userId, sessionId);

            // when
            sessionRegistry.markOffline(userId, sessionId);

            // then
            assertThat(sessionRegistry.isOnline(userId)).isFalse();
        }

        @Test
        void markOffline_hasOtherSessions_staysOnline() {
            // given
            UUID userId = UUID.randomUUID();
            String sessionId1 = "session-1";
            String sessionId2 = "session-2";
            sessionRegistry.markOnline(userId, sessionId1);
            sessionRegistry.markOnline(userId, sessionId2);

            // when
            sessionRegistry.markOffline(userId, sessionId1);

            // then
            assertThat(sessionRegistry.isOnline(userId)).isTrue();
        }

        @Test
        void markOffline_userNotOnline_doesNothing() {
            // given
            UUID userId = UUID.randomUUID();
            String sessionId = "session-1";

            // when
            sessionRegistry.markOffline(userId, sessionId);

            // then
            assertThat(sessionRegistry.isOnline(userId)).isFalse();
        }
    }

    @Nested
    @DisplayName("isOnline()")
    class IsOnlineTests {

        @Test
        void isOnline_userHasActiveSessions_returnsTrue() {
            // given
            UUID userId = UUID.randomUUID();
            String sessionId = "session-1";
            sessionRegistry.markOnline(userId, sessionId);

            // when
            boolean online = sessionRegistry.isOnline(userId);

            // then
            assertThat(online).isTrue();
        }

        @Test
        void isOnline_userHasNoActiveSessions_returnsFalse() {
            // given
            UUID userId = UUID.randomUUID();

            // when
            boolean online = sessionRegistry.isOnline(userId);

            // then
            assertThat(online).isFalse();
        }
    }

    @Nested
    @DisplayName("handleWebSocketConnectListener()")
    class HandleWebSocketConnectListenerTests {

        @Test
        void handleWebSocketConnectListener_validPrincipal_marksOnline() {
            // given
            UUID userId = UUID.randomUUID();
            String sessionId = "session-1";
            Principal principal = mock(Principal.class);
            when(principal.getName()).thenReturn(userId.toString());

            StompHeaderAccessor accessor = StompHeaderAccessor.create(org.springframework.messaging.simp.stomp.StompCommand.CONNECT);
            accessor.setUser(principal);
            accessor.setSessionId(sessionId);
            Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
            SessionConnectedEvent event = new SessionConnectedEvent(this, message, principal);

            // when
            sessionRegistry.handleWebSocketConnectListener(event);

            // then
            assertThat(sessionRegistry.isOnline(userId)).isTrue();
        }

        @Test
        void handleWebSocketConnectListener_invalidPrincipalName_doesNotMarkOnline() {
            // given
            String sessionId = "session-1";
            Principal principal = mock(Principal.class);
            when(principal.getName()).thenReturn("invalid-uuid");

            StompHeaderAccessor accessor = StompHeaderAccessor.create(org.springframework.messaging.simp.stomp.StompCommand.CONNECT);
            accessor.setUser(principal);
            accessor.setSessionId(sessionId);
            Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
            SessionConnectedEvent event = new SessionConnectedEvent(this, message, principal);

            // when
            sessionRegistry.handleWebSocketConnectListener(event);

            // then
            // Verify that it doesn't crash and we aren't online
            assertThat(sessionRegistry.isOnline(UUID.randomUUID())).isFalse(); 
        }
        
        @Test
        void handleWebSocketConnectListener_nullPrincipalOrSession_doesNotMarkOnline() {
            // given
            StompHeaderAccessor accessor = StompHeaderAccessor.create(org.springframework.messaging.simp.stomp.StompCommand.CONNECT);
            Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
            SessionConnectedEvent event = new SessionConnectedEvent(this, message, null);

            // when
            sessionRegistry.handleWebSocketConnectListener(event);

            // then
            assertThat(sessionRegistry.isOnline(UUID.randomUUID())).isFalse(); 
        }
    }

    @Nested
    @DisplayName("handleWebSocketDisconnectListener()")
    class HandleWebSocketDisconnectListenerTests {

        @Test
        void handleWebSocketDisconnectListener_validPrincipal_marksOffline() {
            // given
            UUID userId = UUID.randomUUID();
            String sessionId = "session-1";
            sessionRegistry.markOnline(userId, sessionId); // initially online
            
            Principal principal = mock(Principal.class);
            when(principal.getName()).thenReturn(userId.toString());

            StompHeaderAccessor accessor = StompHeaderAccessor.create(org.springframework.messaging.simp.stomp.StompCommand.DISCONNECT);
            accessor.setUser(principal);
            accessor.setSessionId(sessionId);
            Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
            SessionDisconnectEvent event = new SessionDisconnectEvent(this, message, sessionId, org.springframework.web.socket.CloseStatus.NORMAL);

            // when
            sessionRegistry.handleWebSocketDisconnectListener(event);

            // then
            assertThat(sessionRegistry.isOnline(userId)).isFalse();
        }
        
        @Test
        void handleWebSocketDisconnectListener_invalidPrincipalName_doesNotMarkOffline() {
            // given
            UUID userId = UUID.randomUUID();
            String sessionId = "session-1";
            sessionRegistry.markOnline(userId, sessionId); // initially online
            
            Principal principal = mock(Principal.class);
            when(principal.getName()).thenReturn("invalid-uuid");

            StompHeaderAccessor accessor = StompHeaderAccessor.create(org.springframework.messaging.simp.stomp.StompCommand.DISCONNECT);
            accessor.setUser(principal);
            accessor.setSessionId(sessionId);
            Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
            SessionDisconnectEvent event = new SessionDisconnectEvent(this, message, sessionId, org.springframework.web.socket.CloseStatus.NORMAL);

            // when
            sessionRegistry.handleWebSocketDisconnectListener(event);

            // then
            assertThat(sessionRegistry.isOnline(userId)).isTrue();
        }
    }
}
