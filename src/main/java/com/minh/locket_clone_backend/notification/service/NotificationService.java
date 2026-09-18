package com.minh.locket_clone_backend.notification.service;

import java.util.List;
import java.util.UUID;

public interface NotificationService {

    void notifyNewPhoto(UUID ownerId, List<UUID> eligibleViewerIds);
}
