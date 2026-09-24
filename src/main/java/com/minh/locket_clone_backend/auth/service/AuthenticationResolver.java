package com.minh.locket_clone_backend.auth.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.minh.locket_clone_backend.auth.security.CustomUserDetails;
import com.minh.locket_clone_backend.common.exception.BusinessException;
import com.minh.locket_clone_backend.common.exception.ErrorCode;
import com.minh.locket_clone_backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationResolver {

    private final FirebaseAuth firebaseAuth;
    private final UserService userService;

    // Verifies a Firebase ID Token and resolves it to the internal userId.
    public CustomUserDetails resolveUser(String idToken) throws FirebaseAuthException {
        FirebaseToken token = firebaseAuth.verifyIdToken(idToken);
        String firebaseUid = token.getUid();
        
        UUID userId = userService.findUserIdByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "User account not found or has been deleted"));
                
        return new CustomUserDetails(userId, firebaseUid);
    }
}
