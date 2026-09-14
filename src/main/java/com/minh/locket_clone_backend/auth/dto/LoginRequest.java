package com.minh.locket_clone_backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Firebase ID Token is required")
        String firebaseIdToken
) {}
