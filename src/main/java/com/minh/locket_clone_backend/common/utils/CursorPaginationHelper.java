package com.minh.locket_clone_backend.common.utils;

import com.minh.locket_clone_backend.common.dto.CursorPagedResponse;
import com.minh.locket_clone_backend.common.exception.BusinessException;
import com.minh.locket_clone_backend.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class CursorPaginationHelper {

    public record Cursor(Instant createdAt, UUID id) {}

    public static String encodeCursor(Instant createdAt, UUID id) {
        String plainCursor = createdAt.toString() + "_" + id.toString();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(plainCursor.getBytes(StandardCharsets.UTF_8));
    }

    public static Cursor decodeCursor(String encodedCursor) {
        try {
            String plainCursor = new String(Base64.getUrlDecoder().decode(encodedCursor), StandardCharsets.UTF_8);
            String[] parts = plainCursor.split("_");
            return new Cursor(Instant.parse(parts[0]), UUID.fromString(parts[1]));
        } catch (Exception e) {
            log.warn("Failed to decode cursor: {}", encodedCursor, e);
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Invalid cursor format");
        }
    }

    public static <T, R> CursorPagedResponse<R> buildPagedResponse(
            List<T> items,
            int limit,
            Function<T, R> mapper,
            Function<T, Instant> timeExtractor,
            Function<T, UUID> idExtractor) {

        boolean hasMore = items.size() > limit;
        List<T> pageContent = hasMore ? items.subList(0, limit) : items;

        String nextCursor = null;
        if (!pageContent.isEmpty() && hasMore) {
            T lastItem = pageContent.get(pageContent.size() - 1);
            nextCursor = encodeCursor(timeExtractor.apply(lastItem), idExtractor.apply(lastItem));
        }

        List<R> responses = pageContent.stream().map(mapper).collect(Collectors.toList());

        return new CursorPagedResponse<>(responses, nextCursor, hasMore);
    }
}
