package com.minh.locket_clone_backend.user.repository;

import com.minh.locket_clone_backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByFirebaseUid(String firebaseUid);

    boolean existsByUsername(String username);

    boolean existsByEmailAndIdNot(String email, UUID id);

    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "AND u.id != :currentUserId " +
            "AND NOT EXISTS (SELECT 1 FROM Block b WHERE (b.blockerId = :currentUserId AND b.blockedId = u.id) " +
            "OR (b.blockerId = u.id AND b.blockedId = :currentUserId))")
    List<User> searchUsers(@Param("query") String query, @Param("currentUserId") UUID currentUserId, Pageable pageable);
}
