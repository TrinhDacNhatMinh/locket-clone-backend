package com.minh.locket_clone_backend.photo.controller;

import com.minh.locket_clone_backend.common.dto.CursorPagedResponse;
import com.minh.locket_clone_backend.photo.dto.PhotoResponse;
import com.minh.locket_clone_backend.photo.entity.AudienceType;
import com.minh.locket_clone_backend.photo.service.PhotoService;
import com.minh.locket_clone_backend.auth.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/photos")
@RequiredArgsConstructor
@Validated
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

    @Operation(summary = "Delete a photo", description = "Soft deletes a photo. Only the owner can delete their photo.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Photo deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not the owner of the photo"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @DeleteMapping("/{photoId}")
    public ResponseEntity<Void> deletePhoto(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID photoId
    ) {
        photoService.deletePhoto(userDetails.userId(), photoId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Get current user's photos", description = "Retrieves all photos posted by the current user with cursor-based pagination.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Photos retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error (invalid limit)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/me")
    public ResponseEntity<CursorPagedResponse<PhotoResponse>> getMyPhotos(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "limit", defaultValue = "10") @Min(1) @Max(50) int limit
    ) {
        var response = photoService.getMyPhotos(userDetails.userId(), cursor, limit);
        return ResponseEntity.ok(response);
    }
}
