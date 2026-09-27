package com.minh.locket_clone_backend.reaction.dto;

import com.minh.locket_clone_backend.reaction.entity.Reaction;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

public record ReactionResponse(
        UUID id,
        UUID photoId,
        UUID userId,
        String emoji,
        Instant createdAt
) {
    public static ReactionResponse from(Reaction reaction) {
        return new ReactionResponse(
                reaction.getId(),
                reaction.getPhotoId(),
                reaction.getUserId(),
                reaction.getEmoji(),
                reaction.getCreatedAt()
        );
    }
}
