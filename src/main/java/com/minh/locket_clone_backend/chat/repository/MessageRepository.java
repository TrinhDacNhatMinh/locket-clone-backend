package com.minh.locket_clone_backend.chat.repository;

import com.minh.locket_clone_backend.chat.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByConversationIdOrderByCreatedAtDesc(UUID conversationId, Pageable pageable);

    // Cursor-based pagination: fetch messages older than the given cursor position
    @Query("""
            SELECT m FROM Message m
            WHERE m.conversationId = :conversationId
            AND (m.createdAt < :cursorCreatedAt
                OR (m.createdAt = :cursorCreatedAt AND m.id < :cursorId))
            ORDER BY m.createdAt DESC, m.id DESC
            """)
    List<Message> findByConversationIdNextPage(
            @Param("conversationId") UUID conversationId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable);
}
