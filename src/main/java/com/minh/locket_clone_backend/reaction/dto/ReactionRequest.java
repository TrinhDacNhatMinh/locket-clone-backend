package com.minh.locket_clone_backend.reaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReactionRequest(
        @NotBlank(message = "Emoji is required")
        @Size(max = 10, message = "Emoji string is too long")
        String emoji
) {}
