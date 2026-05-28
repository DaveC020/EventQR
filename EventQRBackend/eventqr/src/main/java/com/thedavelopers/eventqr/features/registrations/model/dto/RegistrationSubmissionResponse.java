package com.thedavelopers.eventqr.features.registrations.model.dto;

import com.thedavelopers.eventqr.shared.interfaces.QrCredentialPort.QrCredentialSnapshot;

public record RegistrationSubmissionResponse(RegistrationResponse registration, QrCredentialSnapshot qrCredential) {
}
