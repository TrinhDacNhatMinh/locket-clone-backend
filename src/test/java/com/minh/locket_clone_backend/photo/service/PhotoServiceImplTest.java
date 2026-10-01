package com.minh.locket_clone_backend.photo.service;

import com.minh.locket_clone_backend.common.exception.BusinessException;
import com.minh.locket_clone_backend.common.exception.ErrorCode;
import com.minh.locket_clone_backend.friend.service.FriendService;
import com.minh.locket_clone_backend.infrastructure.storage.service.StorageService;
import com.minh.locket_clone_backend.notification.entity.NotificationType;
import com.minh.locket_clone_backend.notification.service.NotificationService;
import com.minh.locket_clone_backend.photo.dto.PhotoResponse;
import com.minh.locket_clone_backend.photo.entity.AudienceType;
import com.minh.locket_clone_backend.photo.entity.Photo;
import com.minh.locket_clone_backend.photo.entity.PhotoAudience;
import com.minh.locket_clone_backend.photo.repository.PhotoAudienceRepository;
import com.minh.locket_clone_backend.photo.repository.PhotoRepository;
import com.minh.locket_clone_backend.user.entity.User;
import com.minh.locket_clone_backend.user.service.UserService;
import com.minh.locket_clone_backend.websocket.dto.RealtimeEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhotoServiceImplTest {

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private PhotoAudienceRepository photoAudienceRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private FriendService friendService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserService userService;


    private PhotoServiceImpl photoService;

    @BeforeEach
    void setUp() {
        JsonMapper jsonMapper = new JsonMapper();
        photoService = new PhotoServiceImpl(
                photoRepository,
                photoAudienceRepository,
                storageService,
                friendService,
                notificationService,
                userService,
                jsonMapper
        );
    }

    @Nested
    @DisplayName("createPhoto()")
    class CreatePhotoTests {
        @Test
        void createPhoto_allFriends_savesAndNotifiesFriends() {
            // given
            UUID ownerId = UUID.randomUUID();
            MultipartFile file = new MockMultipartFile("image", new byte[]{1, 2, 3});
            String caption = "caption";

            when(storageService.uploadImage(file)).thenReturn("http://url");
            
            Photo savedPhoto = Photo.builder().ownerId(ownerId).imageUrl("http://url").build();
            savedPhoto.setId(UUID.randomUUID());
            when(photoRepository.save(any(Photo.class))).thenReturn(savedPhoto);
            
            UUID friendId = UUID.randomUUID();
            when(friendService.getFriendIds(ownerId)).thenReturn(List.of(friendId));

            User owner = new User();
            owner.setDisplayName("Alice");
            when(userService.getUserById(ownerId)).thenReturn(owner);

            // when
            PhotoResponse response = photoService.createPhoto(ownerId, file, caption, AudienceType.ALL_FRIENDS, null, null);

            // then
            assertThat(response).isNotNull();
            verify(photoAudienceRepository, never()).saveAll(any());
            verify(notificationService).notify(eq(friendId), eq(NotificationType.NEW_PHOTO), eq(RealtimeEventType.WIDGET_UPDATE), any(), any());
        }

        @Test
        void createPhoto_customAudience_filtersSilentlyAndSaves() {
            // given
            UUID ownerId = UUID.randomUUID();
            MultipartFile file = new MockMultipartFile("image", new byte[]{1, 2, 3});
            
            UUID friendId = UUID.randomUUID();
            UUID notFriendId = UUID.randomUUID(); // Silent filter should drop this

            when(friendService.isFriend(ownerId, friendId)).thenReturn(true);
            when(friendService.isFriend(ownerId, notFriendId)).thenReturn(false);

            when(storageService.uploadImage(file)).thenReturn("http://url");

            Photo savedPhoto = Photo.builder().ownerId(ownerId).imageUrl("http://url").build();
            savedPhoto.setId(UUID.randomUUID());
            when(photoRepository.save(any(Photo.class))).thenReturn(savedPhoto);

            User owner = new User();
            owner.setDisplayName("Alice");
            when(userService.getUserById(ownerId)).thenReturn(owner);

            // when
            photoService.createPhoto(ownerId, file, "", AudienceType.CUSTOM, List.of(friendId, notFriendId), null);

            // then
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<PhotoAudience>> listCaptor = ArgumentCaptor.forClass(List.class);
            verify(photoAudienceRepository).saveAll(listCaptor.capture());
            
            List<PhotoAudience> savedAudiences = listCaptor.getValue();
            assertThat(savedAudiences).hasSize(1);
            assertThat(savedAudiences.get(0).getUserId()).isEqualTo(friendId);

            verify(notificationService).notify(eq(friendId), eq(NotificationType.NEW_PHOTO), eq(RealtimeEventType.WIDGET_UPDATE), any(), any());
            verify(notificationService, never()).notify(eq(notFriendId), any(), any(), any(), any());
        }

        @Test
        void createPhoto_invalidMetadataJson_throwsException() {
            // given
            UUID ownerId = UUID.randomUUID();
            MultipartFile file = new MockMultipartFile("image", new byte[]{1, 2, 3});
            
            // when and then
            BusinessException ex = assertThrows(BusinessException.class, 
                    () -> photoService.createPhoto(ownerId, file, "", AudienceType.ALL_FRIENDS, null, "{invalid json"));
            
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
            assertThat(ex.getMessage()).contains("Invalid JSON");
        }
    }

    @Nested
    @DisplayName("getPhotoIfAllowed()")
    class GetPhotoIfAllowedTests {
        @Test
        void getPhotoIfAllowed_owner_returnsPhoto() {
            // given
            UUID photoId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();
            Photo photo = Photo.builder().ownerId(ownerId).build();
            photo.setId(photoId);

            when(photoRepository.findById(photoId)).thenReturn(Optional.of(photo));

            // when
            Photo result = photoService.getPhotoIfAllowed(ownerId, photoId);

            // then
            assertThat(result).isNotNull();
        }

        @Test
        void getPhotoIfAllowed_allFriendsButNotFriend_throwsException() {
            // given
            UUID photoId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();
            UUID viewerId = UUID.randomUUID();
            Photo photo = Photo.builder().ownerId(ownerId).audienceType(AudienceType.ALL_FRIENDS).build();
            photo.setId(photoId);

            when(photoRepository.findById(photoId)).thenReturn(Optional.of(photo));
            when(friendService.isFriend(ownerId, viewerId)).thenReturn(false);

            // when and then
            BusinessException ex = assertThrows(BusinessException.class, () -> photoService.getPhotoIfAllowed(viewerId, photoId));
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PHOTO_ACCESS_DENIED);
        }

        @Test
        void getPhotoIfAllowed_customAudienceNotIncluded_throwsException() {
            // given
            UUID photoId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();
            UUID viewerId = UUID.randomUUID();
            Photo photo = Photo.builder().ownerId(ownerId).audienceType(AudienceType.CUSTOM).build();
            photo.setId(photoId);

            when(photoRepository.findById(photoId)).thenReturn(Optional.of(photo));
            when(photoAudienceRepository.existsByPhotoIdAndUserId(photoId, viewerId)).thenReturn(false);

            // when and then
            BusinessException ex = assertThrows(BusinessException.class, () -> photoService.getPhotoIfAllowed(viewerId, photoId));
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PHOTO_ACCESS_DENIED);
        }
    }

    @Nested
    @DisplayName("deletePhoto()")
    class DeletePhotoTests {
        @Test
        void deletePhoto_owner_softDeletes() {
            // given
            UUID photoId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();
            Photo photo = Photo.builder().ownerId(ownerId).build();
            photo.setId(photoId);

            when(photoRepository.findByIdAndOwnerId(photoId, ownerId)).thenReturn(Optional.of(photo));

            // when
            photoService.deletePhoto(ownerId, photoId);

            // then
            verify(photoRepository).save(photo);
            assertThat(photo.getDeletedAt()).isNotNull();
        }

        @Test
        void deletePhoto_notOwner_throwsException() {
            // given
            UUID photoId = UUID.randomUUID();
            UUID notOwnerId = UUID.randomUUID();

            when(photoRepository.findByIdAndOwnerId(photoId, notOwnerId)).thenReturn(Optional.empty());

            // when and then
            BusinessException ex = assertThrows(BusinessException.class, () -> photoService.deletePhoto(notOwnerId, photoId));
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PHOTO_NOT_OWNER);
        }
    }
}
