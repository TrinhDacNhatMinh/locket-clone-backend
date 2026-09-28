package com.minh.locket_clone_backend.chat.controller;

import com.minh.locket_clone_backend.auth.security.CustomUserDetails;
import com.minh.locket_clone_backend.chat.dto.CommentRequest;
import com.minh.locket_clone_backend.chat.dto.MessageResponse;
import com.minh.locket_clone_backend.chat.service.ChatService;
import com.minh.locket_clone_backend.common.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/photos")
@RequiredArgsConstructor
@Tag(name = "Comment", description = "Photo comment API")
public class PhotoCommentController {

    private final ChatService chatService;

    @Operation(summary = "Comment on a photo", description = "Sends a comment on a photo as a PHOTO_COMMENT message to the owner's conversation.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Comment sent successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "No access to this photo, or blocked by owner"),
            @ApiResponse(responseCode = "404", description = "Photo not found")
    })
    @PostMapping("/{photoId}/comments")
    public ResponseEntity<BaseResponse<MessageResponse>> commentOnPhoto(
            @PathVariable UUID photoId,
            @Valid @RequestBody CommentRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        MessageResponse response = chatService.createPhotoComment(userDetails.userId(), photoId, request.content());
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(response));
    }
}
