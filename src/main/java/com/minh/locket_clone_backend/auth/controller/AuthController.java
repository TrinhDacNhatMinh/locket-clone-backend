package com.minh.locket_clone_backend.auth.controller;

import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.minh.locket_clone_backend.auth.dto.AuthResponse;
import com.minh.locket_clone_backend.auth.dto.LoginRequest;
import com.minh.locket_clone_backend.auth.service.FirebaseUserSyncService;
import com.minh.locket_clone_backend.common.exception.BusinessException;
import com.minh.locket_clone_backend.common.exception.ErrorCode;
import com.minh.locket_clone_backend.common.response.BaseResponse;
import com.minh.locket_clone_backend.user.entity.AuthProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final FirebaseAuth firebaseAuth;
    private final FirebaseUserSyncService userSyncService;

    @Operation(summary = "Login with Firebase Phone Auth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired Firebase token")
    })
    @PostMapping("/login/phone")
    public ResponseEntity<BaseResponse<AuthResponse>> loginWithPhone(@Valid @RequestBody LoginRequest request) {
        FirebaseToken token = verifyToken(request.firebaseIdToken());
        validateProvider(token, "phone");
        AuthResponse response = userSyncService.syncUser(token, AuthProvider.PHONE);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @Operation(summary = "Login with Firebase Google Auth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired Firebase token")
    })
    @PostMapping("/login/google")
    public ResponseEntity<BaseResponse<AuthResponse>> loginWithGoogle(@Valid @RequestBody LoginRequest request) {
        FirebaseToken token = verifyToken(request.firebaseIdToken());
        validateProvider(token, "google.com");
        AuthResponse response = userSyncService.syncUser(token, AuthProvider.GOOGLE);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @Operation(summary = "Logout")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Logout successful (client should clear token)")
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }

    private FirebaseToken verifyToken(String idToken) {
        try {
            return firebaseAuth.verifyIdToken(idToken);
        } catch (FirebaseAuthException e) {
            log.warn("Firebase token verification failed: {}", e.getMessage());
            if (AuthErrorCode.EXPIRED_ID_TOKEN.equals(e.getAuthErrorCode())) {
                throw new BusinessException(ErrorCode.FIREBASE_TOKEN_EXPIRED);
            }
            throw new BusinessException(ErrorCode.FIREBASE_TOKEN_INVALID);
        }
    }

    private void validateProvider(FirebaseToken token, String expectedSignInProvider) {
        Object signInProvider = null;
        Object firebaseClaim = token.getClaims().get("firebase");
        if (firebaseClaim instanceof Map) {
            signInProvider = ((Map<?, ?>) firebaseClaim).get("sign_in_provider");
        }

        if (signInProvider == null || !signInProvider.toString().equals(expectedSignInProvider)) {
            log.warn("Invalid auth provider. Expected {}, got {}", expectedSignInProvider, signInProvider);
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Invalid authentication provider");
        }
    }
}
