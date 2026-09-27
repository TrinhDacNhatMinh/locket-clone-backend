package com.minh.locket_clone_backend.reaction.service;

import com.minh.locket_clone_backend.common.exception.BusinessException;
import com.minh.locket_clone_backend.common.exception.ErrorCode;
import com.minh.locket_clone_backend.photo.entity.Photo;
import com.minh.locket_clone_backend.photo.service.PhotoService;
import com.minh.locket_clone_backend.reaction.dto.ReactionRequest;
import com.minh.locket_clone_backend.reaction.dto.ReactionResponse;
import com.minh.locket_clone_backend.reaction.entity.Reaction;
import com.minh.locket_clone_backend.reaction.repository.ReactionRepository;
import com.minh.locket_clone_backend.websocket.SessionRegistry;
import com.minh.locket_clone_backend.websocket.RealtimeEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReactionServiceImpl implements ReactionService {

    private final ReactionRepository reactionRepository;
    private final PhotoService photoService;
    private final SessionRegistry sessionRegistry;
    private final RealtimeEventPublisher realtimeEventPublisher;

    @Override
    @Transactional
    public void react(UUID reactorId, UUID photoId, ReactionRequest request) {
        // 1. Check if the user is allowed to view the photo
        Photo photo = photoService.getPhotoIfAllowed(reactorId, photoId);

        // 2. Insert Reaction(seen = false)
        Reaction reaction = Reaction.builder()
                .photoId(photoId)
                .userId(reactorId)
                .emoji(request.emoji())
                .seen(false)
                .build();
        
        Reaction savedReaction = reactionRepository.save(reaction);

        // 3. Check if owner is online
        if (sessionRegistry.isOnline(photo.getOwnerId())) {
            // Send WebSocket event
            ReactionResponse payload = ReactionResponse.from(savedReaction);
            realtimeEventPublisher.publish(photo.getOwnerId(), "REACTION", payload);

            // Mark as seen immediately
            savedReaction.setSeen(true);
            reactionRepository.save(savedReaction);
            
            log.info("Reaction sent via WebSocket to online owner {}", photo.getOwnerId());
        } else {
            log.info("Owner {} is offline. Reaction marked as unseen.", photo.getOwnerId());
        }
    }

    @Override
    @Transactional
    public List<ReactionResponse> getUnseenReactions(UUID ownerId, UUID photoId) {
        // 1. Get photo to verify ownership
        Photo photo = photoService.getPhotoIfAllowed(ownerId, photoId);
        if (!photo.getOwnerId().equals(ownerId)) {
            throw new BusinessException(ErrorCode.PHOTO_NOT_OWNER);
        }

        // 2. Fetch unseen reactions
        List<Reaction> unseenReactions = reactionRepository.findByPhotoIdAndSeenFalse(photoId);

        if (unseenReactions.isEmpty()) {
            return List.of();
        }

        // 3. Update them to seen=true
        unseenReactions.forEach(r -> r.setSeen(true));
        reactionRepository.saveAll(unseenReactions);

        return unseenReactions.stream()
                .map(ReactionResponse::from)
                .collect(Collectors.toList());
    }
}
