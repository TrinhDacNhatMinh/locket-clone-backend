package com.minh.locket_clone_backend.friend.service;

import com.minh.locket_clone_backend.friend.entity.FriendRequestStatus;
import com.minh.locket_clone_backend.friend.repository.FriendRequestRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendRequestCleanupServiceTest {

    @Mock
    private FriendRequestRepository friendRequestRepository;

    @InjectMocks
    private FriendRequestCleanupService cleanupService;

    @Nested
    @DisplayName("cleanupOldRejectedRequests()")
    class CleanupTests {

        @Test
        void cleanupOldRejectedRequests_callsRepositoryWithCorrectParams() {
            // given
            when(friendRequestRepository.deleteByStatusAndUpdatedAtBefore(eq(FriendRequestStatus.REJECTED), any(Instant.class)))
                    .thenReturn(5);

            // when
            cleanupService.cleanupOldRejectedRequests();

            // then
            ArgumentCaptor<Instant> dateCaptor = ArgumentCaptor.forClass(Instant.class);
            verify(friendRequestRepository, times(1))
                    .deleteByStatusAndUpdatedAtBefore(eq(FriendRequestStatus.REJECTED), dateCaptor.capture());

            Instant capturedDate = dateCaptor.getValue();
            assertThat(capturedDate).isNotNull();
            // It should be roughly 7 days ago
            assertThat(capturedDate).isBefore(Instant.now());
        }
    }
}
