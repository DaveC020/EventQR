package com.thedavelopers.eventqr.features.scanning.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.thedavelopers.eventqr.features.scanning.model.entity.ScanPurpose;

public interface ScanPurposeRepository extends JpaRepository<ScanPurpose, UUID> {

    List<ScanPurpose> findByEventId(UUID eventId);

    Optional<ScanPurpose> findByEventIdAndCode(UUID eventId, com.thedavelopers.eventqr.shared.constants.ScanPurposeCode code);
}

