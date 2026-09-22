package com.minh.locket_clone_backend.photo.service;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import com.minh.locket_clone_backend.common.exception.BusinessException;
import com.minh.locket_clone_backend.common.exception.ErrorCode;
import com.minh.locket_clone_backend.friend.service.FriendService;
import com.minh.locket_clone_backend.notification.service.NotificationService;
import com.minh.locket_clone_backend.photo.dto.PhotoResponse;
import com.minh.locket_clone_backend.photo.entity.AudienceType;
import com.minh.locket_clone_backend.photo.entity.Photo;
import com.minh.locket_clone_backend.photo.entity.PhotoAudience;
import com.minh.locket_clone_backend.photo.repository.PhotoAudienceRepository;
import com.minh.locket_clone_backend.photo.repository.PhotoRepository;
import com.minh.locket_clone_backend.common.dto.CursorPagedResponse;
import com.minh.locket_clone_backend.common.utils.CursorPaginationHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhotoServiceImpl implements PhotoService {

    private final PhotoRepository photoRepository;
    private final PhotoAudienceRepository photoAudienceRepository;
    private final StorageService storageService;
    private final FriendService friendService;
    private final NotificationService notificationService;
    private final JsonMapper jsonMapper;

    @Override
    @Transactional
    public PhotoResponse createPhoto(UUID ownerId, MultipartFile image, String caption, AudienceType audienceType, List<UUID> audienceUserIds, String metadataJson) {

        Map<String, Object> metadata = parseAndValidateMetadata(metadataJson);

        if (audienceType == AudienceType.CUSTOM) {
            if (audienceUserIds == null) {
                audienceUserIds = List.of();
            }
            // Silent Filtering
            audienceUserIds = audienceUserIds.stream()
                    .filter(userId -> friendService.isFriend(ownerId, userId))
                    .collect(Collectors.toList());
        }

        String imageUrl = storageService.uploadImage(image);

        Photo photo = Photo.builder()
                .ownerId(ownerId)
                .imageUrl(imageUrl)
                .caption(caption)
                .audienceType(audienceType)
                .metadata(metadata)
                .build();

        final Photo savedPhoto = photoRepository.save(photo);

        List<UUID> eligibleViewerIds;

        if (audienceType == AudienceType.CUSTOM) {
            List<PhotoAudience> audiences = audienceUserIds.stream()
                    .map(userId -> PhotoAudience.builder()
                            .photoId(savedPhoto.getId())
                            .userId(userId)
                            .build())
                    .collect(Collectors.toList());
            photoAudienceRepository.saveAll(audiences);
            eligibleViewerIds = audienceUserIds;
        } else {
            eligibleViewerIds = friendService.getFriendIds(ownerId);
        }

        notificationService.notifyNewPhoto(ownerId, eligibleViewerIds);

        return PhotoResponse.from(savedPhoto);
    }

    @Override
    @Transactional
    public void deletePhoto(UUID requesterId, UUID photoId) {
        Photo photo = photoRepository.findByIdAndOwnerId(photoId, requesterId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHOTO_NOT_OWNER));

        // Softly delete
        photo.setDeletedAt(java.time.Instant.now());
        photoRepository.save(photo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Photo> getFeedPhotos(List<UUID> friendIds, UUID viewerId, Instant cursorCreatedAt, UUID cursorId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        if (cursorCreatedAt == null) {
            return photoRepository.findFeedPhotosFirstPage(friendIds, viewerId, pageable);
        }
        return photoRepository.findFeedPhotosNextPage(friendIds, viewerId, cursorCreatedAt, cursorId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Photo> getFriendPhotos(UUID friendId, UUID viewerId, Instant cursorCreatedAt, UUID cursorId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        if (cursorCreatedAt == null) {
            return photoRepository.findFriendPhotosFirstPage(friendId, viewerId, pageable);
        }
        return photoRepository.findFriendPhotosNextPage(friendId, viewerId, cursorCreatedAt, cursorId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPagedResponse<PhotoResponse> getMyPhotos(UUID ownerId, String cursor, int limit) {
        Pageable pageable = PageRequest.of(0, limit + 1);
        List<Photo> photos;

        if (cursor == null || cursor.trim().isEmpty()) {
            photos = photoRepository.findMyPhotosFirstPage(ownerId, pageable);
        } else {
            CursorPaginationHelper.Cursor decodedCursor = CursorPaginationHelper.decodeCursor(cursor);
            photos = photoRepository.findMyPhotosNextPage(ownerId, decodedCursor.createdAt(), decodedCursor.id(), pageable);
        }

        return CursorPaginationHelper.buildPagedResponse(
                photos,
                limit,
                PhotoResponse::from,
                Photo::getCreatedAt,
                Photo::getId
        );
    }

    private Map<String, Object> parseAndValidateMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.trim().isEmpty()) {
            return null;
        }

        if (metadataJson.length() > 5120) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Metadata size must not exceed 5KB");
        }

        try {
            return jsonMapper.readValue(metadataJson, new TypeReference<>() {
            });
        } catch (JacksonException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Invalid JSON format for metadata");
        }
    }
}
