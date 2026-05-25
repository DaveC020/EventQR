package com.thedavelopers.eventqr.features.users.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.thedavelopers.eventqr.shared.constants.AccountRole;

public record UserRequest(@NotBlank @Email String email, @NotBlank String fullName, String phoneNumber,
                          @NotBlank @Size(min = 8) String password, @NotNull AccountRole role) {
}