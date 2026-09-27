package com.minh.locket_clone_backend.reaction.controller;

import com.minh.locket_clone_backend.auth.security.CustomUserDetails;
import com.minh.locket_clone_backend.common.response.BaseResponse;
import com.minh.locket_clone_backend.reaction.dto.ReactionRequest;
import com.minh.locket_clone_backend.reaction.dto.ReactionResponse;
import com.minh.locket_clone_backend.reaction.service.ReactionService;
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
@RequestMapping("/api/v1/photos/{photoId}/reactions")
@RequiredArgsConstructor
@Tag(name = "Reaction", description = "Reaction API")
public class ReactionController {

    private final ReactionService reactionService;

    @Operation(summary = "React to a photo", description = "Add an emoji reaction to a photo you have access to.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully reacted"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Access denied to this photo"),
            @ApiResponse(responseCode = "404", description = "Photo not found")
    })
    @PostMapping
    public ResponseEntity<BaseResponse<Void>> reactToPhoto(
            @PathVariable UUID photoId,
            @Valid @RequestBody ReactionRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        reactionService.react(userDetails.userId(), photoId, request);
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(null));
    }

    @Operation(summary = "View unseen reactions", description = "Retrieve all unseen reactions for a photo you own. Marks them as seen automatically.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved reactions"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Not the owner of the photo"),
            @ApiResponse(responseCode = "404", description = "Photo not found")
    })
    @PostMapping("/view")
    public ResponseEntity<BaseResponse<List<ReactionResponse>>> viewUnseenReactions(
            @PathVariable UUID photoId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        List<ReactionResponse> reactions = reactionService.getUnseenReactions(userDetails.userId(), photoId);
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(reactions));
    }
}
