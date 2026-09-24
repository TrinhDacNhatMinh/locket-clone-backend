package com.minh.locket_clone_backend.websocket.security;

import com.google.firebase.auth.FirebaseAuthException;
import com.minh.locket_clone_backend.auth.security.CustomUserDetails;
import com.minh.locket_clone_backend.auth.service.AuthenticationResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompHandshakeInterceptor implements HandshakeInterceptor {

    private final AuthenticationResolver authenticationResolver;

    @Override
    public boolean beforeHandshake(@NonNull ServerHttpRequest request,
                                   @NonNull ServerHttpResponse response,
                                   @NonNull WebSocketHandler wsHandler,
                                   @NonNull Map<String, Object> attributes) {
        String token = extractToken(request);

        if (!StringUtils.hasText(token)) {
            log.warn("WebSocket handshake failed: No token provided");
            return false;
        }

        try {
            CustomUserDetails userDetails = authenticationResolver.resolveUser(token);
            attributes.put("userId", userDetails.userId());
            attributes.put("firebaseUid", userDetails.firebaseUid());
            log.debug("WebSocket handshake successful for user: {}", userDetails.userId());
            return true;
        } catch (FirebaseAuthException | RuntimeException e) {
            log.warn("WebSocket handshake failed: Invalid token - {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void afterHandshake(@NonNull ServerHttpRequest request,
                               @NonNull ServerHttpResponse response,
                               @NonNull WebSocketHandler wsHandler,
                               @Nullable Exception exception) {
        // Nothing to do after handshake
    }

    private String extractToken(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            // First check query param (ws://...?token=xxx)
            String token = servletRequest.getServletRequest().getParameter("token");
            if (StringUtils.hasText(token)) {
                return token;
            }

            // Fallback to Authorization header
            String bearerToken = servletRequest.getServletRequest().getHeader("Authorization");
            if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
                return bearerToken.substring(7);
            }
        }
        return null;
    }
}
