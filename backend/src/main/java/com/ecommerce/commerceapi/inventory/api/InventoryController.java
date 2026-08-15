package com.ecommerce.commerceapi.inventory.api;

import com.ecommerce.commerceapi.inventory.service.InventoryService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {
    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @GetMapping
    public InventoryPageResponse list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) InventoryStatus status,
            @RequestParam(required = false) InventorySort sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.list(query, status, sortBy, page, size);
    }

    @GetMapping("/low-stock")
    public InventoryPageResponse lowStock(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) InventorySort sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.lowStock(query, sortBy, page, size);
    }

    @GetMapping("/out-of-stock")
    public InventoryPageResponse outOfStock(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) InventorySort sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.outOfStock(query, sortBy, page, size);
    }

    @GetMapping("/{variantId}")
    public Map<String, Integer> balance(@PathVariable Long variantId) {
        return Map.of("currentStock", service.balance(variantId));
    }

    @GetMapping("/{variantId}/detail")
    public InventoryDetailResponse detail(@PathVariable Long variantId) {
        return service.detail(variantId);
    }

    @GetMapping("/{variantId}/movements")
    public java.util.List<InventoryMovementResponse> movements(@PathVariable Long variantId) {
        return service.movements(variantId);
    }

    @PostMapping("/adjustments")
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryMovementResponse adjust(@Valid @RequestBody InventoryAdjustmentRequest request) {
        return service.adjust(request);
    }

    @PostMapping("/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryMovementResponse record(@Valid @RequestBody InventoryTransactionRequest request) {
        return service.record(request);
    }
}
