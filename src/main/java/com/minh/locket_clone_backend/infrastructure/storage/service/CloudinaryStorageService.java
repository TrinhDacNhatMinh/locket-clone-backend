package com.minh.locket_clone_backend.infrastructure.storage.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.minh.locket_clone_backend.common.exception.BusinessException;
import com.minh.locket_clone_backend.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryStorageService implements StorageService {

    private static final long MAX_FILE_SIZE_BYTES = 20L * 1024 * 1024; // 20MB
    private static final long MAX_AVATAR_SIZE_BYTES = 5L * 1024 * 1024;

    private final Cloudinary cloudinary;

    @Override
    public String uploadImage(MultipartFile file) {
        return upload(file, MAX_FILE_SIZE_BYTES, ObjectUtils.asMap("resource_type", "image"));
    }

    @Override
    public String uploadAvatar(MultipartFile file) {
        return upload(file, MAX_AVATAR_SIZE_BYTES,
                ObjectUtils.asMap("resource_type", "image", "folder", "momento/avatars"));
    }

    private String upload(MultipartFile file, long maxSizeBytes, Map<String, Object> options) {
        validateNotEmpty(file);
        validateSize(file, maxSizeBytes);
        validateFormatByMagicBytes(file);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    options
            );

            String imageUrl = (String) uploadResult.get("secure_url");
            if (!StringUtils.hasText(imageUrl) || !imageUrl.startsWith("https://")) {
                throw new BusinessException(ErrorCode.STORAGE_UPLOAD_FAILED);
            }
            log.info("Image uploaded to Cloudinary, publicId={}, bytes={}", uploadResult.get("public_id"), file.getSize());
            return imageUrl;

        } catch (IOException | RuntimeException e) {
            log.error("Failed to upload image to Cloudinary", e);
            throw new BusinessException(ErrorCode.STORAGE_UPLOAD_FAILED);
        }
    }

    private void validateNotEmpty(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_FILE_FORMAT, "Image file must not be empty");
        }
    }

    private void validateSize(MultipartFile file, long maxSizeBytes) {
        if (file.getSize() > maxSizeBytes) {
            log.warn("Upload rejected: file size {} bytes exceeds safety-net limit of {} bytes "
                            + "— check client-side compression",
                    file.getSize(), maxSizeBytes);
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE,
                    "File size exceeds the " + maxSizeBytes / (1024 * 1024) + " MiB limit");
        }
    }

    // Validates the file is a genuine image by inspecting its magic bytes
    private void validateFormatByMagicBytes(MultipartFile file) {
        byte[] header;
        try (InputStream inputStream = file.getInputStream()) {
            header = inputStream.readNBytes(12);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INVALID_FILE_FORMAT);
        }

        String hex = bytesToHex(header);
        boolean valid = hex.startsWith("FFD8FF") // JPEG
                || hex.startsWith("89504E470D0A1A0A") // PNG
                || (header.length >= 12 && hex.startsWith("52494646") // RIFF
                    && hex.substring(16, 24).equals("57454250")); // WEBP
        if (!valid) {
            throw new BusinessException(ErrorCode.INVALID_FILE_FORMAT);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
