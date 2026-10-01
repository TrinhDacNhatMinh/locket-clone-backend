package com.minh.locket_clone_backend.auth.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.minh.locket_clone_backend.auth.security.CustomUserDetails;
import com.minh.locket_clone_backend.common.exception.BusinessException;
import com.minh.locket_clone_backend.common.exception.ErrorCode;
import com.minh.locket_clone_backend.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationResolverTest {

    @Mock
    private FirebaseAuth firebaseAuth;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthenticationResolver authenticationResolver;

    @Nested
    @DisplayName("resolveUser()")
    class ResolveUserTests {

        @Test
        void resolveUser_validIdTokenAndUserExists_returnsCustomUserDetails() throws FirebaseAuthException {
            // given
            String idToken = "valid-id-token";
            String firebaseUid = "firebase-uid-123";
            UUID userId = UUID.randomUUID();

            FirebaseToken mockToken = mock(FirebaseToken.class);
            when(mockToken.getUid()).thenReturn(firebaseUid);
            when(firebaseAuth.verifyIdToken(idToken)).thenReturn(mockToken);
            when(userService.findUserIdByFirebaseUid(firebaseUid)).thenReturn(Optional.of(userId));

            // when
            CustomUserDetails userDetails = authenticationResolver.resolveUser(idToken);

            // then
            assertThat(userDetails).isNotNull();
            assertThat(userDetails.userId()).isEqualTo(userId);
            assertThat(userDetails.firebaseUid()).isEqualTo(firebaseUid);
            verify(firebaseAuth, times(1)).verifyIdToken(idToken);
            verify(userService, times(1)).findUserIdByFirebaseUid(firebaseUid);
        }

        @Test
        void resolveUser_invalidIdToken_throwsFirebaseAuthException() throws FirebaseAuthException {
            // given
            String idToken = "invalid-id-token";
            when(firebaseAuth.verifyIdToken(idToken)).thenThrow(mock(FirebaseAuthException.class));

            // when and then
            assertThrows(FirebaseAuthException.class, () -> authenticationResolver.resolveUser(idToken));
            verify(userService, never()).findUserIdByFirebaseUid(anyString());
        }

        @Test
        void resolveUser_userNotFound_throwsBusinessException() throws FirebaseAuthException {
            // given
            String idToken = "valid-id-token";
            String firebaseUid = "firebase-uid-123";

            FirebaseToken mockToken = mock(FirebaseToken.class);
            when(mockToken.getUid()).thenReturn(firebaseUid);
            when(firebaseAuth.verifyIdToken(idToken)).thenReturn(mockToken);
            when(userService.findUserIdByFirebaseUid(firebaseUid)).thenReturn(Optional.empty());

            // when and then
            BusinessException ex = assertThrows(BusinessException.class, () -> authenticationResolver.resolveUser(idToken));
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
            verify(firebaseAuth, times(1)).verifyIdToken(idToken);
            verify(userService, times(1)).findUserIdByFirebaseUid(firebaseUid);
        }
    }
}
