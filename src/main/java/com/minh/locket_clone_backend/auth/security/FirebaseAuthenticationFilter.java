package com.minh.locket_clone_backend.auth.security;

import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.minh.locket_clone_backend.common.exception.BusinessException;
import com.minh.locket_clone_backend.common.exception.ErrorCode;
import com.minh.locket_clone_backend.user.entity.User;
import com.minh.locket_clone_backend.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {

    private final FirebaseAuth firebaseAuth;
    private final UserRepository userRepository;
    private final HandlerExceptionResolver handlerExceptionResolver;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt)) {
                FirebaseToken decodedToken;
                try {
                    decodedToken = firebaseAuth.verifyIdToken(jwt);
                } catch (FirebaseAuthException e) {
                    log.warn("Firebase Auth Exception: {}", e.getMessage());
                    if (AuthErrorCode.EXPIRED_ID_TOKEN.equals(e.getAuthErrorCode())) {
                        throw new BusinessException(ErrorCode.FIREBASE_TOKEN_EXPIRED);
                    }
                    throw new BusinessException(ErrorCode.FIREBASE_TOKEN_INVALID);
                }

                String firebaseUid = decodedToken.getUid();

                // Get internal user ID if exists
                User user = userRepository.findByFirebaseUid(firebaseUid).orElse(null);
                if (user == null) {
                    log.warn("User with Firebase UID {} not found in database (may be deleted)", firebaseUid);
                    throw new BusinessException(ErrorCode.USER_NOT_FOUND, "User account not found or has been deleted");
                }
                UUID userId = user.getId();

                CustomUserDetails userDetails = new CustomUserDetails(userId, firebaseUid);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, Collections.emptyList()
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            log.error("Authentication failed", ex);
            // Delegate exception to GlobalExceptionHandler so it returns the proper JSON response
            handlerExceptionResolver.resolveException(request, response, null, ex);
            return; // Stop filter chain execution if authentication fails
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
