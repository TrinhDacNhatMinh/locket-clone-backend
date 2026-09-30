package com.minh.locket_clone_backend.chat.service;

import com.minh.locket_clone_backend.chat.dto.ConversationSummaryResponse;
import com.minh.locket_clone_backend.chat.dto.MessageResponse;
import com.minh.locket_clone_backend.chat.entity.Conversation;
import com.minh.locket_clone_backend.chat.entity.ConversationParticipant;
import com.minh.locket_clone_backend.chat.entity.Message;
import com.minh.locket_clone_backend.chat.entity.MessageType;
import com.minh.locket_clone_backend.chat.repository.ConversationParticipantRepository;
import com.minh.locket_clone_backend.chat.repository.ConversationRepository;
import com.minh.locket_clone_backend.chat.repository.MessageRepository;
import com.minh.locket_clone_backend.common.dto.CursorPagedResponse;
import com.minh.locket_clone_backend.common.exception.BusinessException;
import com.minh.locket_clone_backend.common.exception.ErrorCode;
import com.minh.locket_clone_backend.common.utils.CursorPaginationHelper;
import com.minh.locket_clone_backend.notification.entity.NotificationType;
import com.minh.locket_clone_backend.notification.service.NotificationService;
import com.minh.locket_clone_backend.photo.entity.Photo;
import com.minh.locket_clone_backend.photo.service.PhotoService;
import com.minh.locket_clone_backend.user.entity.User;
import com.minh.locket_clone_backend.user.service.UserService;
import com.minh.locket_clone_backend.websocket.dto.RealtimeEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final MessageRepository messageRepository;
    private final UserService userService;
    private final PhotoService photoService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public MessageResponse sendMessage(UUID senderId, UUID recipientId, String content) {
        if (senderId.equals(recipientId)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Cannot send message to yourself");
        }

        Message message = createAndSaveMessage(senderId, recipientId, MessageType.TEXT, content, null);

        // Deliver the message to the recipient
        User sender = userService.getUserById(senderId);
        MessageResponse response = MessageResponse.from(message, sender);

        notificationService.notify(
                recipientId,
                NotificationType.MESSAGE,
                RealtimeEventType.CHAT_MESSAGE,
                Map.of(
                        "actorDisplayName", sender.getDisplayName(),
                        "messageId", message.getId().toString()
                ),
                response
        );

        return response;
    }

    @Override
    @Transactional
    public UUID getOrCreateConversation(UUID userA, UUID userB) {
        if (userA.equals(userB)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Cannot create conversation with yourself");
        }

        return conversationRepository.findByParticipants(userA, userB)
                .map(Conversation::getId)
                .orElseGet(() -> {
                    Conversation conversation = conversationRepository.save(new Conversation());
                    participantRepository.save(ConversationParticipant.builder()
                            .conversationId(conversation.getId())
                            .userId(userA)
                            .build());
                    participantRepository.save(ConversationParticipant.builder()
                            .conversationId(conversation.getId())
                            .userId(userB)
                            .build());
                    log.info("Created new conversation {} between {} and {}", conversation.getId(), userA, userB);
                    return conversation.getId();
                });
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPagedResponse<MessageResponse> getMessageHistory(
            UUID requesterId, UUID otherUserId, String cursor, int limit) {

        // Get conversation (participants check implicitly enforced by using otherUserId)
        UUID conversationId = conversationRepository.findByParticipants(requesterId, otherUserId)
                .map(Conversation::getId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        // Verify requester is a participant
        if (!participantRepository.existsByConversationIdAndUserId(conversationId, requesterId)) {
            throw new BusinessException(ErrorCode.PHOTO_ACCESS_DENIED);
        }

        // Fetch paginated messages
        Pageable pageable = PageRequest.of(0, limit + 1);
        List<Message> messages;

        if (cursor == null || cursor.isBlank()) {
            messages = messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable);
        } else {
            CursorPaginationHelper.Cursor decoded = CursorPaginationHelper.decodeCursor(cursor);
            messages = messageRepository.findByConversationIdNextPage(
                    conversationId, decoded.createdAt(), decoded.id(), pageable);
        }

        // Map messages, resolving sender display name (supporting deleted users)
        boolean hasMore = messages.size() > limit;
        List<Message> page = hasMore ? messages.subList(0, limit) : messages;

        List<MessageResponse> responses = new ArrayList<>();
        for (Message message : page) {
            User sender;
            try {
                sender = userService.getUserByIdIncludingDeleted(message.getSenderId());
            } catch (BusinessException ex) {
                sender = buildUnknownUserPlaceholder(message.getSenderId());
            }
            responses.add(MessageResponse.from(message, sender));
        }

        String nextCursor = null;
        if (hasMore && !page.isEmpty()) {
            Message last = page.get(page.size() - 1);
            nextCursor = CursorPaginationHelper.encodeCursor(last.getCreatedAt(), last.getId());
        }

        return new CursorPagedResponse<>(responses, nextCursor, hasMore);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationSummaryResponse> getConversations(UUID userId) {
        List<ConversationParticipant> participants = participantRepository.findByUserId(userId);
        List<ConversationSummaryResponse> summaries = new ArrayList<>();

        for (ConversationParticipant participant : participants) {
            UUID conversationId = participant.getConversationId();

            // Find the other participant
            UUID otherUserId = participantRepository.findByConversationId(conversationId)
                    .stream()
                    .map(ConversationParticipant::getUserId)
                    .filter(id -> !id.equals(userId))
                    .findFirst()
                    .orElse(null);

            if (otherUserId == null) continue;

            User otherUser;
            try {
                otherUser = userService.getUserByIdIncludingDeleted(otherUserId);
            } catch (BusinessException ex) {
                otherUser = buildUnknownUserPlaceholder(otherUserId);
            }

            String otherDisplayName = otherUser.getDeletedAt() != null ? "Deleted User" : otherUser.getDisplayName();

            // Fetch last message
            Pageable one = PageRequest.of(0, 1);
            List<Message> lastMessages = messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, one);

            String lastContent = lastMessages.isEmpty() ? null : lastMessages.get(0).getContent();
            var lastAt = lastMessages.isEmpty() ? null : lastMessages.get(0).getCreatedAt();

            Conversation conversation = conversationRepository.findById(conversationId).orElse(null);
            if (conversation == null) continue;

            summaries.add(ConversationSummaryResponse.from(
                    conversation, otherUserId, otherDisplayName,
                    otherUser.getAvatarUrl(), lastContent, lastAt));
        }

        return summaries;
    }

    @Override
    @Transactional
    public MessageResponse createPhotoComment(UUID commenterId, UUID photoId, String content) {
        // Check commenter can view the photo (access control)
        Photo photo = photoService.getPhotoIfAllowed(commenterId, photoId);
        UUID photoOwnerId = photo.getOwnerId();

        if (commenterId.equals(photoOwnerId)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Cannot comment on your own photo");
        }

        Message message = createAndSaveMessage(commenterId, photoOwnerId, MessageType.PHOTO_COMMENT, content, photoId);

        User sender = userService.getUserById(commenterId);
        MessageResponse response = MessageResponse.from(message, sender);

        notificationService.notify(
                photoOwnerId,
                NotificationType.COMMENT,
                RealtimeEventType.CHAT_MESSAGE,
                Map.of(
                        "actorDisplayName", sender.getDisplayName(),
                        "messageId", message.getId().toString(),
                        "photoId", photoId.toString()
                ),
                response
        );

        return response;
    }

    private Message createAndSaveMessage(UUID senderId, UUID recipientId, MessageType type, String content, UUID referencePhotoId) {
        User recipient = userService.getUserByIdIncludingDeleted(recipientId);
        if (recipient.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.RECIPIENT_ACCOUNT_DELETED);
        }

        if (userService.isBlocked(senderId, recipientId)) {
            throw new BusinessException(ErrorCode.CONVERSATION_MESSAGE_BLOCKED);
        }

        UUID conversationId = getOrCreateConversation(senderId, recipientId);
        Message message = messageRepository.save(Message.builder()
                .conversationId(conversationId)
                .senderId(senderId)
                .type(type)
                .content(content)
                .referencePhotoId(referencePhotoId)
                .build());

        log.info("Message {} saved in conversation {} from sender {}", message.getId(), conversationId, senderId);
        return message;
    }

    private User buildUnknownUserPlaceholder(UUID userId) {
        User user = new User();
        user.setId(userId);
        user.setDisplayName("Deleted User");
        user.setDeletedAt(Instant.now());
        return user;
    }
}
