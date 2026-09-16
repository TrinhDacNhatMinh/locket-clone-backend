package com.minh.locket_clone_backend.user.dto;

import com.minh.locket_clone_backend.user.entity.User;
import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Builder
public record ProfileResponse(
        UUID id,
        String username,
        String displayName,
        String email,
        String phoneNumber,
        LocalDate birthday,
        String avatarUrl,
        boolean isProfileCompleted,
        Instant createdAt
) {
    public static ProfileResponse from(User user) {
        return ProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .birthday(user.getBirthday())
                .avatarUrl(user.getAvatarUrl())
                .isProfileCompleted(user.isProfileCompleted())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
