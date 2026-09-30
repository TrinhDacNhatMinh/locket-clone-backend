package com.minh.locket_clone_backend.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import io.swagger.v3.oas.annotations.media.Schema;

public record UpdateProfileRequest(
        @Schema(description = "User's display name", example = "John Doe")
        @Size(min = 1, max = 50, message = "Display name must be between 1 and 50 characters")
        String displayName,

        @Schema(description = "User's email address", example = "john@example.com")
        @Email(message = "Invalid email format")
        String email,

        @Schema(description = "User's phone number in E.164 format", example = "+1234567890")
        @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
        String phoneNumber,

        @Schema(description = "User's birthday", example = "2000-01-01")
        @Past(message = "Birthday must be in the past")
        LocalDate birthday,

        @Schema(description = "URL of the user's avatar image", example = "https://example.com/avatar.jpg")
        String avatarUrl
) {}
