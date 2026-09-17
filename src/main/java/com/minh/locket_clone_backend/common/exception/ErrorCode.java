package com.minh.locket_clone_backend.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    FIREBASE_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "Firebase token is invalid"),
    FIREBASE_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Firebase token has expired"),
    USER_BLOCKED(HttpStatus.FORBIDDEN, "User is blocked"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),
    USERNAME_ALREADY_TAKEN(HttpStatus.CONFLICT, "Username is already taken"),
    EMAIL_ALREADY_IN_USE(HttpStatus.CONFLICT, "Email is already in use"),
    NOT_FRIENDS(HttpStatus.BAD_REQUEST, "Users are not friends"),
    FRIEND_REQUEST_ALREADY_EXISTS(HttpStatus.CONFLICT, "Friend request already exists"),
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "Please wait 7 days before sending another request"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Validation error"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),
    INVALID_FILE_FORMAT(HttpStatus.BAD_REQUEST, "Invalid file format. Only jpg, jpeg, png, webp are allowed"),
    FILE_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE, "File size exceeds the 20MB limit"),
    AUDIENCE_USER_NOT_FRIEND(HttpStatus.BAD_REQUEST, "One or more users in the audience are not your friends"),
    PHOTO_ACCESS_DENIED(HttpStatus.FORBIDDEN, "You do not have permission to access this photo"),
    PHOTO_NOT_OWNER(HttpStatus.FORBIDDEN, "You are not the owner of this photo"),
    PHOTO_NOT_FOUND(HttpStatus.NOT_FOUND, "Photo not found"),
    STORAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload file to storage provider"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");

    private final HttpStatus status;
    private final String message;
}

