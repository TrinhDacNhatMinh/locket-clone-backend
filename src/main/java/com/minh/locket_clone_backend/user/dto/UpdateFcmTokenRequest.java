package com.minh.locket_clone_backend.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateFcmTokenRequest(
        @NotBlank(message = "FCM token is required")
        String fcmToken
) {}
