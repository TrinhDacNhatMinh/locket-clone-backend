package com.minh.locket_clone_backend.reaction.service;

import com.minh.locket_clone_backend.common.exception.BusinessException;
import com.minh.locket_clone_backend.common.exception.ErrorCode;
import com.minh.locket_clone_backend.photo.entity.Photo;
import com.minh.locket_clone_backend.photo.service.PhotoService;
import com.minh.locket_clone_backend.reaction.dto.ReactionRequest;
import com.minh.locket_clone_backend.reaction.dto.ReactionResponse;
import com.minh.locket_clone_backend.reaction.entity.Reaction;
import com.minh.locket_clone_backend.reaction.repository.ReactionRepository;
import com.minh.locket_clone_backend.notification.entity.NotificationType;
import com.minh.locket_clone_backend.notification.service.NotificationService;
import com.minh.locket_clone_backend.user.entity.User;
import com.minh.locket_clone_backend.user.service.UserService;
import com.minh.locket_clone_backend.websocket.SessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minh.locket_clone_backend.websocket.dto.RealtimeEventType;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReactionServiceImpl implements ReactionService {

    private final ReactionRepository reactionRepository;
    private final PhotoService photoService;
    private final NotificationService notificationService;
    private final UserService userService;
    private final SessionRegistry sessionRegistry;

    @Override
    @Transactional
    public void react(UUID reactorId, UUID photoId, ReactionRequest request) {
        // Check if the user is allowed to view the photo
        Photo photo = photoService.getPhotoIfAllowed(reactorId, photoId);

        // Insert Reaction(seen = false)
        Reaction reaction = Reaction.builder()
                .photoId(photoId)
                .userId(reactorId)
                .emoji(request.emoji())
                .seen(false)
                .build();

        Reaction savedReaction = reactionRepository.save(reaction);

        User reactor = userService.getUserById(reactorId);
        notificationService.notify(
                photo.getOwnerId(),
                NotificationType.REACTION,
                RealtimeEventType.REACTION,
                Map.of(
                        "actorDisplayName", reactor.getDisplayName(),
                        "photoId", photoId.toString(),
                        "emoji", request.emoji()
                ),
                ReactionResponse.from(savedReaction)
        );

        // Mark as seen immediately if online
        if (sessionRegistry.isOnline(photo.getOwnerId())) {
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
        // 1. Get a photo to verify ownership
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
