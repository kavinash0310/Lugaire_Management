package com.ecommerce.commerceapi.settlements.api;

import com.ecommerce.commerceapi.orders.domain.OrderPlatform;
import com.ecommerce.commerceapi.settlements.domain.ReconciliationStatus;
import com.ecommerce.commerceapi.settlements.domain.SettlementStatus;
import com.ecommerce.commerceapi.settlements.service.SettlementService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settlements")
public class SettlementController {
    private final SettlementService service;

    public SettlementController(SettlementService service) {
        this.service = service;
    }

    @GetMapping
    public SettlementPageResponse list(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) OrderPlatform platform,
            @RequestParam(required = false) SettlementStatus status,
            @RequestParam(required = false) ReconciliationStatus reconciliationStatus,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.list(search, platform, status, reconciliationStatus, fromDate, toDate, page, size);
    }

    @GetMapping("/{id}")
    public SettlementDetailResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SettlementDetailResponse create(@Valid @RequestBody SettlementRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public SettlementDetailResponse update(@PathVariable Long id, @Valid @RequestBody SettlementRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public SettlementDetailResponse updateStatus(@PathVariable Long id, @Valid @RequestBody SettlementStatusRequest request) {
        return service.updateStatus(id, request);
    }

    @PatchMapping("/{id}/reconcile")
    public SettlementDetailResponse reconcile(@PathVariable Long id) {
        return service.reconcile(id);
    }
}
