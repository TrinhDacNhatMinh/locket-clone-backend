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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minh.locket_clone_backend.websocket.dto.RealtimeEventType;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReactionServiceImpl implements ReactionService {

    private final ReactionRepository reactionRepository;
    private final PhotoService photoService;
    private final NotificationService notificationService;
    private final UserService userService;

    @Override
    @Transactional
    public void react(UUID reactorId, UUID photoId, ReactionRequest request) {
        // Check if the user is allowed to view the photo
        Photo photo = photoService.getPhotoIfAllowed(reactorId, photoId);

        // Delivery does not mean the owner has viewed the reaction.
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
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReactionResponse> getReactions(UUID ownerId, UUID photoId) {
        checkPhotoOwner(ownerId, photoId);

        return reactionRepository.findByPhotoIdOrderByCreatedAtDescIdDesc(photoId).stream()
                .map(ReactionResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<ReactionResponse> getUnseenReactions(UUID ownerId, UUID photoId) {
        checkPhotoOwner(ownerId, photoId);

        // Fetch unseen reactions.
        List<Reaction> unseenReactions = reactionRepository.findByPhotoIdAndSeenFalse(photoId);

        if (unseenReactions.isEmpty()) {
            return List.of();
        }

        // Only this explicit view operation marks reactions as seen.
        unseenReactions.forEach(r -> r.setSeen(true));
        reactionRepository.saveAll(unseenReactions);

        return unseenReactions.stream()
                .map(ReactionResponse::from)
                .collect(Collectors.toList());
    }

    private void checkPhotoOwner(UUID ownerId, UUID photoId) {
        Photo photo = photoService.getPhotoIfAllowed(ownerId, photoId);
        if (!photo.getOwnerId().equals(ownerId)) {
            throw new BusinessException(ErrorCode.PHOTO_NOT_OWNER);
        }
    }
}
