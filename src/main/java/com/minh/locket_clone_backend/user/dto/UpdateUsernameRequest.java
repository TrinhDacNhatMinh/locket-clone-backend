package com.minh.locket_clone_backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateUsernameRequest(
        @NotBlank(message = "Username is required")
        @Pattern(regexp = "^[a-z0-9_]{3,20}$", message = "Username must be 3-20 characters long, containing only lowercase letters, numbers, and underscores")
        String username
) {}
