package com.ecommerce.commerceapi.settings.api;

import com.ecommerce.commerceapi.settings.domain.SettingType;
import java.time.Instant;

public record SettingResponse(
        String key,
        String value,
        SettingType settingType,
        String description,
        Instant updatedAt) {
}
