package com.minh.locket_clone_backend.friend.repository;

import com.minh.locket_clone_backend.friend.entity.Friend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FriendRepository extends JpaRepository<Friend, UUID> {

    boolean existsByUserAIdAndUserBId(UUID userAId, UUID userBId);

    void deleteByUserAIdAndUserBId(UUID userAId, UUID userBId);

    @Query("SELECT f FROM Friend f WHERE f.userAId = :userId OR f.userBId = :userId")
    List<Friend> findAllByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM Friend f WHERE f.userAId = :userId OR f.userBId = :userId")
    void deleteAllByUserId(@Param("userId") UUID userId);
}
