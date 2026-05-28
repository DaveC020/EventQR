package com.thedavelopers.eventqr.features.rewards.controller;

import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.thedavelopers.eventqr.features.organizer.model.dto.TransactionRuleRequest;
import com.thedavelopers.eventqr.features.rewards.model.dto.PointBalanceResponse;
import com.thedavelopers.eventqr.features.rewards.model.dto.PointRuleRequest;
import com.thedavelopers.eventqr.features.rewards.model.dto.RewardRedemptionResponse;
import com.thedavelopers.eventqr.features.rewards.model.dto.RewardRequest;
import com.thedavelopers.eventqr.features.rewards.model.dto.RewardResponse;
import com.thedavelopers.eventqr.features.rewards.model.entity.PointRule;
import com.thedavelopers.eventqr.features.rewards.model.entity.PointTransaction;
import com.thedavelopers.eventqr.features.rewards.service.RewardService;
import com.thedavelopers.eventqr.shared.constants.AccountRole;
import com.thedavelopers.eventqr.shared.response.ApiResponse;
import com.thedavelopers.eventqr.shared.security.JwtService;

@RestController
@RequestMapping("/api/v1")
public class RewardRoutesController {

    private final RewardService rewardService;
    private final JwtService jwtService;

    public RewardRoutesController(RewardService rewardService, JwtService jwtService) {
        this.rewardService = rewardService;
        this.jwtService = jwtService;
    }

    @GetMapping("/events/{eventId}/rewards")
    public ResponseEntity<ApiResponse<List<RewardResponse>>> eventRewards(@PathVariable UUID eventId) {
        return ResponseEntity.ok(ApiResponse.success(rewardService.findRewards(eventId)));
    }

    @GetMapping("/events/{eventId}/rewards/{rewardId}")
    public ResponseEntity<ApiResponse<RewardResponse>> eventReward(@PathVariable UUID eventId, @PathVariable UUID rewardId) {
        return ResponseEntity.ok(ApiResponse.success(rewardService.findReward(eventId, rewardId)));
    }

    @GetMapping("/organizer/events/{eventId}/rewards")
    public ResponseEntity<ApiResponse<List<RewardResponse>>> organizerRewards(HttpServletRequest request,
                                                                              @PathVariable UUID eventId) {
        requireNonAttendee(request);
        return ResponseEntity.ok(ApiResponse.success(rewardService.findRewards(eventId)));
    }

    @PostMapping("/organizer/events/{eventId}/rewards")
    public ResponseEntity<ApiResponse<RewardResponse>> createReward(HttpServletRequest request,
                                                                    @PathVariable UUID eventId,
                                                                    @Valid @RequestBody RewardRequest body) {
        requireNonAttendee(request);
        RewardRequest normalized = new RewardRequest(eventId, body.name(), body.pointsRequired(), body.stockQuantity());
        return ResponseEntity.ok(ApiResponse.success("Reward created", rewardService.saveReward(normalized)));
    }

    @PatchMapping("/organizer/events/{eventId}/rewards/{rewardId}")
    public ResponseEntity<ApiResponse<RewardResponse>> updateReward(HttpServletRequest request,
                                                                    @PathVariable UUID eventId,
                                                                    @PathVariable UUID rewardId,
                                                                    @Valid @RequestBody RewardRequest body) {
        requireNonAttendee(request);
        RewardRequest normalized = new RewardRequest(eventId, body.name(), body.pointsRequired(), body.stockQuantity());
        return ResponseEntity.ok(ApiResponse.success("Reward updated", rewardService.updateReward(eventId, rewardId, normalized)));
    }

    @DeleteMapping("/organizer/events/{eventId}/rewards/{rewardId}")
    public ResponseEntity<ApiResponse<Void>> deleteReward(HttpServletRequest request,
                                                          @PathVariable UUID eventId,
                                                          @PathVariable UUID rewardId) {
        requireNonAttendee(request);
        rewardService.deleteReward(eventId, rewardId);
        return ResponseEntity.ok(ApiResponse.success("Reward deleted", null));
    }

    @GetMapping("/organizer/events/{eventId}/claimed-rewards")
    public ResponseEntity<ApiResponse<List<RewardRedemptionResponse>>> organizerClaimedRewards(HttpServletRequest request,
                                                                                               @PathVariable UUID eventId) {
        requireNonAttendee(request);
        return ResponseEntity.ok(ApiResponse.success(rewardService.findRedemptions(eventId)));
    }

    @GetMapping("/attendees/me/events/{eventId}/claimed-rewards")
    public ResponseEntity<ApiResponse<List<RewardRedemptionResponse>>> attendeeClaimedRewards(HttpServletRequest request,
                                                                                              @PathVariable UUID eventId) {
        UUID userId = currentUserId(request);
        return ResponseEntity.ok(ApiResponse.success(rewardService.findRedemptions(eventId, userId)));
    }

    @GetMapping("/organizer/events/{eventId}/point-rules")
    public ResponseEntity<ApiResponse<List<PointRule>>> pointRules(HttpServletRequest request,
                                                               @PathVariable UUID eventId) {
        requireNonAttendee(request);
        return ResponseEntity.ok(ApiResponse.success(rewardService.listPointRules(eventId)));
    }

    @PostMapping("/organizer/events/{eventId}/point-rules")
    public ResponseEntity<ApiResponse<PointRuleRequest>> createPointRule(HttpServletRequest request,
                                                                         @PathVariable UUID eventId,
                                                                         @Valid @RequestBody PointRuleRequest body) {
        requireNonAttendee(request);
        PointRuleRequest normalized = new PointRuleRequest(eventId, body.scanPurposeId(), body.points(), body.active());
        return ResponseEntity.ok(ApiResponse.success("Point rule saved", rewardService.savePointRule(normalized)));
    }

    @PatchMapping("/organizer/events/{eventId}/point-rules/{ruleId}")
    public ResponseEntity<ApiResponse<PointRuleRequest>> updatePointRule(HttpServletRequest request,
                                                                         @PathVariable UUID eventId,
                                                                         @PathVariable UUID ruleId,
                                                                         @Valid @RequestBody PointRuleRequest body) {
        requireNonAttendee(request);
        PointRuleRequest normalized = new PointRuleRequest(eventId, body.scanPurposeId(), body.points(), body.active());
        return ResponseEntity.ok(ApiResponse.success("Point rule updated", rewardService.updatePointRule(eventId, ruleId, normalized)));
    }

    @DeleteMapping("/organizer/events/{eventId}/point-rules/{ruleId}")
    public ResponseEntity<ApiResponse<Void>> deletePointRule(HttpServletRequest request,
                                                            @PathVariable UUID eventId,
                                                            @PathVariable UUID ruleId) {
        requireNonAttendee(request);
        rewardService.deletePointRule(eventId, ruleId);
        return ResponseEntity.ok(ApiResponse.success("Point rule deleted", null));
    }

    @GetMapping("/attendees/me/events/{eventId}/points")
    public ResponseEntity<ApiResponse<PointBalanceResponse>> attendeePoints(HttpServletRequest request,
                                                                         @PathVariable UUID eventId) {
        UUID userId = currentUserId(request);
        return ResponseEntity.ok(ApiResponse.success(rewardService.getBalance(eventId, userId)));
    }

    @GetMapping("/attendees/me/events/{eventId}/point-transactions")
    public ResponseEntity<ApiResponse<List<PointTransaction>>> attendeePointTransactions(HttpServletRequest request,
                                                                                            @PathVariable UUID eventId) {
        UUID userId = currentUserId(request);
        return ResponseEntity.ok(ApiResponse.success(rewardService.findPointTransactions(eventId, userId)));
    }

    @GetMapping("/organizer/events/{eventId}/point-transactions")
    public ResponseEntity<ApiResponse<List<PointTransaction>>> organizerPointTransactions(HttpServletRequest request,
                                                                                          @PathVariable UUID eventId) {
        requireNonAttendee(request);
        return ResponseEntity.ok(ApiResponse.success(rewardService.findPointTransactions(eventId)));
    }

    private UUID currentUserId(HttpServletRequest request) {
        return jwtService.extractUserIdFromBearer(request.getHeader("Authorization"));
    }

    private void requireNonAttendee(HttpServletRequest request) {
        if (jwtService.extractRoleFromBearer(request.getHeader("Authorization")) == AccountRole.ATTENDEE) {
            throw new com.thedavelopers.eventqr.shared.exceptions.ForbiddenException("Organizer or admin access required");
        }
    }
}
