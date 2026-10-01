package com.minh.locket_clone_backend.widget.service;

import com.minh.locket_clone_backend.friend.service.FriendService;
import com.minh.locket_clone_backend.photo.entity.Photo;
import com.minh.locket_clone_backend.photo.repository.PhotoRepository;
import com.minh.locket_clone_backend.widget.dto.WidgetItemResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WidgetServiceImplTest {

    @Mock
    private FriendService friendService;

    @Mock
    private PhotoRepository photoRepository;

    @InjectMocks
    private WidgetServiceImpl widgetService;

    @Nested
    @DisplayName("getWidgetPhotos()")
    class GetWidgetPhotosTests {
        @Test
        void getWidgetPhotos_noFriends_returnsEmptyList() {
            // given
            UUID viewerId = UUID.randomUUID();
            when(friendService.getFriendIds(viewerId)).thenReturn(List.of());

            // when
            List<WidgetItemResponse> responses = widgetService.getWidgetPhotos(viewerId);

            // then
            assertThat(responses).isEmpty();
        }

        @Test
        void getWidgetPhotos_hasFriends_returnsWidgetPhotos() {
            // given
            UUID viewerId = UUID.randomUUID();
            UUID friendId = UUID.randomUUID();
            List<UUID> friendIds = List.of(friendId);
            
            when(friendService.getFriendIds(viewerId)).thenReturn(friendIds);

            Photo photo = Photo.builder().ownerId(friendId).build();
            photo.setId(UUID.randomUUID());
            when(photoRepository.findWidgetPhotos(eq(friendIds), eq(viewerId))).thenReturn(List.of(photo));

            // when
            List<WidgetItemResponse> responses = widgetService.getWidgetPhotos(viewerId);

            // then
            verify(photoRepository).findWidgetPhotos(eq(friendIds), eq(viewerId));
            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).friendId()).isEqualTo(friendId);
            assertThat(responses.get(0).photo().getId()).isEqualTo(photo.getId());
        }
    }
}
