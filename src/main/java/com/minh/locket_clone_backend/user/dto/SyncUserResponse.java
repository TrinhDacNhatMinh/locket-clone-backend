package com.minh.locket_clone_backend.user.dto;

public record SyncUserResponse(
        UserResponse user,
        boolean isNewUser,
        boolean isProfileIncomplete
) {
}
