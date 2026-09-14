package com.minh.locket_clone_backend.user.dto;

import com.minh.locket_clone_backend.user.entity.AuthProvider;
import com.minh.locket_clone_backend.user.entity.User;
import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

@Builder
public record UserResponse(
        UUID id,
        String username,
        String phoneNumber,
        AuthProvider authProvider,
        String email,
        String displayName,
        LocalDate birthday,
        String avatarUrl
) {
    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .phoneNumber(user.getPhoneNumber())
                .authProvider(user.getAuthProvider())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .birthday(user.getBirthday())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
}
