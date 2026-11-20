package com.minh.locket_clone_backend.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CompleteProfileRequest(
        @Schema(description = "User's display name", example = "John Doe")
        @NotBlank(message = "Display name is required")
        @Size(max = 50, message = "Display name must not exceed 50 characters")
        String displayName,

        @Schema(description = "Desired username", example = "john_doe_99")
        @NotBlank(message = "Username is required")
        @Pattern(regexp = "^[a-z0-9_]{3,20}$", message = "Username must be 3-20 characters long, containing only lowercase letters, numbers, and underscores")
        String username
) {}
