package com.minh.locket_clone_backend.friend.service;

import com.minh.locket_clone_backend.common.exception.BusinessException;
import com.minh.locket_clone_backend.common.exception.ErrorCode;
import com.minh.locket_clone_backend.friend.dto.FriendRequestAction;
import com.minh.locket_clone_backend.friend.dto.FriendRequestResponse;
import com.minh.locket_clone_backend.friend.dto.RespondFriendRequestRequest;
import com.minh.locket_clone_backend.friend.dto.SendFriendRequestRequest;
import com.minh.locket_clone_backend.friend.entity.Friend;
import com.minh.locket_clone_backend.friend.entity.FriendRequest;
import com.minh.locket_clone_backend.friend.entity.FriendRequestStatus;
import com.minh.locket_clone_backend.friend.repository.FriendRepository;
import com.minh.locket_clone_backend.friend.repository.FriendRequestRepository;
import com.minh.locket_clone_backend.user.dto.PublicProfileResponse;
import com.minh.locket_clone_backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendServiceImpl implements FriendService {

    private final FriendRequestRepository friendRequestRepository;
    private final FriendRepository friendRepository;
    private final ObjectProvider<UserService> userServiceProvider;

    @Override
    @Transactional
    public void sendFriendRequest(UUID requesterId, SendFriendRequestRequest request) {
        UUID addresseeId = request.addresseeId();

        if (requesterId.equals(addresseeId)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Cannot send friend request to yourself");
        }

        // Validate user exists by fetching profile
        userServiceProvider.getObject().getProfile(addresseeId);

        // TODO: Check isBlocked() here

        if (isFriend(requesterId, addresseeId)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Already friends");
        }

        if (friendRequestRepository.existsByRequesterIdAndAddresseeIdAndStatus(requesterId, addresseeId, FriendRequestStatus.PENDING)) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_ALREADY_EXISTS);
        }

        // Check rate limit: 7-day cooldown for rejected requests (relying on cronjob to clear old requests)
        if (friendRequestRepository.existsByRequesterIdAndAddresseeIdAndStatus(requesterId, addresseeId, FriendRequestStatus.REJECTED)) {
            throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED);
        }

        // Check if there is a pending request in the opposite direction
        Optional<FriendRequest> reverseRequestOpt = friendRequestRepository
                .findByRequesterIdAndAddresseeIdAndStatus(addresseeId, requesterId, FriendRequestStatus.PENDING);

        if (reverseRequestOpt.isPresent()) {
            // Auto accept
            FriendRequest reverseRequest = reverseRequestOpt.get();
            reverseRequest.setStatus(FriendRequestStatus.ACCEPTED);
            friendRequestRepository.save(reverseRequest);
            createFriendship(requesterId, addresseeId);
            log.info("Auto-accepted friend request between {} and {}", requesterId, addresseeId);
        } else {
            // Create new pending request
            FriendRequest newRequest = FriendRequest.builder()
                    .requesterId(requesterId)
                    .addresseeId(addresseeId)
                    .status(FriendRequestStatus.PENDING)
                    .build();
            friendRequestRepository.save(newRequest);
            log.info("Friend request sent from {} to {}", requesterId, addresseeId);
        }
    }

    @Override
    @Transactional
    public void respondFriendRequest(UUID responderId, UUID requestId, RespondFriendRequestRequest request) {
        FriendRequest friendRequest = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Friend request not found"));

        if (!friendRequest.getAddresseeId().equals(responderId)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "You can only respond to requests addressed to you");
        }

        if (friendRequest.getStatus() != FriendRequestStatus.PENDING) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Friend request is not pending");
        }

        if (request.action() == FriendRequestAction.ACCEPT) {
            friendRequest.setStatus(FriendRequestStatus.ACCEPTED);
            createFriendship(friendRequest.getRequesterId(), friendRequest.getAddresseeId());
            log.info("Friend request {} accepted by {}", requestId, responderId);
        } else if (request.action() == FriendRequestAction.REJECT) {
            friendRequest.setStatus(FriendRequestStatus.REJECTED);
            log.info("Friend request {} rejected by {}", requestId, responderId);
        }

        friendRequestRepository.save(friendRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FriendRequestResponse> getPendingFriendRequests(UUID userId) {
        List<FriendRequest> pendingRequests = friendRequestRepository.findByAddresseeIdAndStatus(userId, FriendRequestStatus.PENDING);

        return pendingRequests.stream()
                .map(request -> {
                    // Fetch the public profile of the requester
                    PublicProfileResponse requesterProfile = userServiceProvider.getObject().getPublicProfile(userId, request.getRequesterId());
                    return new FriendRequestResponse(request.getId(), requesterProfile);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicProfileResponse> getFriendList(UUID userId) {
        List<Friend> friends = friendRepository.findAllByUserId(userId);

        return friends.stream()
                .map(f -> f.getUserAId().equals(userId) ? f.getUserBId() : f.getUserAId())
                .map(friendUserId -> userServiceProvider.getObject().getPublicProfile(userId, friendUserId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void removeFriend(UUID userId, UUID friendUserId) {
        if (!isFriend(userId, friendUserId)) {
            throw new BusinessException(ErrorCode.NOT_FRIENDS);
        }
        removeFriendshipIfExists(userId, friendUserId);
        log.info("User {} removed friend {}", userId, friendUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFriend(UUID userAId, UUID userBId) {
        UUID a = userAId.compareTo(userBId) < 0 ? userAId : userBId;
        UUID b = userAId.compareTo(userBId) < 0 ? userBId : userAId;
        return friendRepository.existsByUserAIdAndUserBId(a, b);
    }

    @Override
    @Transactional
    public void removeFriendshipIfExists(UUID userAId, UUID userBId) {
        UUID a = userAId.compareTo(userBId) < 0 ? userAId : userBId;
        UUID b = userAId.compareTo(userBId) < 0 ? userBId : userAId;
        friendRepository.deleteByUserAIdAndUserBId(a, b);
    }

    @Override
    @Transactional
    public void deleteAllInvolvingUser(UUID userId) {
        friendRepository.deleteAllByUserId(userId);
        friendRequestRepository.deleteAllByUserId(userId);
    }

    private void createFriendship(UUID user1, UUID user2) {
        UUID a = user1.compareTo(user2) < 0 ? user1 : user2;
        UUID b = user1.compareTo(user2) < 0 ? user2 : user1;

        if (!friendRepository.existsByUserAIdAndUserBId(a, b)) {
            Friend friend = Friend.builder()
                    .userAId(a)
                    .userBId(b)
                    .build();
            friendRepository.save(friend);
        }
    }
}
