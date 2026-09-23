package com.minh.locket_clone_backend.widget.dto;

import com.minh.locket_clone_backend.photo.dto.PhotoResponse;

import java.util.UUID;

public record WidgetItemResponse(
        UUID friendId,
        PhotoResponse photo
) {}
