package com.minh.locket_clone_backend.chat.dto;

import com.minh.locket_clone_backend.chat.entity.Message;
import com.minh.locket_clone_backend.chat.entity.MessageType;
import com.minh.locket_clone_backend.user.entity.User;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID conversationId,
        UUID senderId,
        String senderDisplayName,
        MessageType type,
        String content,
        UUID referencePhotoId,
        Instant createdAt
) {
    /**
     * Maps a Message to a response, using the sender's display name.
     * Handles the case where the sender has deleted their account by showing "Deleted User".
     */
    public static MessageResponse from(Message message, User sender) {
        String displayName = sender.getDeletedAt() != null ? "Deleted User" : sender.getDisplayName();
        return new MessageResponse(
                message.getId(),
                message.getConversationId(),
                message.getSenderId(),
                displayName,
                message.getType(),
                message.getContent(),
                message.getReferencePhotoId(),
                message.getCreatedAt()
        );
    }
}
