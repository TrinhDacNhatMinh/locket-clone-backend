package com.minh.locket_clone_backend.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        @NotBlank(message = "Message content is required")
        @Size(max = 2000, message = "Message must not exceed 2000 characters")
        String content
) {}
