package com.ecommerce.commerceapi.suppliers.service;

import com.ecommerce.commerceapi.suppliers.api.ActiveStatusRequest;
import com.ecommerce.commerceapi.suppliers.api.SupplierPageResponse;
import com.ecommerce.commerceapi.suppliers.api.SupplierRequest;
import com.ecommerce.commerceapi.suppliers.api.SupplierResponse;
import com.ecommerce.commerceapi.suppliers.domain.Supplier;
import com.ecommerce.commerceapi.suppliers.repository.SupplierRepository;
import com.ecommerce.commerceapi.audit.service.AuditLogService;
import jakarta.persistence.EntityNotFoundException;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupplierService {
    private final SupplierRepository suppliers;
    private final AuditLogService auditLogs;

    public SupplierService(SupplierRepository suppliers, AuditLogService auditLogs) {
        this.suppliers = suppliers;
        this.auditLogs = auditLogs;
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
        Supplier saved = suppliers.save(supplier);
        auditLogs.log(
                "CREATE",
                "SUPPLIER",
                "Supplier",
                String.valueOf(saved.getId()),
                "Supplier created",
                null,
                Map.of("supplierId", saved.getSupplierId(), "name", saved.getName(), "active", String.valueOf(saved.isActive())));
        return response(saved);
    }

    @Transactional
    public SupplierResponse update(Long id, SupplierRequest request) {
        Supplier supplier = suppliers.findById(id).orElseThrow(() -> new EntityNotFoundException("Supplier not found"));
        Map<String, Object> oldValue = Map.of("supplierId", supplier.getSupplierId(), "name", supplier.getName(), "active", String.valueOf(supplier.isActive()));
        apply(supplier, request);
        Supplier saved = suppliers.save(supplier);
        auditLogs.log(
                "UPDATE",
                "SUPPLIER",
                "Supplier",
                String.valueOf(saved.getId()),
                "Supplier updated",
                oldValue,
                Map.of("supplierId", saved.getSupplierId(), "name", saved.getName(), "active", String.valueOf(saved.isActive())));
        return response(saved);
    }

    @Transactional
    public SupplierResponse updateActive(Long id, boolean active) {
        Supplier supplier = suppliers.findById(id).orElseThrow(() -> new EntityNotFoundException("Supplier not found"));
        boolean previous = supplier.isActive();
        supplier.setActive(active);
        Supplier saved = suppliers.save(supplier);
        auditLogs.log(
                active ? "ACTIVATE" : "DEACTIVATE",
                "SUPPLIER",
                "Supplier",
                String.valueOf(saved.getId()),
                "Supplier " + (active ? "activated" : "deactivated"),
                Map.of("active", String.valueOf(previous)),
                Map.of("active", String.valueOf(saved.isActive())));
        return response(saved);
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
