package com.minh.locket_clone_backend.chat.controller;

import com.minh.locket_clone_backend.auth.security.CustomUserDetails;
import com.minh.locket_clone_backend.chat.dto.ConversationIdResponse;
import com.minh.locket_clone_backend.chat.dto.ConversationSummaryResponse;
import com.minh.locket_clone_backend.chat.dto.MessageResponse;
import com.minh.locket_clone_backend.chat.dto.SendMessageRequest;
import com.minh.locket_clone_backend.chat.service.ChatService;
import com.minh.locket_clone_backend.common.dto.CursorPagedResponse;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
@Tag(name = "Conversation", description = "Chat and messaging API")
public class ConversationController {

    private final ChatService chatService;

    @Operation(summary = "List all conversations", description = "Returns all conversations for the authenticated user, with the last message preview.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved conversations"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<BaseResponse<List<ConversationSummaryResponse>>> getConversations(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<ConversationSummaryResponse> conversations = chatService.getConversations(userDetails.userId());
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(conversations));
    }

    @Operation(summary = "Get or create conversation", description = "Returns the conversation with a specific user, creating it if it doesn't exist yet.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved conversation"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{userId}")
    public ResponseEntity<BaseResponse<ConversationIdResponse>> getOrCreateConversation(
            @PathVariable UUID userId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        UUID conversationId = chatService.getOrCreateConversation(userDetails.userId(), userId);
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(new ConversationIdResponse(conversationId)));
    }

    @Operation(summary = "Send a message", description = "Sends a text message to another user. Returns 403 if blocked or if the recipient account was deleted.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Message sent successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Blocked or recipient account deleted"),
            @ApiResponse(responseCode = "404", description = "Recipient user not found")
    })
    @PostMapping("/{userId}/messages")
    public ResponseEntity<BaseResponse<MessageResponse>> sendMessage(
            @PathVariable UUID userId,
            @Valid @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        MessageResponse response = chatService.sendMessage(userDetails.userId(), userId, request.content());
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(response));
    }

    @Operation(summary = "Get message history", description = "Returns paginated message history for a conversation with a specific user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved messages"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Not a participant of this conversation"),
            @ApiResponse(responseCode = "404", description = "Conversation not found")
    })
    @GetMapping("/{userId}/messages")
    public ResponseEntity<BaseResponse<CursorPagedResponse<MessageResponse>>> getMessageHistory(
            @PathVariable UUID userId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        CursorPagedResponse<MessageResponse> history =
                chatService.getMessageHistory(userDetails.userId(), userId, cursor, limit);
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(history));
    }
}
