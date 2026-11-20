package com.minh.locket_clone_backend.user.controller;

import com.minh.locket_clone_backend.common.response.BaseResponse;
import com.minh.locket_clone_backend.infrastructure.storage.service.StorageService;
import com.minh.locket_clone_backend.user.dto.AvatarUploadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users/me/avatar")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "Endpoints for managing user profiles")
public class AvatarUploadController {

    private final StorageService storageService;

    @Operation(summary = "Upload an avatar image",
            description = "Uploads a JPEG, PNG or WebP image of at most 5 MiB to Cloudinary and returns its HTTPS URL. "
                    + "Call PATCH /api/v1/users/me with avatarUrl to save it to the profile.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Avatar uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Missing, empty or invalid image file"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "413", description = "Image exceeds the upload size limit"),
            @ApiResponse(responseCode = "415", description = "Request must use multipart/form-data"),
            @ApiResponse(responseCode = "500", description = "Storage upload failed")
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponse<AvatarUploadResponse>> uploadAvatar(
            @RequestPart("image") MultipartFile image) {
        String avatarUrl = storageService.uploadAvatar(image);
        return ResponseEntity.ok(BaseResponse.success(new AvatarUploadResponse(avatarUrl)));
    }
}
