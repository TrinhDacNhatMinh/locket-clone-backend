package com.minh.locket_clone_backend.user.dto;

import com.minh.locket_clone_backend.user.entity.User;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record PublicProfileResponse(
        UUID id,
        String username,
        String displayName,
        String avatarUrl,
        Instant createdAt
) {
    public static PublicProfileResponse from(User user) {
        return PublicProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
