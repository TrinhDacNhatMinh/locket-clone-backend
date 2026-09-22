package com.minh.locket_clone_backend.photo.service;

import com.minh.locket_clone_backend.photo.dto.PhotoResponse;
import com.minh.locket_clone_backend.photo.entity.AudienceType;
import org.springframework.web.multipart.MultipartFile;
import com.minh.locket_clone_backend.common.dto.CursorPagedResponse;

import java.time.Instant;
import java.util.List;

import com.minh.locket_clone_backend.photo.entity.Photo;

import java.util.UUID;

public interface PhotoService {

    PhotoResponse createPhoto(UUID ownerId, MultipartFile image, String caption, AudienceType audienceType, List<UUID> audienceUserIds, String metadataJson);

    void deletePhoto(UUID requesterId, UUID photoId);

    CursorPagedResponse<PhotoResponse> getMyPhotos(UUID ownerId, String cursor, int limit);

    List<Photo> getFeedPhotos(List<UUID> friendIds, UUID viewerId, Instant cursorCreatedAt, UUID cursorId, int limit);

    List<Photo> getFriendPhotos(UUID friendId, UUID viewerId, Instant cursorCreatedAt, UUID cursorId, int limit);
}
