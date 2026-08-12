package com.ecommerce.commerceapi.suppliers.service;

import com.ecommerce.commerceapi.suppliers.api.ActiveStatusRequest;
import com.ecommerce.commerceapi.suppliers.api.SupplierPageResponse;
import com.ecommerce.commerceapi.suppliers.api.SupplierRequest;
import com.ecommerce.commerceapi.suppliers.api.SupplierResponse;
import com.ecommerce.commerceapi.suppliers.domain.Supplier;
import com.ecommerce.commerceapi.suppliers.repository.SupplierRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupplierService {
    private final SupplierRepository suppliers;

    public SupplierService(SupplierRepository suppliers) {
        this.suppliers = suppliers;
    }

    @Transactional(readOnly = true)
    public SupplierPageResponse list(String search, Boolean active, int page, int size) {
        var results = suppliers.search(search == null ? "" : search, active, PageRequest.of(page, Math.min(size, 100)));
        return new SupplierPageResponse(
                results.map(this::response).toList(),
                results.getNumber(),
                results.getSize(),
                results.getTotalElements(),
                results.getTotalPages());
    }

    @Transactional(readOnly = true)
    public SupplierResponse findById(Long id) {
        return response(suppliers.findById(id).orElseThrow(() -> new EntityNotFoundException("Supplier not found")));
    }

    @Transactional
    public SupplierResponse create(SupplierRequest request) {
        if (suppliers.findBySupplierIdIgnoreCase(request.supplierId().trim()).isPresent()) {
            throw new IllegalArgumentException("Supplier ID already exists");
        }
        Supplier supplier = new Supplier();
        apply(supplier, request);
        return response(suppliers.save(supplier));
    }

    @Transactional
    public SupplierResponse update(Long id, SupplierRequest request) {
        Supplier supplier = suppliers.findById(id).orElseThrow(() -> new EntityNotFoundException("Supplier not found"));
        apply(supplier, request);
        return response(suppliers.save(supplier));
    }

    @Transactional
    public SupplierResponse updateActive(Long id, boolean active) {
        Supplier supplier = suppliers.findById(id).orElseThrow(() -> new EntityNotFoundException("Supplier not found"));
        supplier.setActive(active);
        return response(suppliers.save(supplier));
    }

    private void apply(Supplier supplier, SupplierRequest request) {
        supplier.setSupplierId(request.supplierId().trim());
        supplier.setName(request.name().trim());
        supplier.setContactPerson(request.contactPerson());
        supplier.setPhone(request.phone());
        supplier.setEmail(request.email());
        supplier.setAddress(request.address());
        supplier.setCity(request.city());
        supplier.setState(request.state());
        supplier.setGstin(request.gstin());
        supplier.setNotes(request.notes());
    }

    private SupplierResponse response(Supplier supplier) {
        return new SupplierResponse(supplier.getId(), supplier.getSupplierId(), supplier.getName(), supplier.getContactPerson(),
                supplier.getPhone(), supplier.getEmail(), supplier.getAddress(), supplier.getCity(), supplier.getState(),
                supplier.getGstin(), supplier.getNotes(), supplier.isActive());
    }
}
