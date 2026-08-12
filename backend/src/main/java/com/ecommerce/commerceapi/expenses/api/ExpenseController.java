package com.ecommerce.commerceapi.expenses.api;

import com.ecommerce.commerceapi.expenses.domain.ExpensePaymentMethod;
import com.ecommerce.commerceapi.expenses.domain.ExpensePaymentStatus;
import com.ecommerce.commerceapi.expenses.service.ExpenseService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
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
@RequestMapping("/api/v1/expenses")
public class ExpenseController {
    private final ExpenseService service;

    public ExpenseController(ExpenseService service) {
        this.service = service;
    }

    @GetMapping
    public ExpensePageResponse list(@RequestParam(defaultValue = "") String search,
                                    @RequestParam(required = false) Long categoryId,
                                    @RequestParam(required = false) Long supplierId,
                                    @RequestParam(required = false) ExpensePaymentStatus paymentStatus,
                                    @RequestParam(required = false) ExpensePaymentMethod paymentMethod,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                                    @RequestParam(required = false) BigDecimal minAmount,
                                    @RequestParam(required = false) BigDecimal maxAmount,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        return service.list(search, categoryId, supplierId, paymentStatus, paymentMethod, fromDate, toDate, minAmount, maxAmount, page, size);
    }

    @GetMapping("/{id}")
    public ExpenseResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse create(@Valid @RequestBody ExpenseRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public ExpenseResponse update(@PathVariable Long id, @Valid @RequestBody ExpenseRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/payment-status")
    public ExpenseResponse updatePaymentStatus(@PathVariable Long id, @Valid @RequestBody ExpensePaymentStatusRequest request) {
        return service.updatePaymentStatus(id, request);
    }
}
