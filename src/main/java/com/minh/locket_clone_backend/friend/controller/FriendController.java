package com.minh.locket_clone_backend.friend.controller;

import com.minh.locket_clone_backend.auth.security.CustomUserDetails;
import com.minh.locket_clone_backend.common.response.BaseResponse;
import com.minh.locket_clone_backend.friend.dto.FriendRequestResponse;
import com.minh.locket_clone_backend.friend.dto.RespondFriendRequestRequest;
import com.minh.locket_clone_backend.friend.dto.SendFriendRequestRequest;
import com.minh.locket_clone_backend.friend.service.FriendService;
import com.minh.locket_clone_backend.user.dto.PublicProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/friends")
@RequiredArgsConstructor
@Tag(name = "Friends", description = "Endpoints for managing friends and friend requests")
public class FriendController {

    private final FriendService friendService;

    @Operation(summary = "Send a friend request")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Friend request sent or auto-accepted successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error (already friends, existing request)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is blocked)"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/requests")
    public ResponseEntity<Void> sendFriendRequest(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody SendFriendRequestRequest request) {
        friendService.sendFriendRequest(userDetails.userId(), request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Respond to a friend request (ACCEPT or REJECT)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Responded successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error (invalid status or not your request)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Friend request not found")
    })
    @PatchMapping("/requests/{requestId}")
    public ResponseEntity<Void> respondFriendRequest(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID requestId,
            @Valid @RequestBody RespondFriendRequestRequest request) {
        friendService.respondFriendRequest(userDetails.userId(), requestId, request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Get pending friend requests")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved pending friend requests"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/requests/pending")
    public ResponseEntity<BaseResponse<List<FriendRequestResponse>>> getPendingFriendRequests(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<FriendRequestResponse> requests = friendService.getPendingFriendRequests(userDetails.userId());
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(requests));
    }

    @Operation(summary = "Get list of friends")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Friend list retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<BaseResponse<List<PublicProfileResponse>>> getFriendList(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<PublicProfileResponse> friends = friendService.getFriendList(userDetails.userId());
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(friends));
    }

    @Operation(summary = "Remove a friend")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Friend removed successfully"),
            @ApiResponse(responseCode = "400", description = "Users are not friends"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @DeleteMapping("/{friendUserId}")
    public ResponseEntity<Void> removeFriend(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID friendUserId) {
        friendService.removeFriend(userDetails.userId(), friendUserId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
