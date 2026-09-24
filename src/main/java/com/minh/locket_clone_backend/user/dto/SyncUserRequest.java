package com.minh.locket_clone_backend.user.dto;

import com.minh.locket_clone_backend.user.entity.AuthProvider;

public record SyncUserRequest(
        String firebaseUid,
        AuthProvider expectedProvider,
        String email,
        String displayName,
        String avatarUrl,
        String phoneNumber
) {
}
