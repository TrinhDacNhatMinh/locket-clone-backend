package com.minh.locket_clone_backend.reaction.repository;

import com.minh.locket_clone_backend.reaction.entity.Reaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReactionRepository extends JpaRepository<Reaction, UUID> {
    
    List<Reaction> findByPhotoIdAndSeenFalse(UUID photoId);
}
