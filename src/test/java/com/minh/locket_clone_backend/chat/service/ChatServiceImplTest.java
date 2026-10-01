package com.minh.locket_clone_backend.chat.service;

import com.minh.locket_clone_backend.chat.dto.MessageResponse;
import com.minh.locket_clone_backend.chat.entity.Conversation;
import com.minh.locket_clone_backend.chat.entity.ConversationParticipant;
import com.minh.locket_clone_backend.chat.entity.Message;
import com.minh.locket_clone_backend.chat.entity.MessageType;
import com.minh.locket_clone_backend.chat.repository.ConversationParticipantRepository;
import com.minh.locket_clone_backend.chat.repository.ConversationRepository;
import com.minh.locket_clone_backend.chat.repository.MessageRepository;
import com.minh.locket_clone_backend.common.exception.BusinessException;
import com.minh.locket_clone_backend.common.exception.ErrorCode;
import com.minh.locket_clone_backend.notification.entity.NotificationType;
import com.minh.locket_clone_backend.notification.service.NotificationService;
import com.minh.locket_clone_backend.photo.entity.Photo;
import com.minh.locket_clone_backend.photo.service.PhotoService;
import com.minh.locket_clone_backend.user.entity.User;
import com.minh.locket_clone_backend.user.service.UserService;
import com.minh.locket_clone_backend.websocket.dto.RealtimeEventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ConversationParticipantRepository participantRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserService userService;

    @Mock
    private PhotoService photoService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ChatServiceImpl chatService;

    @Nested
    @DisplayName("sendMessage()")
    class SendMessageTests {
        @Test
        void sendMessage_valid_savesAndNotifies() {
            // given
            UUID senderId = UUID.randomUUID();
            UUID recipientId = UUID.randomUUID();
            UUID conversationId = UUID.randomUUID();
            String content = "Hello";

            User recipient = new User();
            recipient.setId(recipientId);
            when(userService.getUserByIdIncludingDeleted(recipientId)).thenReturn(recipient);
            when(userService.isBlocked(senderId, recipientId)).thenReturn(false);

            Conversation conv = new Conversation();
            conv.setId(conversationId);
            when(conversationRepository.findByParticipants(senderId, recipientId)).thenReturn(Optional.of(conv));

            Message savedMessage = Message.builder().type(MessageType.TEXT).content(content).build();
            savedMessage.setId(UUID.randomUUID());
            when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);

            User sender = new User();
            sender.setDisplayName("Alice");
            when(userService.getUserById(senderId)).thenReturn(sender);

            // when
            MessageResponse response = chatService.sendMessage(senderId, recipientId, content);

            // then
            assertThat(response).isNotNull();
            verify(messageRepository).save(any(Message.class));
            verify(notificationService).notify(eq(recipientId), eq(NotificationType.MESSAGE), eq(RealtimeEventType.CHAT_MESSAGE), any(), any());
        }

        @Test
        void sendMessage_recipientDeleted_throwsException() {
            // given
            UUID senderId = UUID.randomUUID();
            UUID recipientId = UUID.randomUUID();
            
            User recipient = new User();
            recipient.setDeletedAt(Instant.now());
            
            when(userService.getUserByIdIncludingDeleted(recipientId)).thenReturn(recipient);

            // when and then
            BusinessException ex = assertThrows(BusinessException.class, () -> chatService.sendMessage(senderId, recipientId, "hi"));
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RECIPIENT_ACCOUNT_DELETED);
        }

        @Test
        void sendMessage_blocked_throwsException() {
            // given
            UUID senderId = UUID.randomUUID();
            UUID recipientId = UUID.randomUUID();
            
            User recipient = new User();
            when(userService.getUserByIdIncludingDeleted(recipientId)).thenReturn(recipient);
            when(userService.isBlocked(senderId, recipientId)).thenReturn(true);

            // when and then
            BusinessException ex = assertThrows(BusinessException.class, () -> chatService.sendMessage(senderId, recipientId, "hi"));
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CONVERSATION_MESSAGE_BLOCKED);
        }
    }

    @Nested
    @DisplayName("getOrCreateConversation()")
    class GetOrCreateConversationTests {
        @Test
        void getOrCreateConversation_exists_returnsId() {
            // given
            UUID userA = UUID.randomUUID();
            UUID userB = UUID.randomUUID();
            Conversation conv = new Conversation();
            conv.setId(UUID.randomUUID());

            when(conversationRepository.findByParticipants(userA, userB)).thenReturn(Optional.of(conv));

            // when
            UUID resultId = chatService.getOrCreateConversation(userA, userB);

            // then
            assertThat(resultId).isEqualTo(conv.getId());
            verify(conversationRepository, never()).save(any());
        }

        @Test
        void getOrCreateConversation_notExists_createsAndReturnsId() {
            // given
            UUID userA = UUID.randomUUID();
            UUID userB = UUID.randomUUID();
            
            when(conversationRepository.findByParticipants(userA, userB)).thenReturn(Optional.empty());
            
            Conversation newConv = new Conversation();
            newConv.setId(UUID.randomUUID());
            when(conversationRepository.save(any(Conversation.class))).thenReturn(newConv);

            // when
            UUID resultId = chatService.getOrCreateConversation(userA, userB);

            // then
            assertThat(resultId).isEqualTo(newConv.getId());
            verify(conversationRepository).save(any(Conversation.class));
            verify(participantRepository, times(2)).save(any(ConversationParticipant.class));
        }
    }

    @Nested
    @DisplayName("createPhotoComment()")
    class CreatePhotoCommentTests {
        @Test
        void createPhotoComment_valid_savesAndNotifies() {
            // given
            UUID commenterId = UUID.randomUUID();
            UUID photoId = UUID.randomUUID();
            UUID photoOwnerId = UUID.randomUUID();
            
            Photo photo = Photo.builder().ownerId(photoOwnerId).build();
            photo.setId(photoId);
            when(photoService.getPhotoIfAllowed(commenterId, photoId)).thenReturn(photo);

            User recipient = new User();
            when(userService.getUserByIdIncludingDeleted(photoOwnerId)).thenReturn(recipient);
            when(userService.isBlocked(commenterId, photoOwnerId)).thenReturn(false);

            Conversation conv = new Conversation();
            conv.setId(UUID.randomUUID());
            when(conversationRepository.findByParticipants(commenterId, photoOwnerId)).thenReturn(Optional.of(conv));

            Message savedMessage = Message.builder().type(MessageType.PHOTO_COMMENT).build();
            savedMessage.setId(UUID.randomUUID());
            when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);

            User commenter = new User();
            commenter.setDisplayName("Bob");
            when(userService.getUserById(commenterId)).thenReturn(commenter);

            // when
            MessageResponse response = chatService.createPhotoComment(commenterId, photoId, "Nice pic");

            // then
            assertThat(response).isNotNull();
            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
            verify(messageRepository).save(messageCaptor.capture());
            assertThat(messageCaptor.getValue().getType()).isEqualTo(MessageType.PHOTO_COMMENT);
            assertThat(messageCaptor.getValue().getReferencePhotoId()).isEqualTo(photoId);

            verify(notificationService).notify(eq(photoOwnerId), eq(NotificationType.COMMENT), eq(RealtimeEventType.CHAT_MESSAGE), any(), any());
        }

        @Test
        void createPhotoComment_ownPhoto_throwsException() {
            // given
            UUID commenterId = UUID.randomUUID();
            UUID photoId = UUID.randomUUID();
            
            Photo photo = Photo.builder().ownerId(commenterId).build();
            photo.setId(photoId);
            when(photoService.getPhotoIfAllowed(commenterId, photoId)).thenReturn(photo);

            // when and then
            BusinessException ex = assertThrows(BusinessException.class, () -> chatService.createPhotoComment(commenterId, photoId, "Nice"));
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
        }
    }
}
