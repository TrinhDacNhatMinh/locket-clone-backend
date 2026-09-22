package com.minh.locket_clone_backend.feed.controller;

import com.minh.locket_clone_backend.auth.security.CustomUserDetails;
import com.minh.locket_clone_backend.common.dto.CursorPagedResponse;
import com.minh.locket_clone_backend.feed.service.FeedService;
import com.minh.locket_clone_backend.photo.dto.PhotoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/feed")
@RequiredArgsConstructor
@Validated
@Tag(name = "Feed", description = "Feed retrieval endpoints")
public class FeedController {

    private final FeedService feedService;

    @Operation(summary = "Get user feed", description = "Retrieves a paginated list of photos from all friends.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved feed"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<CursorPagedResponse<PhotoResponse>> getFeed(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "limit", defaultValue = "10") @Min(1) @Max(50) int limit
    ) {
        CursorPagedResponse<PhotoResponse> response = feedService.getFeed(userDetails.userId(), cursor, limit);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get specific friend's feed", description = "Retrieves a paginated list of photos from a specific friend.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved friend's feed"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not friends with this user"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/friends/{friendId}")
    public ResponseEntity<CursorPagedResponse<PhotoResponse>> getFriendFeed(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID friendId,
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "limit", defaultValue = "10") @Min(1) @Max(50) int limit
    ) {
        CursorPagedResponse<PhotoResponse> response = feedService.getFriendPhotoHistory(userDetails.userId(), friendId, cursor, limit);
        return ResponseEntity.ok(response);
    }
}
