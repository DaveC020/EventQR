package com.thedavelopers.eventqr.features.eventrequests.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.thedavelopers.eventqr.features.eventrequests.model.entity.EventCreationRequest;

public interface EventCreationRequestRepository extends JpaRepository<EventCreationRequest, UUID> {

    List<EventCreationRequest> findByRequesterUserIdOrderByCreatedAtDesc(UUID requesterUserId);

    List<EventCreationRequest> findAllByOrderByCreatedAtDesc();
}
