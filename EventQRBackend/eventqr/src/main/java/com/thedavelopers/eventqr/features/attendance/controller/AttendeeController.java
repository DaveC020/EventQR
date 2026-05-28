package com.thedavelopers.eventqr.features.attendance.controller;

import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.thedavelopers.eventqr.features.registrations.model.dto.RegistrationResponse;
import com.thedavelopers.eventqr.features.registrations.service.RegistrationService;
import com.thedavelopers.eventqr.features.transactions.model.dto.TransactionResponse;
import com.thedavelopers.eventqr.features.transactions.service.TransactionService;
import com.thedavelopers.eventqr.shared.constants.TransactionType;
import com.thedavelopers.eventqr.shared.response.ApiResponse;
import com.thedavelopers.eventqr.shared.security.JwtService;

@RestController
@RequestMapping("/api/v1/attendees")
public class AttendeeController {

    private final RegistrationService registrationService;
    private final TransactionService transactionService;
    private final JwtService jwtService;

    public AttendeeController(RegistrationService registrationService, TransactionService transactionService, JwtService jwtService) {
        this.registrationService = registrationService;
        this.transactionService = transactionService;
        this.jwtService = jwtService;
    }

    @GetMapping("/me/transactions")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> myTransactions(HttpServletRequest request) {
        UUID userId = jwtService.extractUserIdFromBearer(request.getHeader("Authorization"));
        return ResponseEntity.ok(ApiResponse.success(transactionService.findByAttendee(userId)));
    }

    @GetMapping("/me/events/{eventId}/transactions")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> myEventTransactions(HttpServletRequest request,
                                                                                      @PathVariable UUID eventId) {
        UUID userId = jwtService.extractUserIdFromBearer(request.getHeader("Authorization"));
        return ResponseEntity.ok(ApiResponse.success(transactionService.findByEventAndAttendee(eventId, userId)));
    }

    @GetMapping("/me/events/{eventId}/status")
    public ResponseEntity<ApiResponse<RegistrationResponse>> myEventStatus(HttpServletRequest request,
                                                                          @PathVariable UUID eventId) {
        UUID userId = jwtService.extractUserIdFromBearer(request.getHeader("Authorization"));
        RegistrationResponse registration = registrationService.findByAttendeeUserId(userId).stream()
                .filter(item -> item.eventId().equals(eventId))
                .findFirst()
                .orElseThrow(() -> new com.thedavelopers.eventqr.shared.exceptions.ResourceNotFoundException("Registration not found for event"));
        return ResponseEntity.ok(ApiResponse.success(registration));
    }

    @GetMapping("/me/events/{eventId}/benefit-claims")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> myBenefitClaims(HttpServletRequest request,
                                                                                @PathVariable UUID eventId) {
        UUID userId = jwtService.extractUserIdFromBearer(request.getHeader("Authorization"));
        List<TransactionResponse> claims = transactionService.findByEventAndAttendee(eventId, userId).stream()
                .filter(transaction -> transaction.transactionType() == TransactionType.BENEFIT_CLAIM)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(claims));
    }
}
