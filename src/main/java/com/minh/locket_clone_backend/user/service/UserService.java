package com.minh.locket_clone_backend.user.service;

import com.minh.locket_clone_backend.user.dto.*;

import java.util.UUID;

public interface UserService {
    ProfileResponse getProfile(UUID userId);

    ProfileResponse updateProfile(UUID userId, UpdateProfileRequest request);

    ProfileResponse updateUsername(UUID userId, UpdateUsernameRequest request);

    void updateFcmToken(UUID userId, UpdateFcmTokenRequest request);

    PublicProfileResponse getPublicProfile(UUID currentUserId, UUID targetUserId);

    void blockUser(UUID blockerId, UUID blockedId);

    void unblockUser(UUID blockerId, UUID blockedId);

    boolean isBlocked(UUID currentUserId, UUID targetUserId);

    void deleteAccount(UUID userId);
}
