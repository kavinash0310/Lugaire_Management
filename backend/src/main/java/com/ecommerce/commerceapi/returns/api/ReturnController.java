package com.ecommerce.commerceapi.returns.api;

import com.ecommerce.commerceapi.returns.domain.ReturnStatus;
import com.ecommerce.commerceapi.returns.domain.ReturnType;
import com.ecommerce.commerceapi.returns.service.ReturnService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/returns")
public class ReturnController {
    private final ReturnService service;

    public ReturnController(ReturnService service) {
        this.service = service;
    }

    @GetMapping
    public Page<ReturnResponse> list(@RequestParam(defaultValue = "") String search,
                                     @RequestParam(required = false) Long marketplaceId,
                                     @RequestParam(required = false) ReturnType type,
                                     @RequestParam(required = false) ReturnStatus status,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        return service.list(search, marketplaceId, type, status, fromDate, toDate, page, size);
    }

    @GetMapping("/{id}")
    public ReturnResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReturnResponse create(@Valid @RequestBody ReturnCreateRequest request) {
        return service.create(request);
    }

    @PatchMapping("/{id}/status")
    public ReturnResponse updateStatus(@PathVariable Long id, @Valid @RequestBody ReturnStatusRequest request) {
        return service.updateStatus(id, request);
    }
}
