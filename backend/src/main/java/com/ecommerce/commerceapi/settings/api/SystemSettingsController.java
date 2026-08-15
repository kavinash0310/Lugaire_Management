package com.ecommerce.commerceapi.settings.api;

import com.ecommerce.commerceapi.settings.service.SystemSettingsService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settings")
public class SystemSettingsController {
    private final SystemSettingsService settings;

    public SystemSettingsController(SystemSettingsService settings) {
        this.settings = settings;
    }

    @GetMapping
    public List<SettingResponse> list() {
        return settings.list();
    }

    @GetMapping("/{key}")
    public SettingResponse findByKey(@PathVariable String key) {
        return settings.findByKey(key);
    }

    @PutMapping("/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public SettingResponse update(@PathVariable String key, @Valid @RequestBody SettingUpdateRequest request) {
        return settings.update(key, request);
    }
}
