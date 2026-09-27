package com.minh.locket_clone_backend.reaction.service;

import com.minh.locket_clone_backend.reaction.dto.ReactionRequest;
import com.minh.locket_clone_backend.reaction.dto.ReactionResponse;

import java.util.List;
import java.util.UUID;

public interface ReactionService {

    void react(UUID reactorId, UUID photoId, ReactionRequest request);

    List<ReactionResponse> getUnseenReactions(UUID ownerId, UUID photoId);
}
