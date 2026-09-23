package com.minh.locket_clone_backend.widget.controller;

import com.minh.locket_clone_backend.auth.security.CustomUserDetails;
import com.minh.locket_clone_backend.widget.dto.WidgetItemResponse;
import com.minh.locket_clone_backend.widget.service.WidgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/widget")
@RequiredArgsConstructor
@Tag(name = "Widget", description = "Endpoints for home screen widget")
public class WidgetController {

    private final WidgetService widgetService;

    @Operation(summary = "Get widget photos", description = "Retrieves the most recent eligible photo from each friend for the home screen widget.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Widget photos retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<List<WidgetItemResponse>> getWidgetPhotos(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<WidgetItemResponse> response = widgetService.getWidgetPhotos(userDetails.userId());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
