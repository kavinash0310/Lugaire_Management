package com.ecommerce.commerceapi.purchases.api;

import com.ecommerce.commerceapi.purchases.domain.PurchasePaymentStatus;
import com.ecommerce.commerceapi.purchases.domain.PurchaseStatus;
import com.ecommerce.commerceapi.purchases.service.PurchaseService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/v1/purchases")
public class PurchaseController {
    private final PurchaseService service;

    public PurchaseController(PurchaseService service) {
        this.service = service;
    }

    @GetMapping
    public PurchasePageResponse list(@RequestParam(defaultValue = "") String search,
                                     @RequestParam(required = false) Long supplierId,
                                     @RequestParam(required = false) PurchaseStatus status,
                                     @RequestParam(required = false) PurchasePaymentStatus paymentStatus,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        return service.list(search, supplierId, status, paymentStatus, fromDate, toDate, page, size);
    }

    @GetMapping("/{id}")
    public PurchaseDetailResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseResponse create(@Valid @RequestBody PurchaseRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public PurchaseResponse update(@PathVariable Long id, @Valid @RequestBody PurchaseRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public PurchaseResponse updateStatus(@PathVariable Long id, @Valid @RequestBody PurchaseStatusRequest request) {
        return service.updateStatus(id, request);
    }

    @PatchMapping("/{id}/payment-status")
    public PurchaseResponse updatePaymentStatus(@PathVariable Long id, @Valid @RequestBody PurchasePaymentStatusRequest request) {
        return service.updatePaymentStatus(id, request);
    }
}
