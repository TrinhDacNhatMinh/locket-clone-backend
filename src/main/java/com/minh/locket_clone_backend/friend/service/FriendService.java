package com.minh.locket_clone_backend.friend.service;

import com.minh.locket_clone_backend.friend.dto.FriendRequestResponse;
import com.minh.locket_clone_backend.friend.dto.RespondFriendRequestRequest;
import com.minh.locket_clone_backend.friend.dto.SendFriendRequestRequest;
import com.minh.locket_clone_backend.user.dto.PublicProfileResponse;

import java.util.List;
import java.util.UUID;

public interface FriendService {

    void sendFriendRequest(UUID requesterId, SendFriendRequestRequest request);

    void respondFriendRequest(UUID responderId, UUID requestId, RespondFriendRequestRequest request);

    List<FriendRequestResponse> getPendingFriendRequests(UUID userId);

    List<PublicProfileResponse> getFriendList(UUID userId);

    void removeFriend(UUID userId, UUID friendUserId);

    boolean isFriend(UUID userAId, UUID userBId);

    void removeFriendshipIfExists(UUID userAId, UUID userBId);

    void deleteFriendRequestsBetween(UUID userAId, UUID userBId);

    void deleteAllInvolvingUser(UUID userId);
}
