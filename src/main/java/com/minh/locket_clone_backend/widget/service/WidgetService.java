package com.minh.locket_clone_backend.widget.service;

import com.minh.locket_clone_backend.widget.dto.WidgetItemResponse;

import java.util.List;
import java.util.UUID;

public interface WidgetService {
    List<WidgetItemResponse> getWidgetPhotos(UUID viewerId);
}
