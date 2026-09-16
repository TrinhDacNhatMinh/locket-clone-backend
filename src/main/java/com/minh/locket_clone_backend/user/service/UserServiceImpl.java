package com.minh.locket_clone_backend.user.service;

import com.minh.locket_clone_backend.common.exception.BusinessException;
import com.minh.locket_clone_backend.common.exception.ErrorCode;
import com.minh.locket_clone_backend.user.dto.*;
import com.minh.locket_clone_backend.user.entity.User;
import com.minh.locket_clone_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getProfile(UUID userId) {
        User user = getUserById(userId);
        return ProfileResponse.from(user);
    }

    @Override
    @Transactional
    public ProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = getUserById(userId);

        if (request.email() != null) {
            if (userRepository.existsByEmailAndIdNot(request.email(), userId)) {
                log.warn("User {} attempted to update email to {} which is already in use", userId, request.email());
                throw new BusinessException(ErrorCode.EMAIL_ALREADY_IN_USE);
            }
            user.setEmail(request.email());
        }

        if (request.phoneNumber() != null) {
            user.setPhoneNumber(request.phoneNumber());
        }

        if (request.displayName() != null) {
            user.setDisplayName(request.displayName());
        }

        if (request.birthday() != null) {
            user.setBirthday(request.birthday());
        }

        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl());
        }

        User savedUser = userRepository.save(user);
        log.info("Profile updated for user {}", userId);

        return ProfileResponse.from(savedUser);
    }

    @Override
    @Transactional
    public ProfileResponse updateUsername(UUID userId, UpdateUsernameRequest request) {
        User user = getUserById(userId);

        if (request.username().equals(user.getUsername())) {
            return ProfileResponse.from(user);
        }

        if (userRepository.existsByUsername(request.username())) {
            log.warn("User {} attempted to claim username {} which is already taken", userId, request.username());
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_TAKEN);
        }

        user.setUsername(request.username());
        user.setProfileCompleted(true);

        User savedUser = userRepository.save(user);
        log.info("User {} claimed permanent username {}", userId, request.username());

        return ProfileResponse.from(savedUser);
    }

    @Override
    @Transactional
    public void updateFcmToken(UUID userId, UpdateFcmTokenRequest request) {
        User user = getUserById(userId);
        user.setFcmToken(request.fcmToken());
        userRepository.save(user);
        log.info("Updated FCM token for user {}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public PublicProfileResponse getPublicProfile(UUID currentUserId, UUID targetUserId) {
        User targetUser = getUserById(targetUserId);

        // TODO: Integrate with Block module here

        return PublicProfileResponse.from(targetUser);
    }

    @Override
    @Transactional
    public void deleteAccount(UUID userId) {
        User user = getUserById(userId);

        // Soft-delete the User record itself
        user.setDeletedAt(Instant.now());

        // Release unique identifiers so the same Firebase account
        user.setFirebaseUid(null);
        user.setPhoneNumber(null);
        user.setEmail(null);
        user.setUsername(null);
        user.setFcmToken(null);
        userRepository.save(user);

        // Step 3: Cascade soft-delete owned photos
        // TODO(Sprint 4): uncomment once Photo entity exists

        // Step 4: Hard-delete all Friend relationships involving this user
        // TODO(Sprint 3): implement FriendRepository.deleteAllInvolvingUser(userId)

        // Step 5: Hard-delete all FriendRequest involving this user, both directions
        // (requester -> this user, and this user -> addressee)
        // TODO(Sprint 3): implement FriendRequestRepository.deleteAllInvolvingUser(userId)

        log.info("Account soft-deleted for user {}", userId);
    }

    private User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
