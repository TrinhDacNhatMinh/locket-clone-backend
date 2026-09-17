package com.minh.locket_clone_backend.user.repository;

import com.minh.locket_clone_backend.user.entity.Block;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BlockRepository extends JpaRepository<Block, UUID> {

    boolean existsByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);

    @Modifying
    @Query("DELETE FROM Block b WHERE b.blockerId = :blockerId AND b.blockedId = :blockedId")
    int deleteByBlockerIdAndBlockedId(@Param("blockerId") UUID blockerId, @Param("blockedId") UUID blockedId);

    @Query("SELECT COUNT(b) > 0 FROM Block b WHERE (b.blockerId = :currentUserId AND b.blockedId = :targetUserId) OR (b.blockerId = :targetUserId AND b.blockedId = :currentUserId)")
    boolean existsBidirectional(@Param("currentUserId") UUID currentUserId, @Param("targetUserId") UUID targetUserId);

    @Modifying
    @Query("DELETE FROM Block b WHERE b.blockerId = :userId OR b.blockedId = :userId")
    void deleteAllByUserId(@Param("userId") UUID userId);
}
