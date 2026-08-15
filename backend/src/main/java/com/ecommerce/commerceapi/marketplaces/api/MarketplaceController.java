package com.ecommerce.commerceapi.marketplaces.api;

import com.ecommerce.commerceapi.marketplaces.service.MarketplaceService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/marketplaces")
public class MarketplaceController {
    private final MarketplaceService service;

    public MarketplaceController(MarketplaceService service) {
        this.service = service;
    }

    @GetMapping
    public List<MarketplaceResponse> findAll() {
        return service.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MarketplaceResponse create(@Valid @RequestBody MarketplaceRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public MarketplaceResponse update(@PathVariable Long id, @Valid @RequestBody MarketplaceRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/active")
    public MarketplaceResponse updateActive(@PathVariable Long id, @RequestBody ActiveStatusRequest request) {
        return service.updateActive(id, request.active());
    }
}
