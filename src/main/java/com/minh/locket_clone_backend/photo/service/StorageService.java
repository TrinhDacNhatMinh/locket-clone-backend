package com.minh.locket_clone_backend.photo.service;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    /**
     * Uploads an already-compressed image file to cloud storage and returns its
     * publicly accessible URL.
     *
     * <p>Client (Android) is responsible for compressing the image (JPEG, max
     * 1080px on the longest edge, ~80% quality) before calling the upload API.
     * This method only enforces a safety-net size limit and validates the file
     * is a genuine image via magic bytes — it does not perform any server-side
     * compression or transformation.
     *
     * @throws com.minh.locket_clone_backend.common.exception.BusinessException
     *         with {@code INVALID_FILE_FORMAT} if the file is empty or not a
     *         recognized image format (checked via magic bytes, not declared
     *         content-type)
     * @throws com.minh.locket_clone_backend.common.exception.BusinessException
     *         with {@code FILE_TOO_LARGE} if the file exceeds the safety-net
     *         limit (5MB) — a properly compressed client upload should never
     *         hit this; if it does, treat it as a client bug or abuse signal
     * @throws com.minh.locket_clone_backend.common.exception.BusinessException
     *         with {@code STORAGE_UPLOAD_FAILED} if the upload to the storage
     *         provider fails (network, quota, credentials)
     */
    String uploadImage(MultipartFile file);
}
