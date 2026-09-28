package com.minh.locket_clone_backend.chat.service;

import com.minh.locket_clone_backend.chat.dto.ConversationSummaryResponse;
import com.minh.locket_clone_backend.chat.dto.MessageResponse;
import com.minh.locket_clone_backend.common.dto.CursorPagedResponse;

import java.util.List;
import java.util.UUID;

public interface ChatService {

    MessageResponse sendMessage(UUID senderId, UUID recipientId, String content);

    UUID getOrCreateConversation(UUID userA, UUID userB);

    CursorPagedResponse<MessageResponse> getMessageHistory(UUID requesterId, UUID otherUserId, String cursor, int limit);

    List<ConversationSummaryResponse> getConversations(UUID userId);

    MessageResponse createPhotoComment(UUID commenterId, UUID photoId, String content);
}
