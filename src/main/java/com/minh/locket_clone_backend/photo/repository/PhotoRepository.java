package com.minh.locket_clone_backend.photo.repository;

import com.minh.locket_clone_backend.photo.entity.Photo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

import org.springframework.data.domain.Pageable;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, UUID> {

    Optional<Photo> findByIdAndOwnerId(UUID id, UUID ownerId);

    @Modifying
    @Query("UPDATE Photo p SET p.deletedAt = CURRENT_TIMESTAMP WHERE p.ownerId = :ownerId AND p.deletedAt IS NULL")
    void softDeleteAllByOwnerId(@Param("ownerId") UUID ownerId);

    // Feed queries (First Page and Next Page to avoid NULL parameter issues in JPQL)
    @Query("SELECT p FROM Photo p WHERE p.ownerId IN :friendIds AND " +
            "(p.audienceType = 'ALL_FRIENDS' OR EXISTS (SELECT 1 FROM PhotoAudience pa WHERE pa.photoId = p.id AND pa.userId = :viewerId)) "
            +
            "ORDER BY p.createdAt DESC, p.id DESC")
    List<Photo> findFeedPhotosFirstPage(@Param("friendIds") List<UUID> friendIds, @Param("viewerId") UUID viewerId, Pageable pageable);

    @Query("SELECT p FROM Photo p WHERE p.ownerId IN :friendIds AND " +
            "(p.audienceType = 'ALL_FRIENDS' OR EXISTS (SELECT 1 FROM PhotoAudience pa WHERE pa.photoId = p.id AND pa.userId = :viewerId)) AND "
            +
            "(p.createdAt < :cursorCreatedAt OR (p.createdAt = :cursorCreatedAt AND p.id < :cursorId)) " +
            "ORDER BY p.createdAt DESC, p.id DESC")
    List<Photo> findFeedPhotosNextPage(@Param("friendIds") List<UUID> friendIds, @Param("viewerId") UUID viewerId, @Param("cursorCreatedAt") Instant cursorCreatedAt, @Param("cursorId") UUID cursorId, Pageable pageable);

    // Friend Feed queries
    @Query("SELECT p FROM Photo p WHERE p.ownerId = :friendId AND " +
            "(p.audienceType = 'ALL_FRIENDS' OR EXISTS (SELECT 1 FROM PhotoAudience pa WHERE pa.photoId = p.id AND pa.userId = :viewerId)) "
            +
            "ORDER BY p.createdAt DESC, p.id DESC")
    List<Photo> findFriendPhotosFirstPage(@Param("friendId") UUID friendId, @Param("viewerId") UUID viewerId, Pageable pageable);

    @Query("SELECT p FROM Photo p WHERE p.ownerId = :friendId AND " +
            "(p.audienceType = 'ALL_FRIENDS' OR EXISTS (SELECT 1 FROM PhotoAudience pa WHERE pa.photoId = p.id AND pa.userId = :viewerId)) AND "
            +
            "(p.createdAt < :cursorCreatedAt OR (p.createdAt = :cursorCreatedAt AND p.id < :cursorId)) " +
            "ORDER BY p.createdAt DESC, p.id DESC")
    List<Photo> findFriendPhotosNextPage(@Param("friendId") UUID friendId, @Param("viewerId") UUID viewerId, @Param("cursorCreatedAt") Instant cursorCreatedAt, @Param("cursorId") UUID cursorId, Pageable pageable);

    // My photos queries
    @Query("SELECT p FROM Photo p WHERE p.ownerId = :ownerId ORDER BY p.createdAt DESC, p.id DESC")
    List<Photo> findMyPhotosFirstPage(@Param("ownerId") UUID ownerId, Pageable pageable);

    @Query("SELECT p FROM Photo p WHERE p.ownerId = :ownerId AND " +
            "(p.createdAt < :cursorCreatedAt OR (p.createdAt = :cursorCreatedAt AND p.id < :cursorId)) " +
            "ORDER BY p.createdAt DESC, p.id DESC")
    List<Photo> findMyPhotosNextPage(@Param("ownerId") UUID ownerId, @Param("cursorCreatedAt") Instant cursorCreatedAt, @Param("cursorId") UUID cursorId, Pageable pageable);

    // Widget query (Native PostgresSQL query with DISTINCT ON)
    @Query(nativeQuery = true, value =
            "SELECT DISTINCT ON (owner_id) * " +
                    "FROM photos p " +
                    "WHERE p.owner_id IN :friendIds " +
                    "AND p.deleted_at IS NULL " +
                    "AND (p.audience_type = 'ALL_FRIENDS' " +
                    "OR EXISTS (SELECT 1 FROM photo_audiences pa WHERE pa.photo_id = p.id AND pa.user_id = :viewerId)) " +
                    "ORDER BY p.owner_id, p.created_at DESC")
    List<Photo> findWidgetPhotos(@Param("friendIds") List<UUID> friendIds, @Param("viewerId") UUID viewerId);
}
