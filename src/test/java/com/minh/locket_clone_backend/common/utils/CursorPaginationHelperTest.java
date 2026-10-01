package com.minh.locket_clone_backend.common.utils;

import com.minh.locket_clone_backend.common.dto.CursorPagedResponse;
import com.minh.locket_clone_backend.common.exception.BusinessException;
import com.minh.locket_clone_backend.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CursorPaginationHelperTest {

    @Nested
    @DisplayName("encodeCursor()")
    class EncodeCursorTests {
        @Test
        void encodeCursor_validInputs_returnsBase64String() {
            // given
            Instant createdAt = Instant.parse("2023-10-01T12:00:00Z");
            UUID id = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

            // when
            String encoded = CursorPaginationHelper.encodeCursor(createdAt, id);

            // then
            assertThat(encoded).isNotBlank();

            CursorPaginationHelper.Cursor decoded = CursorPaginationHelper.decodeCursor(encoded);
            assertThat(decoded.createdAt()).isEqualTo(createdAt);
            assertThat(decoded.id()).isEqualTo(id);
        }
    }

    @Nested
    @DisplayName("decodeCursor()")
    class DecodeCursorTests {
        @Test
        void decodeCursor_validCursor_returnsCursorObject() {
            // given
            Instant createdAt = Instant.parse("2023-10-01T12:00:00Z");
            UUID id = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
            String encoded = CursorPaginationHelper.encodeCursor(createdAt, id);

            // when
            CursorPaginationHelper.Cursor decoded = CursorPaginationHelper.decodeCursor(encoded);

            // then
            assertThat(decoded.createdAt()).isEqualTo(createdAt);
            assertThat(decoded.id()).isEqualTo(id);
        }

        @Test
        void decodeCursor_invalidFormat_throwsBusinessException() {
            // given
            String invalidCursor = "invalid_base64_string";

            // when and then
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> CursorPaginationHelper.decodeCursor(invalidCursor));
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
        }

        @Test
        void decodeCursor_nullInput_throwsBusinessException() {
            // given & when & then
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> CursorPaginationHelper.decodeCursor(null));
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
        }
    }

    @Nested
    @DisplayName("buildPagedResponse()")
    class BuildPagedResponseTests {

        @Test
        void buildPagedResponse_hasMoreItems_returnsPagedResponseWithNextCursor() {
            // given
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            UUID id3 = UUID.randomUUID();
            Instant now = Instant.now();

            List<DummyItem> items = List.of(
                    new DummyItem(id1, now, "Item 1"),
                    new DummyItem(id2, now.minusSeconds(60), "Item 2"),
                    new DummyItem(id3, now.minusSeconds(120), "Item 3")
            );

            // Limit is 2, item size is 3 -> hasMore = true
            int limit = 2;

            // when
            CursorPagedResponse<DummyResponse> response = CursorPaginationHelper.buildPagedResponse(
                    items,
                    limit,
                    item -> new DummyResponse(item.id(), item.name()),
                    DummyItem::createdAt,
                    DummyItem::id
            );

            // then
            assertThat(response.hasMore()).isTrue();
            assertThat(response.data()).hasSize(2);
            assertThat(response.data().get(0).id()).isEqualTo(id1);
            assertThat(response.data().get(1).id()).isEqualTo(id2);

            // nextCursor should be encoded from the last item of the page (Item 2)
            CursorPaginationHelper.Cursor decodedCursor = CursorPaginationHelper.decodeCursor(response.nextCursor());
            assertThat(decodedCursor.id()).isEqualTo(id2);
            assertThat(decodedCursor.createdAt()).isEqualTo(now.minusSeconds(60));
        }

        @Test
        void buildPagedResponse_noMoreItems_returnsPagedResponseWithoutNextCursor() {
            // given
            UUID id1 = UUID.randomUUID();
            Instant now = Instant.now();

            List<DummyItem> items = List.of(
                    new DummyItem(id1, now, "Item 1")
            );

            // Limit is 2, item size is 1 -> hasMore = false
            int limit = 2;

            // when
            CursorPagedResponse<DummyResponse> response = CursorPaginationHelper.buildPagedResponse(
                    items,
                    limit,
                    item -> new DummyResponse(item.id(), item.name()),
                    DummyItem::createdAt,
                    DummyItem::id
            );

            // then
            assertThat(response.hasMore()).isFalse();
            assertThat(response.data()).hasSize(1);
            assertThat(response.nextCursor()).isNull();
        }

        @Test
        void buildPagedResponse_emptyList_returnsEmptyPagedResponse() {
            // given
            List<DummyItem> items = List.of();
            int limit = 2;

            // when
            CursorPagedResponse<DummyResponse> response = CursorPaginationHelper.buildPagedResponse(
                    items,
                    limit,
                    item -> new DummyResponse(item.id(), item.name()),
                    DummyItem::createdAt,
                    DummyItem::id
            );

            // then
            assertThat(response.hasMore()).isFalse();
            assertThat(response.data()).isEmpty();
            assertThat(response.nextCursor()).isNull();
        }

        record DummyItem(UUID id, Instant createdAt, String name) {
        }

        record DummyResponse(UUID id, String name) {
        }
    }
}
