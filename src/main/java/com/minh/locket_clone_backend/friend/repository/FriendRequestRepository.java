package com.minh.locket_clone_backend.friend.repository;

import com.minh.locket_clone_backend.friend.entity.FriendRequest;
import com.minh.locket_clone_backend.friend.entity.FriendRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FriendRequestRepository extends JpaRepository<FriendRequest, UUID> {

    Optional<FriendRequest> findByRequesterIdAndAddresseeIdAndStatus(UUID requesterId, UUID addresseeId, FriendRequestStatus status);

    boolean existsByRequesterIdAndAddresseeIdAndStatus(UUID requesterId, UUID addresseeId, FriendRequestStatus status);

    List<FriendRequest> findByAddresseeIdAndStatus(UUID addresseeId, FriendRequestStatus status);

    @Modifying
    @Query("DELETE FROM FriendRequest fr WHERE fr.requesterId = :userId OR fr.addresseeId = :userId")
    void deleteAllByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM FriendRequest fr WHERE fr.status = :status AND fr.updatedAt < :cutoff")
    int deleteByStatusAndUpdatedAtBefore(@Param("status") FriendRequestStatus status, @Param("cutoff") java.time.Instant cutoff);

    @Modifying
    @Query("DELETE FROM FriendRequest fr WHERE (fr.requesterId = :userA AND fr.addresseeId = :userB) OR (fr.requesterId = :userB AND fr.addresseeId = :userA)")
    void deleteFriendRequestsBetween(@Param("userA") UUID userA, @Param("userB") UUID userB);
}
