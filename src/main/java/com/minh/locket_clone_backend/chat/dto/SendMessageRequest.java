package com.minh.locket_clone_backend.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

public record SendMessageRequest(
        @Schema(description = "Content of the chat message", example = "Hey, how are you?")
        @NotBlank(message = "Message content is required")
        @Size(max = 2000, message = "Message must not exceed 2000 characters")
        String content
) {}
