package com.minh.locket_clone_backend.feed.service;

import com.minh.locket_clone_backend.common.dto.CursorPagedResponse;
import com.minh.locket_clone_backend.common.exception.BusinessException;
import com.minh.locket_clone_backend.common.exception.ErrorCode;
import com.minh.locket_clone_backend.friend.service.FriendService;
import com.minh.locket_clone_backend.photo.dto.PhotoResponse;
import com.minh.locket_clone_backend.photo.entity.Photo;
import com.minh.locket_clone_backend.photo.service.PhotoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedServiceImplTest {

    @Mock
    private PhotoService photoService;

    @Mock
    private FriendService friendService;

    @InjectMocks
    private FeedServiceImpl feedService;

    @Nested
    @DisplayName("getFeed()")
    class GetFeedTests {
        @Test
        void getFeed_noFriends_returnsEmpty() {
            // given
            UUID viewerId = UUID.randomUUID();
            when(friendService.getFriendIds(viewerId)).thenReturn(List.of());

            // when
            CursorPagedResponse<PhotoResponse> response = feedService.getFeed(viewerId, null, 10);

            // then
            assertThat(response.data()).isEmpty();
            assertThat(response.hasMore()).isFalse();
        }

        @Test
        void getFeed_noCursor_fetchesFirstPage() {
            // given
            UUID viewerId = UUID.randomUUID();
            UUID friendId = UUID.randomUUID();
            List<UUID> friendIds = List.of(friendId);
            when(friendService.getFriendIds(viewerId)).thenReturn(friendIds);

            Photo p = Photo.builder().ownerId(friendId).build();
            p.setId(UUID.randomUUID());
            p.setCreatedAt(Instant.now());
            when(photoService.getFeedPhotos(eq(friendIds), eq(viewerId), isNull(), isNull(), eq(11)))
                    .thenReturn(List.of(p));

            // when
            CursorPagedResponse<PhotoResponse> response = feedService.getFeed(viewerId, null, 10);

            // then
            verify(photoService).getFeedPhotos(friendIds, viewerId, null, null, 11);
            assertThat(response.data()).hasSize(1);
            assertThat(response.hasMore()).isFalse();
        }

        @Test
        void getFeed_withCursor_fetchesNextPage() {
            // given
            UUID viewerId = UUID.randomUUID();
            UUID friendId = UUID.randomUUID();
            List<UUID> friendIds = List.of(friendId);
            when(friendService.getFriendIds(viewerId)).thenReturn(friendIds);

            Instant now = Instant.now();
            UUID lastId = UUID.randomUUID();
            String rawCursor = now.toString() + "_" + lastId;
            String encodedCursor = Base64.getUrlEncoder().withoutPadding().encodeToString(rawCursor.getBytes());

            Photo p1 = Photo.builder().ownerId(friendId).build();
            p1.setId(UUID.randomUUID());
            p1.setCreatedAt(now.minusSeconds(10));
            Photo p2 = Photo.builder().ownerId(friendId).build();
            p2.setId(UUID.randomUUID());
            p2.setCreatedAt(now.minusSeconds(20));

            when(photoService.getFeedPhotos(eq(friendIds), eq(viewerId), any(Instant.class), eq(lastId), eq(2)))
                    .thenReturn(List.of(p1, p2));

            // when (limit 1, repo returns 2, so hasMore = true)
            CursorPagedResponse<PhotoResponse> response = feedService.getFeed(viewerId, encodedCursor, 1);

            // then
            verify(photoService).getFeedPhotos(eq(friendIds), eq(viewerId), any(Instant.class), eq(lastId), eq(2));
            assertThat(response.data()).hasSize(1);
            assertThat(response.hasMore()).isTrue();
            assertThat(response.nextCursor()).isNotNull();
        }
    }

    @Nested
    @DisplayName("getFriendPhotoHistory()")
    class GetFriendPhotoHistoryTests {
        @Test
        void getFriendPhotoHistory_notFriend_throwsException() {
            // given
            UUID viewerId = UUID.randomUUID();
            UUID friendId = UUID.randomUUID();
            when(friendService.isFriend(viewerId, friendId)).thenReturn(false);

            // when and then
            BusinessException ex = assertThrows(BusinessException.class, () -> feedService.getFriendPhotoHistory(viewerId, friendId, null, 10));
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_FRIENDS);
        }

        @Test
        void getFriendPhotoHistory_isFriend_fetchesPhotos() {
            // given
            UUID viewerId = UUID.randomUUID();
            UUID friendId = UUID.randomUUID();
            when(friendService.isFriend(viewerId, friendId)).thenReturn(true);

            Photo p = Photo.builder().ownerId(friendId).build();
            p.setId(UUID.randomUUID());
            p.setCreatedAt(Instant.now());
            when(photoService.getFriendPhotos(eq(friendId), eq(viewerId), isNull(), isNull(), eq(11)))
                    .thenReturn(List.of(p));

            // when
            CursorPagedResponse<PhotoResponse> response = feedService.getFriendPhotoHistory(viewerId, friendId, null, 10);

            // then
            verify(photoService).getFriendPhotos(friendId, viewerId, null, null, 11);
            assertThat(response.data()).hasSize(1);
            assertThat(response.hasMore()).isFalse();
        }
    }
}
