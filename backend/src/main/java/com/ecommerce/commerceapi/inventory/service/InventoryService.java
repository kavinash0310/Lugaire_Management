package com.ecommerce.commerceapi.inventory.service;

import com.ecommerce.commerceapi.inventory.api.InventoryTransactionRequest;
import com.ecommerce.commerceapi.inventory.domain.InventoryTransaction;
import com.ecommerce.commerceapi.inventory.repository.InventoryTransactionRepository;
import com.ecommerce.commerceapi.products.domain.ProductVariant;
import com.ecommerce.commerceapi.products.repository.ProductVariantRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {
    private final InventoryTransactionRepository transactions;
    private final ProductVariantRepository variants;

    public InventoryService(InventoryTransactionRepository transactions, ProductVariantRepository variants) {
        this.transactions = transactions;
        this.variants = variants;
    }

    @Transactional
    public void record(InventoryTransactionRequest request) {
        ProductVariant variant = variants.findByIdForInventoryUpdate(request.variantId())
                .orElseThrow(() -> new EntityNotFoundException("Variant not found"));
        if (!variant.isActive()) {
            throw new IllegalArgumentException("Variant is inactive");
        }

        int balance = transactions.balance(request.variantId());
        if (!request.transactionType().isInbound() && balance < request.quantity()) {
            throw new IllegalArgumentException("Insufficient sellable stock");
        }

        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setVariant(variant);
        transaction.setTransactionType(request.transactionType());
        transaction.setQuantity(request.quantity());
        transaction.setUnitCost(request.unitCost());
        transaction.setReferenceType(request.referenceType());
        transaction.setReferenceId(request.referenceId());
        transaction.setNotes(request.notes());
        transaction.setTransactionDate(request.transactionDate());
        transactions.save(transaction);
    }

    public int balance(Long variantId) {
        return transactions.balance(variantId);
    }
}
