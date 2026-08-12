package com.ecommerce.commerceapi.suppliers.api;

public record SupplierResponse(Long id, String supplierId, String name, String contactPerson, String phone,
                               String email, String address, String city, String state, String gstin, String notes,
                               boolean active) {}
