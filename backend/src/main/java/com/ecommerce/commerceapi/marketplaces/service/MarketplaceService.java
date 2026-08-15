package com.ecommerce.commerceapi.marketplaces.service;

import com.ecommerce.commerceapi.marketplaces.api.MarketplaceRequest;
import com.ecommerce.commerceapi.marketplaces.api.MarketplaceResponse;
import com.ecommerce.commerceapi.marketplaces.domain.Marketplace;
import com.ecommerce.commerceapi.marketplaces.repository.MarketplaceRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketplaceService {
    private final MarketplaceRepository repository;

    public MarketplaceService(MarketplaceRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<MarketplaceResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Marketplace requireMarketplace(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Marketplace not found"));
    }

    @Transactional(readOnly = true)
    public Marketplace requireActiveMarketplace(Long id) {
        Marketplace marketplace = requireMarketplace(id);
        if (!marketplace.isActive()) {
            throw new IllegalArgumentException("Marketplace is inactive");
        }
        return marketplace;
    }

    @Transactional
    public MarketplaceResponse create(MarketplaceRequest request) {
        String code = normalizeCode(request.code());
        if (repository.findByCodeIgnoreCase(code).isPresent()) {
            throw new IllegalArgumentException("Marketplace code already exists");
        }
        Marketplace marketplace = new Marketplace();
        apply(marketplace, request, code);
        return toResponse(repository.save(marketplace));
    }

    @Transactional
    public MarketplaceResponse update(Long id, MarketplaceRequest request) {
        Marketplace marketplace = requireMarketplace(id);
        String code = normalizeCode(request.code());
        if (repository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new IllegalArgumentException("Marketplace code already exists");
        }
        apply(marketplace, request, code);
        return toResponse(marketplace);
    }

    @Transactional
    public MarketplaceResponse updateActive(Long id, boolean active) {
        Marketplace marketplace = requireMarketplace(id);
        marketplace.setActive(active);
        return toResponse(marketplace);
    }

    private void apply(Marketplace marketplace, MarketplaceRequest request, String code) {
        marketplace.setName(request.name().trim());
        marketplace.setCode(code);
        marketplace.setActive(request.active());
        marketplace.setDefaultCommissionRate(request.defaultCommissionRate());
        marketplace.setDefaultShippingCharge(request.defaultShippingCharge());
        marketplace.setRemarks(request.remarks());
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private MarketplaceResponse toResponse(Marketplace marketplace) {
        return new MarketplaceResponse(
                marketplace.getId(),
                marketplace.getName(),
                marketplace.getCode(),
                marketplace.isActive(),
                marketplace.getDefaultCommissionRate(),
                marketplace.getDefaultShippingCharge(),
                marketplace.getRemarks(),
                marketplace.getCreatedAt(),
                marketplace.getUpdatedAt());
    }
}
