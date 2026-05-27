package com.thedavelopers.eventqr.features.uploads.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.thedavelopers.eventqr.shared.response.ApiResponse;

@RestController
@RequestMapping("/api/v1")
public class UploadController {

    @PostMapping("/uploads/event-logo")
    public ResponseEntity<ApiResponse<Void>> uploadEventLogo(@RequestParam("file") MultipartFile file) {
        return notImplemented("File upload storage is not wired yet");
    }

    @PostMapping("/uploads/id-template-assets")
    public ResponseEntity<ApiResponse<Void>> uploadTemplateAsset(@RequestParam("file") MultipartFile file) {
        return notImplemented("File upload storage is not wired yet");
    }

    @PostMapping("/uploads/profile-photo")
    public ResponseEntity<ApiResponse<Void>> uploadProfilePhoto(@RequestParam("file") MultipartFile file) {
        return notImplemented("File upload storage is not wired yet");
    }

    @GetMapping("/files/{fileId}")
    public ResponseEntity<ApiResponse<Void>> getFile(@PathVariable UUID fileId) {
        return notImplemented("File retrieval storage is not wired yet");
    }

    @DeleteMapping("/files/{fileId}")
    public ResponseEntity<ApiResponse<Void>> deleteFile(@PathVariable UUID fileId) {
        return notImplemented("File deletion storage is not wired yet");
    }

    private ResponseEntity<ApiResponse<Void>> notImplemented(String message) {
        return ResponseEntity.status(501).body(new ApiResponse<>(false, message, null, java.time.Instant.now()));
    }
}