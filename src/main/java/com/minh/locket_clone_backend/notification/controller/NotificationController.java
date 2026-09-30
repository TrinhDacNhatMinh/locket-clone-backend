package com.minh.locket_clone_backend.notification.controller;

import com.minh.locket_clone_backend.auth.security.CustomUserDetails;
import com.minh.locket_clone_backend.common.dto.CursorPagedResponse;
import com.minh.locket_clone_backend.common.response.BaseResponse;
import com.minh.locket_clone_backend.notification.dto.NotificationResponse;
import com.minh.locket_clone_backend.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification", description = "Notification management API")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Get notifications", description = "Get paginated list of notifications for the current user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notifications retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<BaseResponse<CursorPagedResponse<NotificationResponse>>> getNotifications(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        CursorPagedResponse<NotificationResponse> response = notificationService.getNotifications(userDetails.userId(), cursor, limit);
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(response));
    }

    @Operation(summary = "Mark notification as read", description = "Mark a specific notification as read.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notification marked as read successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PatchMapping("/{id}/read")
    public ResponseEntity<BaseResponse<Void>> markAsRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        notificationService.markAsRead(userDetails.userId(), id);
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(null));
    }

    @Operation(summary = "Mark all notifications as read", description = "Mark all notifications of the current user as read.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "All notifications marked as read successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PatchMapping("/read-all")
    public ResponseEntity<BaseResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        notificationService.markAllAsRead(userDetails.userId());
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(null));
    }
}
