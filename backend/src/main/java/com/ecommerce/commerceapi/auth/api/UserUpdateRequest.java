package com.ecommerce.commerceapi.auth.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserUpdateRequest(
        @NotBlank String name,
        @Email @NotBlank String email,
        @NotBlank String roleCode,
        boolean active) {}
