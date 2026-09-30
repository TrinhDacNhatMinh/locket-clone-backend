package com.minh.locket_clone_backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

public record LoginRequest(
        @Schema(description = "Firebase ID Token obtained from client SDK", example = "eyJhbGciOiJSUzI1NiIs...")
        @NotBlank(message = "Firebase ID Token is required")
        String firebaseIdToken
) {}
