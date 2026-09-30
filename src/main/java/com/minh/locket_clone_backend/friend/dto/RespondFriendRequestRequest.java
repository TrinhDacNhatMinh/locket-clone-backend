package com.minh.locket_clone_backend.friend.dto;

import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

public record RespondFriendRequestRequest(
        @Schema(description = "Action to take on the friend request (ACCEPT or REJECT)", example = "ACCEPT")
        @NotNull(message = "Action is required")
        FriendRequestAction action
) {}
