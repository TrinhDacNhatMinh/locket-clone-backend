package com.minh.locket_clone_backend.friend.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public record SendFriendRequestRequest(
        @Schema(description = "UUID of the user to send the friend request to")
        @NotNull(message = "Addressee ID is required") UUID addresseeId) {
}
