package com.thedavelopers.eventqr.features.notifications.controller;

import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.thedavelopers.eventqr.features.auditlogs.service.AuditLogService;
import com.thedavelopers.eventqr.features.notifications.model.dto.NotificationRequest;
import com.thedavelopers.eventqr.features.notifications.model.dto.NotificationResponse;
import com.thedavelopers.eventqr.features.notifications.service.NotificationService;
import com.thedavelopers.eventqr.features.users.service.UserService;
import com.thedavelopers.eventqr.shared.constants.AccountRole;
import com.thedavelopers.eventqr.shared.response.ApiResponse;
import com.thedavelopers.eventqr.shared.security.JwtService;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final JwtService jwtService;
    private final AuditLogService auditLogService;
    private final UserService userService;

    public NotificationController(NotificationService notificationService, JwtService jwtService,
                                  AuditLogService auditLogService, UserService userService) {
        this.notificationService = notificationService;
        this.jwtService = jwtService;
        this.auditLogService = auditLogService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<NotificationResponse>> create(HttpServletRequest servletRequest,
                                                                    @Valid @RequestBody NotificationRequest request) {
        NotificationResponse response = notificationService.create(request);
        UUID userId = jwtService.extractUserIdFromBearer(servletRequest.getHeader("Authorization"));
        if (jwtService.extractRoleFromBearer(servletRequest.getHeader("Authorization")) == AccountRole.ADMIN) {
            auditLogService.log(
                    "NOTIFICATION_BROADCAST",
                    request.title(),
                    userId,
                    userService.findOne(userId).fullName(),
                    request.eventId(),
                    request.recipientUserId());
        }
        return ResponseEntity.ok(ApiResponse.success("Notification created", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> mine(HttpServletRequest request) {
        UUID userId = jwtService.extractUserIdFromBearer(request.getHeader("Authorization"));
        return ResponseEntity.ok(ApiResponse.success(notificationService.findByRecipient(userId)));
    }

    @GetMapping("/{notificationId}")
    public ResponseEntity<ApiResponse<NotificationResponse>> findOne(@PathVariable UUID notificationId) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.findOne(notificationId)));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markRead(@PathVariable UUID notificationId) {
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", notificationService.markRead(notificationId)));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllRead(HttpServletRequest request) {
        UUID userId = jwtService.extractUserIdFromBearer(request.getHeader("Authorization"));
        notificationService.markAllRead(userId);
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", null));
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID notificationId) {
        notificationService.delete(notificationId);
        return ResponseEntity.ok(ApiResponse.success("Notification deleted", null));
    }

    @GetMapping("/recipient/{recipientUserId}")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> findByRecipient(@PathVariable UUID recipientUserId) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.findByRecipient(recipientUserId)));
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> findByEvent(@PathVariable UUID eventId) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.findByEvent(eventId)));
    }
}
