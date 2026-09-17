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
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");

    private final HttpStatus status;
    private final String message;
}

