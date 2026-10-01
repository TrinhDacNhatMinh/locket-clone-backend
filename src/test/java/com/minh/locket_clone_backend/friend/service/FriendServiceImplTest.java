package com.minh.locket_clone_backend.friend.service;

import com.minh.locket_clone_backend.common.exception.BusinessException;
import com.minh.locket_clone_backend.common.exception.ErrorCode;
import com.minh.locket_clone_backend.friend.config.FriendProperties;
import com.minh.locket_clone_backend.friend.dto.FriendRequestAction;
import com.minh.locket_clone_backend.friend.dto.RespondFriendRequestRequest;
import com.minh.locket_clone_backend.friend.dto.SendFriendRequestRequest;
import com.minh.locket_clone_backend.friend.entity.Friend;
import com.minh.locket_clone_backend.friend.entity.FriendRequest;
import com.minh.locket_clone_backend.friend.entity.FriendRequestStatus;
import com.minh.locket_clone_backend.friend.repository.FriendRepository;
import com.minh.locket_clone_backend.friend.repository.FriendRequestRepository;
import com.minh.locket_clone_backend.notification.entity.NotificationType;
import com.minh.locket_clone_backend.notification.service.NotificationService;
import com.minh.locket_clone_backend.user.entity.User;
import com.minh.locket_clone_backend.user.service.UserService;
import com.minh.locket_clone_backend.websocket.dto.RealtimeEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendServiceImplTest {

    @Mock
    private FriendRequestRepository friendRequestRepository;

    @Mock
    private FriendRepository friendRepository;

    @Mock
    private ObjectProvider<UserService> userServiceProvider;

    @Mock
    private UserService userService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private FriendProperties friendProperties;

    @InjectMocks
    private FriendServiceImpl friendService;

    @BeforeEach
    void setUp() {
        lenient().when(userServiceProvider.getObject()).thenReturn(userService);
    }

    private User createMockUser(UUID id, String name) {
        User user = new User();
        user.setId(id);
        user.setDisplayName(name);
        return user;
    }

    @Nested
    @DisplayName("sendFriendRequest()")
    class SendFriendRequestTests {
        @Test
        void sendFriendRequest_valid_createsPendingRequest() {
            // given
            UUID requesterId = UUID.randomUUID();
            UUID addresseeId = UUID.randomUUID();
            SendFriendRequestRequest request = new SendFriendRequestRequest(addresseeId);

            when(userService.isBlocked(requesterId, addresseeId)).thenReturn(false);
            when(friendRepository.existsByUserAIdAndUserBId(any(UUID.class), any(UUID.class))).thenReturn(false);
            when(friendProperties.getMaxLimit()).thenReturn(20);
            when(friendRepository.countByUserId(requesterId)).thenReturn(5L);

            when(friendRequestRepository.existsByRequesterIdAndAddresseeIdAndStatus(requesterId, addresseeId, FriendRequestStatus.PENDING)).thenReturn(false);
            when(friendRequestRepository.existsByRequesterIdAndAddresseeIdAndStatus(requesterId, addresseeId, FriendRequestStatus.REJECTED)).thenReturn(false);
            when(friendRequestRepository.findByRequesterIdAndAddresseeIdAndStatus(addresseeId, requesterId, FriendRequestStatus.PENDING)).thenReturn(Optional.empty());

            User requester = createMockUser(requesterId, "Requester");
            when(userService.getUserById(requesterId)).thenReturn(requester);

            // when
            friendService.sendFriendRequest(requesterId, request);

            // then
            ArgumentCaptor<FriendRequest> requestCaptor = ArgumentCaptor.forClass(FriendRequest.class);
            verify(friendRequestRepository).save(requestCaptor.capture());
            assertThat(requestCaptor.getValue().getStatus()).isEqualTo(FriendRequestStatus.PENDING);

            verify(notificationService).notify(eq(addresseeId), eq(NotificationType.FRIEND_REQUEST), eq(RealtimeEventType.NOTIFICATION), any(), any());
        }

        @Test
        void sendFriendRequest_reverseRequestExists_autoAccepts() {
            // given
            UUID requesterId = UUID.randomUUID();
            UUID addresseeId = UUID.randomUUID();
            SendFriendRequestRequest request = new SendFriendRequestRequest(addresseeId);

            when(userService.isBlocked(requesterId, addresseeId)).thenReturn(false);
            when(friendRepository.existsByUserAIdAndUserBId(any(UUID.class), any(UUID.class))).thenReturn(false);
            when(friendProperties.getMaxLimit()).thenReturn(20);
            when(friendRepository.countByUserId(requesterId)).thenReturn(5L);

            when(friendRequestRepository.existsByRequesterIdAndAddresseeIdAndStatus(requesterId, addresseeId, FriendRequestStatus.PENDING)).thenReturn(false);
            when(friendRequestRepository.existsByRequesterIdAndAddresseeIdAndStatus(requesterId, addresseeId, FriendRequestStatus.REJECTED)).thenReturn(false);

            FriendRequest reverseReq = new FriendRequest();
            reverseReq.setStatus(FriendRequestStatus.PENDING);
            when(friendRequestRepository.findByRequesterIdAndAddresseeIdAndStatus(addresseeId, requesterId, FriendRequestStatus.PENDING))
                    .thenReturn(Optional.of(reverseReq));

            User addressee = createMockUser(addresseeId, "Addressee");
            when(userService.getUserById(addresseeId)).thenReturn(addressee);

            // when
            friendService.sendFriendRequest(requesterId, request);

            // then
            assertThat(reverseReq.getStatus()).isEqualTo(FriendRequestStatus.ACCEPTED);
            verify(friendRequestRepository).save(reverseReq);
            verify(friendRepository).save(any(Friend.class));
            verify(notificationService).notify(eq(requesterId), eq(NotificationType.FRIEND_ACCEPTED), eq(RealtimeEventType.NOTIFICATION), any(), any());
        }

        @Test
        void sendFriendRequest_overLimit_throwsException() {
            // given
            UUID requesterId = UUID.randomUUID();
            UUID addresseeId = UUID.randomUUID();
            SendFriendRequestRequest request = new SendFriendRequestRequest(addresseeId);

            when(userService.isBlocked(requesterId, addresseeId)).thenReturn(false);
            when(friendRepository.existsByUserAIdAndUserBId(any(UUID.class), any(UUID.class))).thenReturn(false);
            when(friendProperties.getMaxLimit()).thenReturn(20);
            when(friendRepository.countByUserId(requesterId)).thenReturn(20L);

            // when and then
            BusinessException ex = assertThrows(BusinessException.class, () -> friendService.sendFriendRequest(requesterId, request));
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FRIEND_LIMIT_EXCEEDED);
        }

        @Test
        void sendFriendRequest_recentlyRejected_throwsException() {
            // given
            UUID requesterId = UUID.randomUUID();
            UUID addresseeId = UUID.randomUUID();
            SendFriendRequestRequest request = new SendFriendRequestRequest(addresseeId);

            when(userService.isBlocked(requesterId, addresseeId)).thenReturn(false);
            when(friendRepository.existsByUserAIdAndUserBId(any(UUID.class), any(UUID.class))).thenReturn(false);
            when(friendProperties.getMaxLimit()).thenReturn(20);
            when(friendRepository.countByUserId(requesterId)).thenReturn(5L);

            when(friendRequestRepository.existsByRequesterIdAndAddresseeIdAndStatus(requesterId, addresseeId, FriendRequestStatus.PENDING)).thenReturn(false);
            when(friendRequestRepository.existsByRequesterIdAndAddresseeIdAndStatus(requesterId, addresseeId, FriendRequestStatus.REJECTED)).thenReturn(true);

            // when and then
            BusinessException ex = assertThrows(BusinessException.class, () -> friendService.sendFriendRequest(requesterId, request));
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RATE_LIMIT_EXCEEDED);
        }

        @Test
        void sendFriendRequest_alreadyPending_throwsException() {
            // given
            UUID requesterId = UUID.randomUUID();
            UUID addresseeId = UUID.randomUUID();
            SendFriendRequestRequest request = new SendFriendRequestRequest(addresseeId);

            when(userService.isBlocked(requesterId, addresseeId)).thenReturn(false);
            when(friendRepository.existsByUserAIdAndUserBId(any(UUID.class), any(UUID.class))).thenReturn(false);
            when(friendProperties.getMaxLimit()).thenReturn(20);
            when(friendRepository.countByUserId(requesterId)).thenReturn(5L);

            when(friendRequestRepository.existsByRequesterIdAndAddresseeIdAndStatus(requesterId, addresseeId, FriendRequestStatus.PENDING)).thenReturn(true);

            // when and then
            BusinessException ex = assertThrows(BusinessException.class, () -> friendService.sendFriendRequest(requesterId, request));
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FRIEND_REQUEST_ALREADY_EXISTS);
        }
    }

    @Nested
    @DisplayName("respondFriendRequest()")
    class RespondFriendRequestTests {
        @Test
        void respondFriendRequest_accept_createsFriendship() {
            // given
            UUID responderId = UUID.randomUUID();
            UUID requestId = UUID.randomUUID();
            UUID requesterId = UUID.randomUUID();
            RespondFriendRequestRequest request = new RespondFriendRequestRequest(FriendRequestAction.ACCEPT);

            FriendRequest req = new FriendRequest();
            req.setAddresseeId(responderId);
            req.setRequesterId(requesterId);
            req.setStatus(FriendRequestStatus.PENDING);

            when(friendRequestRepository.findById(requestId)).thenReturn(Optional.of(req));
            when(friendProperties.getMaxLimit()).thenReturn(20);
            when(friendRepository.countByUserId(any(UUID.class))).thenReturn(5L);
            when(friendRepository.existsByUserAIdAndUserBId(any(UUID.class), any(UUID.class))).thenReturn(false);

            User responder = createMockUser(responderId, "Responder");
            when(userService.getUserById(responderId)).thenReturn(responder);

            // when
            friendService.respondFriendRequest(responderId, requestId, request);

            // then
            assertThat(req.getStatus()).isEqualTo(FriendRequestStatus.ACCEPTED);
            verify(friendRepository).save(any(Friend.class));
            verify(notificationService).notify(eq(requesterId), eq(NotificationType.FRIEND_ACCEPTED), eq(RealtimeEventType.NOTIFICATION), any(), any());
        }

        @Test
        void respondFriendRequest_reject_updatesStatusToRejected() {
            // given
            UUID responderId = UUID.randomUUID();
            UUID requestId = UUID.randomUUID();
            RespondFriendRequestRequest request = new RespondFriendRequestRequest(FriendRequestAction.REJECT);

            FriendRequest req = new FriendRequest();
            req.setAddresseeId(responderId);
            req.setStatus(FriendRequestStatus.PENDING);

            when(friendRequestRepository.findById(requestId)).thenReturn(Optional.of(req));

            // when
            friendService.respondFriendRequest(responderId, requestId, request);

            // then
            assertThat(req.getStatus()).isEqualTo(FriendRequestStatus.REJECTED);
            verify(friendRequestRepository).save(req);
            verify(friendRepository, never()).save(any());
        }
    }
}
