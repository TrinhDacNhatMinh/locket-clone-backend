package com.minh.locket_clone_backend.reaction.service;

import com.minh.locket_clone_backend.common.exception.BusinessException;
import com.minh.locket_clone_backend.common.exception.ErrorCode;
import com.minh.locket_clone_backend.notification.entity.NotificationType;
import com.minh.locket_clone_backend.notification.service.NotificationService;
import com.minh.locket_clone_backend.photo.entity.Photo;
import com.minh.locket_clone_backend.photo.service.PhotoService;
import com.minh.locket_clone_backend.reaction.dto.ReactionRequest;
import com.minh.locket_clone_backend.reaction.dto.ReactionResponse;
import com.minh.locket_clone_backend.reaction.entity.Reaction;
import com.minh.locket_clone_backend.reaction.repository.ReactionRepository;
import com.minh.locket_clone_backend.user.entity.User;
import com.minh.locket_clone_backend.user.service.UserService;
import com.minh.locket_clone_backend.websocket.SessionRegistry;
import com.minh.locket_clone_backend.websocket.dto.RealtimeEventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReactionServiceImplTest {

    @Mock
    private ReactionRepository reactionRepository;

    @Mock
    private PhotoService photoService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserService userService;

    @Mock
    private SessionRegistry sessionRegistry;

    @InjectMocks
    private ReactionServiceImpl reactionService;

    @Nested
    @DisplayName("react()")
    class ReactTests {
        @Test
        void react_validAndOffline_savesUnseen() {
            // given
            UUID reactorId = UUID.randomUUID();
            UUID photoId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();
            ReactionRequest request = new ReactionRequest("👍");

            Photo photo = Photo.builder().ownerId(ownerId).build();
            photo.setId(photoId);
            when(photoService.getPhotoIfAllowed(reactorId, photoId)).thenReturn(photo);

            Reaction savedReaction = Reaction.builder().photoId(photoId).userId(reactorId).emoji("👍").seen(false).build();
            savedReaction.setId(UUID.randomUUID());
            when(reactionRepository.save(any(Reaction.class))).thenReturn(savedReaction);

            User reactor = new User();
            reactor.setDisplayName("Reactor");
            when(userService.getUserById(reactorId)).thenReturn(reactor);

            when(sessionRegistry.isOnline(ownerId)).thenReturn(false);

            // when
            reactionService.react(reactorId, photoId, request);

            // then
            verify(reactionRepository, times(1)).save(any(Reaction.class));
            verify(notificationService).notify(eq(ownerId), eq(NotificationType.REACTION), eq(RealtimeEventType.REACTION), any(), any());
        }

        @Test
        void react_validAndOnline_savesAndMarksSeen() {
            // given
            UUID reactorId = UUID.randomUUID();
            UUID photoId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();
            ReactionRequest request = new ReactionRequest("🔥");

            Photo photo = Photo.builder().ownerId(ownerId).build();
            photo.setId(photoId);
            when(photoService.getPhotoIfAllowed(reactorId, photoId)).thenReturn(photo);

            Reaction savedReaction = Reaction.builder().photoId(photoId).userId(reactorId).emoji("🔥").seen(false).build();
            savedReaction.setId(UUID.randomUUID());
            when(reactionRepository.save(any(Reaction.class))).thenReturn(savedReaction);

            User reactor = new User();
            reactor.setDisplayName("Reactor");
            when(userService.getUserById(reactorId)).thenReturn(reactor);

            when(sessionRegistry.isOnline(ownerId)).thenReturn(true);

            // when
            reactionService.react(reactorId, photoId, request);

            // then
            ArgumentCaptor<Reaction> reactionCaptor = ArgumentCaptor.forClass(Reaction.class);
            // First save (initial), Second save (setSeen=true)
            verify(reactionRepository, times(2)).save(reactionCaptor.capture());
            assertThat(reactionCaptor.getAllValues().get(1).isSeen()).isTrue();
        }
    }

    @Nested
    @DisplayName("getUnseenReactions()")
    class GetUnseenReactionsTests {
        @Test
        void getUnseenReactions_notOwner_throwsException() {
            // given
            UUID notOwnerId = UUID.randomUUID();
            UUID photoId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();

            Photo photo = Photo.builder().ownerId(ownerId).build();
            photo.setId(photoId);
            when(photoService.getPhotoIfAllowed(notOwnerId, photoId)).thenReturn(photo);

            // when and then
            BusinessException ex = assertThrows(BusinessException.class, () -> reactionService.getUnseenReactions(notOwnerId, photoId));
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PHOTO_NOT_OWNER);
        }

        @Test
        void getUnseenReactions_hasUnseen_marksAsSeenAndReturns() {
            // given
            UUID ownerId = UUID.randomUUID();
            UUID photoId = UUID.randomUUID();

            Photo photo = Photo.builder().ownerId(ownerId).build();
            photo.setId(photoId);
            when(photoService.getPhotoIfAllowed(ownerId, photoId)).thenReturn(photo);

            Reaction r1 = Reaction.builder().seen(false).build();
            r1.setId(UUID.randomUUID());
            Reaction r2 = Reaction.builder().seen(false).build();
            r2.setId(UUID.randomUUID());
            when(reactionRepository.findByPhotoIdAndSeenFalse(photoId)).thenReturn(List.of(r1, r2));

            // when
            List<ReactionResponse> responses = reactionService.getUnseenReactions(ownerId, photoId);

            // then
            assertThat(responses).hasSize(2);
            verify(reactionRepository).saveAll(any());
            assertThat(r1.isSeen()).isTrue();
            assertThat(r2.isSeen()).isTrue();
        }
    }
}
