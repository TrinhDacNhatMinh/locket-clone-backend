package com.minh.locket_clone_backend.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    @Override
    public void notifyNewPhoto(UUID ownerId, List<UUID> eligibleViewerIds) {
        // TODO: Implement real push notifications via FCM / WebSocket
        log.info("Stub: notifyNewPhoto called for owner {} with {} eligible viewers", ownerId, eligibleViewerIds.size());
    }
}
