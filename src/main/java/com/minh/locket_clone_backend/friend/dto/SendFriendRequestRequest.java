package com.minh.locket_clone_backend.friend.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SendFriendRequestRequest(
        @NotNull(message = "Addressee ID is required")
        UUID addresseeId
) {}
