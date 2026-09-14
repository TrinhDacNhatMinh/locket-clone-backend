package com.minh.locket_clone_backend.auth.dto;

import com.minh.locket_clone_backend.user.dto.UserResponse;

public record AuthResponse(
        UserResponse user,
        boolean isNewUser,
        boolean isProfileIncomplete
) {}
