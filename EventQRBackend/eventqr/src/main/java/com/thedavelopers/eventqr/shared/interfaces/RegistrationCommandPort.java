package com.thedavelopers.eventqr.shared.interfaces;

import java.util.UUID;

public interface RegistrationCommandPort {

    void markEntered(UUID registrationId);

    void markExited(UUID registrationId);

    void markAttended(UUID registrationId);

    void setQrCredentialId(UUID registrationId, UUID qrCredentialId);

    void addPoints(UUID registrationId, int points);
}