package com.minh.locket_clone_backend.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UsernameAvailabilityRequest(
        @Schema(description = "Username to check", example = "an_2026", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Username is required")
        @Pattern(regexp = "^[a-z0-9_]{3,20}$", message = "Username must be 3-20 characters long, containing only lowercase letters, numbers, and underscores")
        String username
) {}
