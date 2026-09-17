package com.minh.locket_clone_backend.friend.service;

import com.minh.locket_clone_backend.friend.entity.FriendRequestStatus;
import com.minh.locket_clone_backend.friend.repository.FriendRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class FriendRequestCleanupService {

    private final FriendRequestRepository friendRequestRepository;

    /**
     * Run every day at 00:00.
     * Cleans up rejected friend requests older than 7 days.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void cleanupOldRejectedRequests() {
        log.info("Starting cleanup of old rejected friend requests...");
        
        Instant cutoffDate = Instant.now().minus(7, ChronoUnit.DAYS);
        
        int deletedCount = friendRequestRepository.deleteByStatusAndUpdatedAtBefore(
                FriendRequestStatus.REJECTED, 
                cutoffDate
        );
        
        log.info("Cleanup finished. Deleted {} old rejected friend requests.", deletedCount);
    }
}
