package com.ecommerce.commerceapi.masters.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MasterDataRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 20) String code,
        @Size(max = 30) String groupName,
        @Size(max = 30) String sizeType) {}
