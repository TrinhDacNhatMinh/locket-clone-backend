package com.minh.locket_clone_backend.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record UsernameAvailabilityResponse(
        @Schema(description = "Username that was checked", example = "an_2026")
        String username,

        @Schema(description = "True when the username is unused or already belongs to the current user", example = "true")
        boolean available
) {}
