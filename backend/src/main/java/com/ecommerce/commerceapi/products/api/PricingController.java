package com.ecommerce.commerceapi.products.api;

import com.ecommerce.commerceapi.products.service.PricingService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pricing")
public class PricingController {
    private final PricingService service;

    public PricingController(PricingService service) {
        this.service = service;
    }

    @GetMapping
    public PricingPageResponse list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) PricingSort sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.list(query, brandId, categoryId, sortBy, page, size);
    }

    @GetMapping("/{variantId}")
    public PricingDetailResponse detail(@PathVariable Long variantId) {
        return service.detail(variantId);
    }

    @GetMapping("/{variantId}/history")
    public List<PricingHistoryResponse> history(@PathVariable Long variantId) {
        return service.history(variantId);
    }

    @PutMapping("/{variantId}")
    public PricingDetailResponse update(@PathVariable Long variantId, @Valid @RequestBody PricingUpdateRequest request) {
        return service.update(variantId, request);
    }
}
