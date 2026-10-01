package com.minh.locket_clone_backend.auth.service;

import com.google.firebase.auth.FirebaseToken;
import com.minh.locket_clone_backend.auth.dto.AuthResponse;
import com.minh.locket_clone_backend.user.dto.SyncUserRequest;
import com.minh.locket_clone_backend.user.dto.SyncUserResponse;
import com.minh.locket_clone_backend.user.dto.UserResponse;
import com.minh.locket_clone_backend.user.entity.AuthProvider;
import com.minh.locket_clone_backend.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FirebaseUserSyncServiceTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private FirebaseUserSyncService firebaseUserSyncService;

    @Nested
    @DisplayName("syncUser()")
    class SyncUserTests {

        @Test
        void syncUser_phoneProvider_extractsPhoneNumberAndReturnsResponse() {
            // given
            FirebaseToken mockToken = mock(FirebaseToken.class);
            when(mockToken.getUid()).thenReturn("firebase-uid-123");
            when(mockToken.getEmail()).thenReturn(null);
            when(mockToken.getName()).thenReturn(null);
            when(mockToken.getPicture()).thenReturn(null);
            when(mockToken.getClaims()).thenReturn(Map.of("phone_number", "+84123456789"));

            UserResponse userResponse = new UserResponse(
                    UUID.randomUUID(), "firebase-uid-123", "+84123456789", AuthProvider.PHONE,
                    null, null, null, null
            );
            SyncUserResponse syncUserResponse = new SyncUserResponse(userResponse, true, true);

            when(userService.syncWithFirebase(any(SyncUserRequest.class))).thenReturn(syncUserResponse);

            // when
            AuthResponse response = firebaseUserSyncService.syncUser(mockToken, AuthProvider.PHONE);

            // then
            assertThat(response).isNotNull();
            assertThat(response.user()).isEqualTo(userResponse);
            assertThat(response.isNewUser()).isTrue();
            assertThat(response.isProfileIncomplete()).isTrue();

            ArgumentCaptor<SyncUserRequest> requestCaptor = ArgumentCaptor.forClass(SyncUserRequest.class);
            verify(userService, times(1)).syncWithFirebase(requestCaptor.capture());

            SyncUserRequest capturedRequest = requestCaptor.getValue();
            assertThat(capturedRequest.firebaseUid()).isEqualTo("firebase-uid-123");
            assertThat(capturedRequest.expectedProvider()).isEqualTo(AuthProvider.PHONE);
            assertThat(capturedRequest.phoneNumber()).isEqualTo("+84123456789");
        }

        @Test
        void syncUser_nonPhoneProvider_doesNotExtractPhoneNumber() {
            // given
            FirebaseToken mockToken = mock(FirebaseToken.class);
            when(mockToken.getUid()).thenReturn("firebase-uid-123");
            when(mockToken.getEmail()).thenReturn("test@gmail.com");
            when(mockToken.getName()).thenReturn("Test User");
            when(mockToken.getPicture()).thenReturn("pic.jpg");

            UserResponse userResponse = new UserResponse(
                    UUID.randomUUID(), "firebase-uid-123", null, AuthProvider.GOOGLE,
                    "test@gmail.com", "Test User", null, "pic.jpg"
            );
            SyncUserResponse syncUserResponse = new SyncUserResponse(userResponse, false, false);

            when(userService.syncWithFirebase(any(SyncUserRequest.class))).thenReturn(syncUserResponse);

            // when
            AuthResponse response = firebaseUserSyncService.syncUser(mockToken, AuthProvider.GOOGLE);

            // then
            assertThat(response).isNotNull();
            assertThat(response.user()).isEqualTo(userResponse);
            assertThat(response.isNewUser()).isFalse();
            assertThat(response.isProfileIncomplete()).isFalse();

            ArgumentCaptor<SyncUserRequest> requestCaptor = ArgumentCaptor.forClass(SyncUserRequest.class);
            verify(userService, times(1)).syncWithFirebase(requestCaptor.capture());

            SyncUserRequest capturedRequest = requestCaptor.getValue();
            assertThat(capturedRequest.firebaseUid()).isEqualTo("firebase-uid-123");
            assertThat(capturedRequest.expectedProvider()).isEqualTo(AuthProvider.GOOGLE);
            assertThat(capturedRequest.email()).isEqualTo("test@gmail.com");
            assertThat(capturedRequest.phoneNumber()).isNull();
        }
    }
}
