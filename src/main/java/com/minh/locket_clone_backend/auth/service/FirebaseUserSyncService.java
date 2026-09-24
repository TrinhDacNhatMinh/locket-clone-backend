package com.minh.locket_clone_backend.auth.service;

import com.google.firebase.auth.FirebaseToken;
import com.minh.locket_clone_backend.auth.dto.AuthResponse;
import com.minh.locket_clone_backend.user.entity.AuthProvider;
import com.minh.locket_clone_backend.user.dto.SyncUserRequest;
import com.minh.locket_clone_backend.user.dto.SyncUserResponse;
import com.minh.locket_clone_backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FirebaseUserSyncService {

    private final UserService userService;

    public AuthResponse syncUser(FirebaseToken token, AuthProvider expectedProvider) {
        String phoneNumber = null;
        if (expectedProvider == AuthProvider.PHONE) {
            phoneNumber = (String) token.getClaims().get("phone_number");
        }

        SyncUserRequest request = new SyncUserRequest(
                token.getUid(),
                expectedProvider,
                token.getEmail(),
                token.getName(),
                token.getPicture(),
                phoneNumber
        );

        SyncUserResponse response = userService.syncWithFirebase(request);
        return new AuthResponse(response.user(), response.isNewUser(), response.isProfileIncomplete());
    }
}
