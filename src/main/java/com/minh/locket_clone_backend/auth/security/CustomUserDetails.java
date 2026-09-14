package com.minh.locket_clone_backend.auth.security;

import java.util.UUID;

public record CustomUserDetails(
        UUID userId,
        String firebaseUid) {
}
