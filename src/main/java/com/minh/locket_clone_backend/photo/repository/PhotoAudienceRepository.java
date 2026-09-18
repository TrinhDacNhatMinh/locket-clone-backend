package com.minh.locket_clone_backend.photo.repository;

import com.minh.locket_clone_backend.photo.entity.PhotoAudience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PhotoAudienceRepository extends JpaRepository<PhotoAudience, UUID> {
    
    @Query("SELECT pa.userId FROM PhotoAudience pa WHERE pa.photoId = :photoId")
    List<UUID> findUserIdsByPhotoId(@Param("photoId") UUID photoId);
    
    boolean existsByPhotoIdAndUserId(UUID photoId, UUID userId);
}
