package com.minh.locket_clone_backend.photo.dto;

import com.minh.locket_clone_backend.photo.entity.AudienceType;
import com.minh.locket_clone_backend.photo.entity.Photo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhotoResponse {
    private UUID id;
    private UUID ownerId;
    private String imageUrl;
    private String caption;
    private AudienceType audienceType;
    private Map<String, Object> metadata;
    private Instant createdAt;

    public static PhotoResponse from(Photo photo) {
        return PhotoResponse.builder()
                .id(photo.getId())
                .ownerId(photo.getOwnerId())
                .imageUrl(photo.getImageUrl())
                .caption(photo.getCaption())
                .audienceType(photo.getAudienceType())
                .metadata(photo.getMetadata())
                .createdAt(photo.getCreatedAt())
                .build();
    }
}
