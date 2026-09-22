package com.minh.locket_clone_backend.common.dto;

import java.util.List;

public record CursorPagedResponse<T>(
        List<T> data,
        String nextCursor,
        boolean hasMore
) {}
