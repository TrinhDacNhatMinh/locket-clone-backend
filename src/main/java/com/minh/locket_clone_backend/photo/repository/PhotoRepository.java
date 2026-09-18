package com.minh.locket_clone_backend.photo.repository;

import com.minh.locket_clone_backend.photo.entity.Photo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, UUID> {
    
    Optional<Photo> findByIdAndOwnerId(UUID id, UUID ownerId);

    @Modifying
    @Query("UPDATE Photo p SET p.deletedAt = CURRENT_TIMESTAMP WHERE p.ownerId = :ownerId AND p.deletedAt IS NULL")
    void softDeleteAllByOwnerId(@Param("ownerId") UUID ownerId);
}
