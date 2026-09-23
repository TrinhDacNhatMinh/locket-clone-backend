package com.minh.locket_clone_backend.widget.service;

import com.minh.locket_clone_backend.friend.service.FriendService;
import com.minh.locket_clone_backend.photo.dto.PhotoResponse;
import com.minh.locket_clone_backend.photo.entity.Photo;
import com.minh.locket_clone_backend.photo.repository.PhotoRepository;
import com.minh.locket_clone_backend.widget.dto.WidgetItemResponse;
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
public class WidgetServiceImpl implements WidgetService {

    private final FriendService friendService;
    private final PhotoRepository photoRepository;

    @Override
    @Transactional(readOnly = true)
    public List<WidgetItemResponse> getWidgetPhotos(UUID viewerId) {
        List<UUID> friendIds = friendService.getFriendIds(viewerId);

        if (friendIds.isEmpty()) {
            return List.of();
        }

        List<Photo> widgetPhotos = photoRepository.findWidgetPhotos(friendIds, viewerId);

        return widgetPhotos.stream()
                .map(photo -> new WidgetItemResponse(photo.getOwnerId(), PhotoResponse.from(photo)))
                .collect(Collectors.toList());
    }
}
