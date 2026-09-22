package com.minh.locket_clone_backend.feed.service;

import com.minh.locket_clone_backend.common.dto.CursorPagedResponse;
import com.minh.locket_clone_backend.photo.dto.PhotoResponse;

import java.util.UUID;

public interface FeedService {
    
    CursorPagedResponse<PhotoResponse> getFeed(UUID viewerId, String cursor, int limit);
    
    CursorPagedResponse<PhotoResponse> getFriendPhotoHistory(UUID viewerId, UUID friendId, String cursor, int limit);
}
