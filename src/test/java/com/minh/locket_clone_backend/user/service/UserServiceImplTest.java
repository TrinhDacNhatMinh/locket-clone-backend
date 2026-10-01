package com.minh.locket_clone_backend.user.service;

import com.minh.locket_clone_backend.common.exception.BusinessException;
import com.minh.locket_clone_backend.common.exception.ErrorCode;
import com.minh.locket_clone_backend.friend.service.FriendService;
import com.minh.locket_clone_backend.photo.service.PhotoService;
import com.minh.locket_clone_backend.user.dto.ProfileResponse;
import com.minh.locket_clone_backend.user.dto.UpdateFcmTokenRequest;
import com.minh.locket_clone_backend.user.dto.UpdateProfileRequest;
import com.minh.locket_clone_backend.user.dto.UpdateUsernameRequest;
import com.minh.locket_clone_backend.user.entity.Block;
import com.minh.locket_clone_backend.user.entity.User;
import com.minh.locket_clone_backend.user.repository.BlockRepository;
import com.minh.locket_clone_backend.user.repository.UserRepository;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BlockRepository blockRepository;

    @Mock
    private ObjectProvider<FriendService> friendServiceProvider;

    @Mock
    private ObjectProvider<PhotoService> photoServiceProvider;

    @Mock
    private FriendService friendService;

    @Mock
    private PhotoService photoService;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        lenient().when(friendServiceProvider.getObject()).thenReturn(friendService);
        lenient().when(photoServiceProvider.getObject()).thenReturn(photoService);
        userService = new UserServiceImpl(userRepository, blockRepository, friendServiceProvider, photoServiceProvider);
    }

    private User createMockUser(UUID id) {
        User user = new User();
        user.setId(id);
        user.setUsername("testuser");
        user.setEmail("test@gmail.com");
        return user;
    }

    @Nested
    @DisplayName("updateProfile()")
    class UpdateProfileTests {
        @Test
        void updateProfile_success_updatesAndSavesData() {
            // given
            UUID userId = UUID.randomUUID();
            User user = createMockUser(userId);
            UpdateProfileRequest request = new UpdateProfileRequest("new_name", "new@gmail.com", "+123456", null, "new_url");

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.existsByEmailAndIdNot(request.email(), userId)).thenReturn(false);
            when(userRepository.save(any(User.class))).thenReturn(user);

            // when
            ProfileResponse response = userService.updateProfile(userId, request);

            // then
            assertThat(response).isNotNull();
            verify(userRepository).save(user);
            assertThat(user.getDisplayName()).isEqualTo("new_name");
            assertThat(user.getEmail()).isEqualTo("new@gmail.com");
            assertThat(user.getPhoneNumber()).isEqualTo("+123456");
            assertThat(user.getAvatarUrl()).isEqualTo("new_url");
        }
    }

    @Nested
    @DisplayName("updateUsername()")
    class UpdateUsernameTests {
        @Test
        void updateUsername_duplicateUsername_throwsException() {
            // given
            UUID userId = UUID.randomUUID();
            User user = createMockUser(userId);
            UpdateUsernameRequest request = new UpdateUsernameRequest("taken_username");

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.existsByUsername(request.username())).thenReturn(true);

            // when and then
            BusinessException ex = assertThrows(BusinessException.class, () -> userService.updateUsername(userId, request));
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USERNAME_ALREADY_TAKEN);
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateFcmToken()")
    class UpdateFcmTokenTests {
        @Test
        void updateFcmToken_success_savesToken() {
            // given
            UUID userId = UUID.randomUUID();
            User user = createMockUser(userId);
            UpdateFcmTokenRequest request = new UpdateFcmTokenRequest("new_token");

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            // when
            userService.updateFcmToken(userId, request);

            // then
            verify(userRepository).save(user);
            assertThat(user.getFcmToken()).isEqualTo("new_token");
        }
    }

    @Nested
    @DisplayName("blockUser() & unblockUser() & isBlocked()")
    class BlockTests {
        @Test
        void blockUser_success_savesBlockAndClearsFriendship() {
            // given
            UUID blockerId = UUID.randomUUID();
            UUID blockedId = UUID.randomUUID();
            User blockedUser = createMockUser(blockedId);

            when(userRepository.findById(blockedId)).thenReturn(Optional.of(blockedUser));
            when(blockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)).thenReturn(false);

            // when
            userService.blockUser(blockerId, blockedId);

            // then
            ArgumentCaptor<Block> blockCaptor = ArgumentCaptor.forClass(Block.class);
            verify(blockRepository).save(blockCaptor.capture());
            assertThat(blockCaptor.getValue().getBlockerId()).isEqualTo(blockerId);
            assertThat(blockCaptor.getValue().getBlockedId()).isEqualTo(blockedId);

            verify(friendService).removeFriendshipIfExists(blockerId, blockedId);
            verify(friendService).deleteFriendRequestsBetween(blockerId, blockedId);
        }

        @Test
        void unblockUser_success_deletesBlock() {
            // given
            UUID blockerId = UUID.randomUUID();
            UUID blockedId = UUID.randomUUID();

            when(blockRepository.deleteByBlockerIdAndBlockedId(blockerId, blockedId)).thenReturn(1);

            // when
            userService.unblockUser(blockerId, blockedId);

            // then
            verify(blockRepository).deleteByBlockerIdAndBlockedId(blockerId, blockedId);
        }

        @Test
        void isBlocked_checksBidirectionalBlock() {
            // given
            UUID user1 = UUID.randomUUID();
            UUID user2 = UUID.randomUUID();
            when(blockRepository.existsBidirectional(user1, user2)).thenReturn(true);

            // when
            boolean result = userService.isBlocked(user1, user2);

            // then
            assertThat(result).isTrue();
            verify(blockRepository).existsBidirectional(user1, user2);
        }
    }

    @Nested
    @DisplayName("deleteAccount()")
    class DeleteAccountTests {
        @Test
        void deleteAccount_success_cascadesDeletion() {
            // given
            UUID userId = UUID.randomUUID();
            User user = createMockUser(userId);
            user.setFirebaseUid("fb123");
            user.setPhoneNumber("123");
            user.setFcmToken("token");

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            // when
            userService.deleteAccount(userId);

            // then
            verify(userRepository).save(user);
            assertThat(user.getDeletedAt()).isNotNull();
            assertThat(user.getFirebaseUid()).isNull();
            assertThat(user.getPhoneNumber()).isNull();
            assertThat(user.getEmail()).isNull();
            assertThat(user.getUsername()).isNull();
            assertThat(user.getFcmToken()).isNull();

            verify(photoService).softDeleteAllOwnedBy(userId);
            verify(friendService).deleteAllInvolvingUser(userId);
            verify(blockRepository).deleteAllByUserId(userId);
        }
    }
}
