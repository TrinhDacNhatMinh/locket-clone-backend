package com.minh.locket_clone_backend.feed.service;

import com.minh.locket_clone_backend.common.dto.CursorPagedResponse;
import com.minh.locket_clone_backend.common.exception.BusinessException;
import com.minh.locket_clone_backend.common.exception.ErrorCode;
import com.minh.locket_clone_backend.friend.service.FriendService;
import com.minh.locket_clone_backend.photo.dto.PhotoResponse;
import com.minh.locket_clone_backend.photo.entity.Photo;
import com.minh.locket_clone_backend.photo.service.PhotoService;
import com.minh.locket_clone_backend.common.utils.CursorPaginationHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {

    private final PhotoService photoService;
    private final FriendService friendService;

    @Override
    @Transactional(readOnly = true)
    public CursorPagedResponse<PhotoResponse> getFeed(UUID viewerId, String cursor, int limit) {
        List<UUID> friendIds = friendService.getFriendIds(viewerId);

        if (friendIds.isEmpty()) {
            return new CursorPagedResponse<>(List.of(), null, false);
        }

        // We fetch limit + 1 to determine if there is a next page
        List<Photo> photos;

        if (cursor == null || cursor.trim().isEmpty()) {
            photos = photoService.getFeedPhotos(friendIds, viewerId, null, null, limit + 1);
        } else {
            CursorPaginationHelper.Cursor decodedCursor = CursorPaginationHelper.decodeCursor(cursor);
            photos = photoService.getFeedPhotos(friendIds, viewerId, decodedCursor.createdAt(), decodedCursor.id(), limit + 1);
        }

        return CursorPaginationHelper.buildPagedResponse(
                photos,
                limit,
                PhotoResponse::from,
                Photo::getCreatedAt,
                Photo::getId
        );
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPagedResponse<PhotoResponse> getFriendPhotoHistory(UUID viewerId, UUID friendId, String cursor, int limit) {
        if (!friendService.isFriend(viewerId, friendId)) {
            throw new BusinessException(ErrorCode.NOT_FRIENDS, "You are not friends with this user");
        }

        List<Photo> photos;

        if (cursor == null || cursor.trim().isEmpty()) {
            photos = photoService.getFriendPhotos(friendId, viewerId, null, null, limit + 1);
        } else {
            CursorPaginationHelper.Cursor decodedCursor = CursorPaginationHelper.decodeCursor(cursor);
            photos = photoService.getFriendPhotos(friendId, viewerId, decodedCursor.createdAt(), decodedCursor.id(), limit + 1);
        }

        return CursorPaginationHelper.buildPagedResponse(
                photos,
                limit,
                PhotoResponse::from,
                Photo::getCreatedAt,
                Photo::getId
        );
    }
}
