package com.minh.locket_clone_backend.friend.dto;

import com.minh.locket_clone_backend.user.dto.PublicProfileResponse;

import java.util.UUID;

public record FriendRequestResponse(
        UUID requestId,
        PublicProfileResponse requester
) {}
