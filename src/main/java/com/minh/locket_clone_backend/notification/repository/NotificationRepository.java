package com.minh.locket_clone_backend.notification.repository;

import com.minh.locket_clone_backend.notification.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    @Query("""
            SELECT n FROM Notification n WHERE n.userId = :userId
            ORDER BY n.createdAt DESC, n.id DESC
            """)
    List<Notification> findByUserIdFirstPage(@Param("userId") UUID userId, Pageable pageable);

    @Query("""
            SELECT n FROM Notification n WHERE n.userId = :userId AND
            (n.createdAt < :cursorCreatedAt OR (n.createdAt = :cursorCreatedAt AND n.id < :cursorId))
            ORDER BY n.createdAt DESC, n.id DESC
            """)
    List<Notification> findByUserIdNextPage(@Param("userId") UUID userId, @Param("cursorCreatedAt") Instant cursorCreatedAt, @Param("cursorId") UUID cursorId, Pageable pageable);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id AND n.userId = :userId")
    void markAsRead(@Param("id") UUID id, @Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId AND n.isRead = false")
    void markAllAsRead(@Param("userId") UUID userId);
}
