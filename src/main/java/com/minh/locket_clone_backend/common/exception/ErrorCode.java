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
    NOT_FRIENDS(HttpStatus.BAD_REQUEST, "Users are not friends"),
    FRIEND_REQUEST_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "Friend request already exists"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Validation error"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");

    private final HttpStatus status;
    private final String message;
}

