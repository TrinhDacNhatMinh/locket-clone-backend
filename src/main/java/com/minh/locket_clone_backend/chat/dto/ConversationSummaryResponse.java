package com.minh.locket_clone_backend.chat.dto;

import com.minh.locket_clone_backend.chat.entity.Conversation;

import java.time.Instant;
import java.util.UUID;

public record ConversationSummaryResponse(
        UUID conversationId,
        UUID otherUserId,
        String otherUserDisplayName,
        String otherUserAvatarUrl,
        String lastMessageContent,
        Instant lastMessageAt
) {
    public static ConversationSummaryResponse from(
            Conversation conversation,
            UUID otherUserId,
            String otherUserDisplayName,
            String otherUserAvatarUrl,
            String lastMessageContent,
            Instant lastMessageAt) {
        return new ConversationSummaryResponse(
                conversation.getId(),
                otherUserId,
                otherUserDisplayName,
                otherUserAvatarUrl,
                lastMessageContent,
                lastMessageAt
        );
    }
}
