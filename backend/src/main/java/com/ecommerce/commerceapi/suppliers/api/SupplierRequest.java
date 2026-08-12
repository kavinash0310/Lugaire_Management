package com.ecommerce.commerceapi.suppliers.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SupplierRequest(
        @NotBlank String supplierId,
        @NotBlank String name,
        String contactPerson,
        String phone,
        @Email String email,
        String address,
        String city,
        String state,
        String gstin,
        String notes) {}
