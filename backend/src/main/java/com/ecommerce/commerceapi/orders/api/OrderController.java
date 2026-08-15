package com.ecommerce.commerceapi.orders.api;

import com.ecommerce.commerceapi.orders.service.OrderService;
import com.ecommerce.commerceapi.orders.domain.OrderStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final OrderService service;

    public OrderController(OrderService service) { this.service = service; }

    @GetMapping("/{id}")
    public OrderDetailResponse findById(@PathVariable Long id) {
        return service.findDetailById(id);
    }

    @GetMapping
    public OrderPageResponse list(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) Long marketplaceId,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.list(search, marketplaceId, status, page, Math.min(size, 100));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody OrderRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public OrderResponse update(@PathVariable Long id, @Valid @RequestBody OrderUpdateRequest request) {
        return service.update(id, request);
    }

    @org.springframework.web.bind.annotation.PatchMapping("/{id}/status")
    public OrderResponse updateStatus(@PathVariable Long id, @Valid @RequestBody OrderStatusRequest request) {
        return service.updateStatus(id, request.status());
    }

    @org.springframework.web.bind.annotation.PatchMapping("/{id}/payment-status")
    public OrderResponse updatePaymentStatus(@PathVariable Long id, @Valid @RequestBody PaymentStatusRequest request) {
        return service.updatePaymentStatus(id, request.status());
    }
}
