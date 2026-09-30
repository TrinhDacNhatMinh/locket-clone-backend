package com.minh.locket_clone_backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

public record UpdateFcmTokenRequest(
        @Schema(description = "Firebase Cloud Messaging token for push notifications")
        @NotBlank(message = "FCM token is required")
        String fcmToken
) {}
