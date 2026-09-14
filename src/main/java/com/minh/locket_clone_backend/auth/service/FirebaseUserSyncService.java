package com.minh.locket_clone_backend.auth.service;

import com.google.firebase.auth.FirebaseToken;
import com.minh.locket_clone_backend.auth.dto.AuthResponse;
import com.minh.locket_clone_backend.user.dto.UserResponse;
import com.minh.locket_clone_backend.user.entity.AuthProvider;
import com.minh.locket_clone_backend.user.entity.User;
import com.minh.locket_clone_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FirebaseUserSyncService {

    private final UserRepository userRepository;

    @Transactional
    public AuthResponse syncUser(FirebaseToken token, AuthProvider expectedProvider) {
        String firebaseUid = token.getUid();
        Optional<User> userOptional = userRepository.findByFirebaseUid(firebaseUid);

        if (userOptional.isPresent()) {
            User existingUser = userOptional.get();
            boolean isProfileIncomplete = !existingUser.isProfileCompleted();
            return new AuthResponse(UserResponse.from(existingUser), false, isProfileIncomplete);
        }

        // Create new user
        User newUser = new User();
        newUser.setFirebaseUid(firebaseUid);
        newUser.setAuthProvider(expectedProvider);
        newUser.setUsername(generateTempUsername());

        if (expectedProvider == AuthProvider.GOOGLE) {
            newUser.setEmail(token.getEmail());
            newUser.setDisplayName(token.getName());
            newUser.setAvatarUrl(token.getPicture());
        } else if (expectedProvider == AuthProvider.PHONE) {
            String phoneNumber = (String) token.getClaims().get("phone_number");
            newUser.setPhoneNumber(phoneNumber);
        }
        
        newUser.setProfileCompleted(false);

        User savedUser = userRepository.save(newUser);
        return new AuthResponse(UserResponse.from(savedUser), true, true);
    }

    private String generateTempUsername() {
        int maxRetries = 5;
        for (int i = 0; i < maxRetries; i++) {
            String tempUsername = "user_" + UUID.randomUUID().toString().substring(0, 8);
            if (!userRepository.existsByUsername(tempUsername)) {
                return tempUsername;
            }
        }
        throw new RuntimeException("Could not generate a unique temporary username after " + maxRetries + " attempts");
    }
}
