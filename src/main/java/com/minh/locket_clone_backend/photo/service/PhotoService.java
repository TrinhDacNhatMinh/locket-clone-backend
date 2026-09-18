package com.minh.locket_clone_backend.photo.service;

import com.minh.locket_clone_backend.photo.dto.PhotoResponse;
import com.minh.locket_clone_backend.photo.entity.AudienceType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface PhotoService {

    PhotoResponse createPhoto(UUID ownerId, MultipartFile image, String caption, AudienceType audienceType, List<UUID> audienceUserIds, String metadataJson);

}
