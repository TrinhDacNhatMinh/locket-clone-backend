package com.minh.locket_clone_backend.friend.dto;

import jakarta.validation.constraints.NotNull;

public record RespondFriendRequestRequest(
        @NotNull(message = "Action is required")
        FriendRequestAction action
) {}
