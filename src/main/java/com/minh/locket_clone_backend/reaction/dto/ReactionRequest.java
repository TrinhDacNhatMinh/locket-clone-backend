package com.minh.locket_clone_backend.reaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

public record ReactionRequest(
        @Schema(description = "Emoji character or shortcode for the reaction", example = "🔥")
        @NotBlank(message = "Emoji is required")
        @Size(max = 10, message = "Emoji string is too long")
        String emoji
) {}
