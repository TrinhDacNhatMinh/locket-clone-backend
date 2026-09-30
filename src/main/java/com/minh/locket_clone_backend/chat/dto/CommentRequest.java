package com.minh.locket_clone_backend.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

public record CommentRequest(
        @Schema(description = "Content of the comment on a photo", example = "Awesome photo!")
        @NotBlank(message = "Comment content is required")
        @Size(max = 2000, message = "Comment must not exceed 2000 characters")
        String content
) {}
