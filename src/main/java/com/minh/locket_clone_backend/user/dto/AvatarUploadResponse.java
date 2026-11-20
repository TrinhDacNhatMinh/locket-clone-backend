package com.minh.locket_clone_backend.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record AvatarUploadResponse(
        @Schema(description = "HTTPS URL of the uploaded avatar. Submit this URL to PATCH /api/v1/users/me to update the profile.",
                example = "https://res.cloudinary.com/momento/image/upload/v1/momento/avatars/avatar.jpg")
        String avatarUrl
) {}
