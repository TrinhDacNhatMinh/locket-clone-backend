package com.minh.locket_clone_backend.photo.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.minh.locket_clone_backend.common.exception.BusinessException;
import com.minh.locket_clone_backend.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryStorageService implements StorageService {

    private static final long MAX_FILE_SIZE_BYTES = 20L * 1024 * 1024; // 20MB

    private static final Set<String> ALLOWED_MAGIC_BYTES_PREFIXES = Set.of(
            "FFD8FF",   // JPEG
            "89504E47", // PNG
            "52494646"  // WEBP (RIFF container header)
    );

    private final Cloudinary cloudinary;

    @Override
    public String uploadImage(MultipartFile file) {
        validateNotEmpty(file);
        validateSize(file);
        validateFormatByMagicBytes(file);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap("resource_type", "image")
            );

            String imageUrl = (String) uploadResult.get("secure_url");
            log.info("Image uploaded to Cloudinary, publicId={}, bytes={}", uploadResult.get("public_id"), file.getSize());
            return imageUrl;

        } catch (IOException e) {
            log.error("Failed to upload image to Cloudinary", e);
            throw new BusinessException(ErrorCode.STORAGE_UPLOAD_FAILED);
        }
    }

    private void validateNotEmpty(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_FILE_FORMAT, "Image file must not be empty");
        }
    }

    private void validateSize(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            log.warn("Upload rejected: file size {} bytes exceeds safety-net limit of {} bytes "
                            + "— check client-side compression",
                    file.getSize(), MAX_FILE_SIZE_BYTES);
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }
    }

    // Validates the file is a genuine image by inspecting its magic bytes
    private void validateFormatByMagicBytes(MultipartFile file) {
        byte[] header = new byte[4];
        try (InputStream inputStream = file.getInputStream()) {
            int bytesRead = inputStream.read(header);
            if (bytesRead < 4) {
                throw new BusinessException(ErrorCode.INVALID_FILE_FORMAT);
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INVALID_FILE_FORMAT);
        }

        String hex = bytesToHex(header);
        boolean valid = ALLOWED_MAGIC_BYTES_PREFIXES.stream().anyMatch(hex::startsWith);
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