package com.minh.locket_clone_backend.photo.controller;

import com.minh.locket_clone_backend.photo.dto.PhotoResponse;
import com.minh.locket_clone_backend.photo.entity.AudienceType;
import com.minh.locket_clone_backend.photo.service.PhotoService;
import com.minh.locket_clone_backend.auth.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/photos")
@RequiredArgsConstructor
@Tag(name = "Photo", description = "Photo management endpoints")
public class PhotoController {

    private final PhotoService photoService;

    @Operation(summary = "Upload a new photo", description = "Uploads a photo to Cloudinary and saves metadata. Audience can be ALL_FRIENDS or CUSTOM.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Photo uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid file format, size limit exceeded, or custom audience includes non-friends"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PhotoResponse> uploadPhoto(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "caption", required = false) String caption,
            @RequestParam("audienceType") AudienceType audienceType,
            @RequestParam(value = "audienceUserIds", required = false) List<UUID> audienceUserIds,
            @RequestParam(value = "metadata", required = false) String metadataJson
    ) {
        PhotoResponse response = photoService.createPhoto(userDetails.userId(), image, caption, audienceType, audienceUserIds, metadataJson);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
