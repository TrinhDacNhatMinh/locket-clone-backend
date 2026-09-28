package com.minh.locket_clone_backend.chat.repository;

import com.minh.locket_clone_backend.chat.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    /**
     * Finds a conversation that has exactly these two participants and no others.
     */
    @Query("""
            SELECT c FROM Conversation c
            WHERE EXISTS (
                SELECT 1 FROM ConversationParticipant p1
                WHERE p1.conversationId = c.id AND p1.userId = :userA
            )
            AND EXISTS (
                SELECT 1 FROM ConversationParticipant p2
                WHERE p2.conversationId = c.id AND p2.userId = :userB
            )
            AND (
                SELECT COUNT(p) FROM ConversationParticipant p
                WHERE p.conversationId = c.id
            ) = 2
            """)
    Optional<Conversation> findByParticipants(@Param("userA") UUID userA, @Param("userB") UUID userB);
}
