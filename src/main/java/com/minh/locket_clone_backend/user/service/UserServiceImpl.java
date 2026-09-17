package com.minh.locket_clone_backend.user.service;

import com.minh.locket_clone_backend.common.exception.BusinessException;
import com.minh.locket_clone_backend.common.exception.ErrorCode;
import com.minh.locket_clone_backend.friend.service.FriendService;
import com.minh.locket_clone_backend.user.dto.*;
import com.minh.locket_clone_backend.user.entity.Block;
import com.minh.locket_clone_backend.user.entity.User;
import com.minh.locket_clone_backend.user.repository.BlockRepository;
import com.minh.locket_clone_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final BlockRepository blockRepository;
    private final ObjectProvider<FriendService> friendServiceProvider;

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

        if (blockRepository.existsByBlockerIdAndBlockedId(targetUserId, currentUserId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        return PublicProfileResponse.from(targetUser);
    }

    @Override
    @Transactional
    public void blockUser(UUID blockerId, UUID blockedId) {
        if (blockerId.equals(blockedId)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        getUserById(blockedId); // Verify blocked user exists

        if (!blockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
            blockRepository.save(Block.builder()
                    .blockerId(blockerId)
                    .blockedId(blockedId)
                    .build());

            // Auto-remove friendship if exists, and delete any pending requests
            friendServiceProvider.getObject().removeFriendshipIfExists(blockerId, blockedId);
            friendServiceProvider.getObject().deleteFriendRequestsBetween(blockerId, blockedId);
            log.info("User {} blocked user {}", blockerId, blockedId);
        }
    }

    @Override
    @Transactional
    public void unblockUser(UUID blockerId, UUID blockedId) {
        int deletedCount = blockRepository.deleteByBlockerIdAndBlockedId(blockerId, blockedId);
        if (deletedCount > 0) {
            log.info("User {} unblocked user {}", blockerId, blockedId);
        } else {
            log.debug("User {} attempted to unblock user {} but no block existed", blockerId, blockedId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBlocked(UUID currentUserId, UUID targetUserId) {
        return blockRepository.existsBidirectional(currentUserId, targetUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicProfileResponse> searchUsers(UUID currentUserId, String query, int limit, int page) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        Pageable pageable = PageRequest.of(page, limit);
        List<User> users = userRepository.searchUsers(query.trim(), currentUserId, pageable);

        return users.stream()
                .map(PublicProfileResponse::from)
                .collect(Collectors.toList());
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

        // Cascade soft-delete owned photos
        // TODO: uncomment once Photo entity exists

        // Hard-delete all Friend relationships and FriendRequests involving this user
        friendServiceProvider.getObject().deleteAllInvolvingUser(userId);

        // Hard-delete all blocks involving this user
        blockRepository.deleteAllByUserId(userId);

        log.info("Account soft-deleted for user {}", userId);
    }

    private User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
