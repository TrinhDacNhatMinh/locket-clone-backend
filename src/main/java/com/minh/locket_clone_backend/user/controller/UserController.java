package com.minh.locket_clone_backend.user.controller;

import com.minh.locket_clone_backend.auth.security.CustomUserDetails;
import com.minh.locket_clone_backend.common.response.BaseResponse;
import com.minh.locket_clone_backend.user.dto.ProfileResponse;
import com.minh.locket_clone_backend.user.dto.PublicProfileResponse;
import com.minh.locket_clone_backend.user.dto.UpdateProfileRequest;
import com.minh.locket_clone_backend.user.dto.UpdateUsernameRequest;
import com.minh.locket_clone_backend.user.dto.UpdateFcmTokenRequest;
import com.minh.locket_clone_backend.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
@Tag(name = "User Profile", description = "Endpoints for managing user profiles")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Get current user profile")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/me")
    public ResponseEntity<BaseResponse<ProfileResponse>> getMe(@AuthenticationPrincipal CustomUserDetails userDetails) {
        ProfileResponse response = userService.getProfile(userDetails.userId());
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(response));
    }

    @Operation(summary = "Update current user profile")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "409", description = "Email already in use")
    })
    @PatchMapping("/me")
    public ResponseEntity<BaseResponse<ProfileResponse>> updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {
        ProfileResponse response = userService.updateProfile(userDetails.userId(), request);
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(response));
    }

    @Operation(summary = "Complete profile by setting a permanent username")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Username updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error (invalid format)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "409", description = "Username is already taken")
    })
    @PatchMapping("/me/username")
    public ResponseEntity<BaseResponse<ProfileResponse>> updateUsername(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateUsernameRequest request) {
        ProfileResponse response = userService.updateUsername(userDetails.userId(), request);
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(response));
    }

    @Operation(summary = "Update FCM token for push notifications")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "FCM token updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PatchMapping("/me/fcm-token")
    public ResponseEntity<Void> updateFcmToken(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateFcmTokenRequest request) {
        userService.updateFcmToken(userDetails.userId(), request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Get a user's public profile")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden (User blocked)"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{userId}")
    public ResponseEntity<BaseResponse<PublicProfileResponse>> getPublicProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID userId) {
        PublicProfileResponse response = userService.getPublicProfile(userDetails.userId(), userId);
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(response));
    }

    @Operation(summary = "Block a user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User blocked successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/{userId}/block")
    public ResponseEntity<Void> blockUser(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID userId) {
        userService.blockUser(userDetails.userId(), userId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Unblock a user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User unblocked successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @DeleteMapping("/{userId}/block")
    public ResponseEntity<Void> unblockUser(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID userId) {
        userService.unblockUser(userDetails.userId(), userId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Delete current user account (soft delete)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Account deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal CustomUserDetails userDetails) {
        userService.deleteAccount(userDetails.userId());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Search users by username")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/search")
    public ResponseEntity<BaseResponse<List<PublicProfileResponse>>> searchUsers(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("q") String query,
            @RequestParam(value = "limit", defaultValue = "20") @Min(1) @Max(50) int limit,
            @RequestParam(value = "page", defaultValue = "0") @Min(0) int page) {
        List<PublicProfileResponse> response = userService.searchUsers(userDetails.userId(), query, limit, page);
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(response));
    }
}
