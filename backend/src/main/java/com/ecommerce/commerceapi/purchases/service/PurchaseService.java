package com.ecommerce.commerceapi.purchases.service;

import com.ecommerce.commerceapi.inventory.api.InventoryTransactionRequest;
import com.ecommerce.commerceapi.inventory.domain.InventoryTransactionType;
import com.ecommerce.commerceapi.inventory.service.InventoryService;
import com.ecommerce.commerceapi.products.repository.ProductVariantRepository;
import com.ecommerce.commerceapi.purchases.api.PurchaseDetailResponse;
import com.ecommerce.commerceapi.purchases.api.PurchaseItemResponse;
import com.ecommerce.commerceapi.purchases.api.PurchasePageResponse;
import com.ecommerce.commerceapi.purchases.api.PurchasePaymentStatusRequest;
import com.ecommerce.commerceapi.purchases.api.PurchaseRequest;
import com.ecommerce.commerceapi.purchases.api.PurchaseResponse;
import com.ecommerce.commerceapi.purchases.api.PurchaseStatusRequest;
import com.ecommerce.commerceapi.purchases.domain.Purchase;
import com.ecommerce.commerceapi.purchases.domain.PurchaseItem;
import com.ecommerce.commerceapi.purchases.domain.PurchasePaymentStatus;
import com.ecommerce.commerceapi.purchases.domain.PurchaseStatus;
import com.ecommerce.commerceapi.purchases.repository.PurchaseItemRepository;
import com.ecommerce.commerceapi.purchases.repository.PurchaseRepository;
import com.ecommerce.commerceapi.suppliers.repository.SupplierRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchaseService {
    private final PurchaseRepository purchases;
    private final PurchaseItemRepository items;
    private final SupplierRepository suppliers;
    private final ProductVariantRepository variants;
    private final InventoryService inventory;

    public PurchaseService(PurchaseRepository purchases, PurchaseItemRepository items, SupplierRepository suppliers,
                           ProductVariantRepository variants, InventoryService inventory) {
        this.purchases = purchases;
        this.items = items;
        this.suppliers = suppliers;
        this.variants = variants;
        this.inventory = inventory;
    }

    @Transactional(readOnly = true)
    public PurchasePageResponse list(String search, Long supplierId, PurchaseStatus status, PurchasePaymentStatus paymentStatus,
                                     LocalDate fromDate, LocalDate toDate, int page, int size) {
        var results = purchases.search(search == null ? "" : search, supplierId, status, paymentStatus, fromDate, toDate, PageRequest.of(page, Math.min(size, 100)));
        return new PurchasePageResponse(results.map(this::response).toList(), results.getNumber(), results.getSize(), results.getTotalElements(), results.getTotalPages());
    }

    @Transactional(readOnly = true)
    public PurchaseDetailResponse findById(Long id) {
        return detail(purchases.findById(id).orElseThrow(() -> new EntityNotFoundException("Purchase not found")));
    }

    @Transactional
    public PurchaseResponse create(PurchaseRequest request) {
        if (purchases.findByPurchaseIdIgnoreCase(request.purchaseId().trim()).isPresent()) {
            throw new IllegalArgumentException("Purchase ID already exists");
        }
        var supplier = suppliers.findById(request.supplierId()).orElseThrow(() -> new EntityNotFoundException("Supplier not found"));
        if (!supplier.isActive()) {
            throw new IllegalArgumentException("Supplier is inactive");
        }

        Purchase purchase = new Purchase();
        purchase.setPurchaseId(request.purchaseId().trim());
        purchase.setSupplier(supplier);
        purchase.setPurchaseDate(request.purchaseDate());
        purchase.setInvoiceNumber(request.invoiceNumber().trim());
        purchase.setInvoiceDate(request.invoiceDate());
        purchase.setOtherCharges(request.otherCharges());
        purchase.setPaidAmount(request.paidAmount());
        purchase.setPaymentStatus(request.paymentStatus());
        purchase.setRemarks(request.remarks());

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal taxTotal = BigDecimal.ZERO;
        for (PurchaseRequest.Item requestItem : request.items()) {
            var variant = variants.findById(requestItem.variantId()).orElseThrow(() -> new EntityNotFoundException("Variant not found"));
            if (!variant.isActive()) {
                throw new IllegalArgumentException("Variant is inactive");
            }
            PurchaseItem item = new PurchaseItem();
            item.setPurchase(purchase);
            item.setVariant(variant);
            item.setSkuSnapshot(variant.getSku());
            item.setQuantity(requestItem.quantity());
            item.setUnitCost(requestItem.unitCost());
            item.setTax(requestItem.tax());
            item.setLineTotal(requestItem.unitCost().multiply(BigDecimal.valueOf(requestItem.quantity())).add(requestItem.tax()));
            subtotal = subtotal.add(requestItem.unitCost().multiply(BigDecimal.valueOf(requestItem.quantity())));
            taxTotal = taxTotal.add(requestItem.tax());
            purchase.getItems().add(item);
        }
        purchase.setSubtotal(subtotal);
        purchase.setTax(taxTotal);
        purchase.setTotalAmount(subtotal.add(taxTotal).add(request.otherCharges()));
        purchase.setDueAmount(purchase.getTotalAmount().subtract(request.paidAmount()));
        if (purchase.getDueAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Paid amount cannot exceed total amount");
        }
        return response(purchases.save(purchase));
    }

    @Transactional
    public PurchaseResponse update(Long id, PurchaseRequest request) {
        Purchase purchase = purchases.findById(id).orElseThrow(() -> new EntityNotFoundException("Purchase not found"));
        if (purchase.getPurchaseStatus() == PurchaseStatus.CANCELLED) {
            throw new IllegalArgumentException("Cancelled purchase cannot be edited");
        }
        if (purchases.findByPurchaseIdIgnoreCaseAndIdNot(request.purchaseId().trim(), id).isPresent()) {
            throw new IllegalArgumentException("Purchase ID already exists");
        }
        if (purchases.findByInvoiceNumberIgnoreCaseAndIdNot(request.invoiceNumber().trim(), id).isPresent()) {
            throw new IllegalArgumentException("Invoice number already exists");
        }

        boolean inventoryWasReceived = purchase.isInventoryReceived();
        if (inventoryWasReceived) {
            reverseInventory(purchase);
            purchase.setInventoryReceived(false);
        }

        purchase.getItems().clear();
        applyHeader(purchase, request);

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal taxTotal = BigDecimal.ZERO;
        for (PurchaseRequest.Item requestItem : request.items()) {
            var variant = variants.findById(requestItem.variantId()).orElseThrow(() -> new EntityNotFoundException("Variant not found"));
            if (!variant.isActive()) {
                throw new IllegalArgumentException("Variant is inactive");
            }
            PurchaseItem item = new PurchaseItem();
            item.setPurchase(purchase);
            item.setVariant(variant);
            item.setSkuSnapshot(variant.getSku());
            item.setQuantity(requestItem.quantity());
            item.setUnitCost(requestItem.unitCost());
            item.setTax(requestItem.tax());
            item.setLineTotal(requestItem.unitCost().multiply(BigDecimal.valueOf(requestItem.quantity())).add(requestItem.tax()));
            subtotal = subtotal.add(requestItem.unitCost().multiply(BigDecimal.valueOf(requestItem.quantity())));
            taxTotal = taxTotal.add(requestItem.tax());
            purchase.getItems().add(item);
        }
        purchase.setSubtotal(subtotal);
        purchase.setTax(taxTotal);
        purchase.setTotalAmount(subtotal.add(taxTotal).add(request.otherCharges()));
        purchase.setDueAmount(purchase.getTotalAmount().subtract(request.paidAmount()));
        if (purchase.getDueAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Paid amount cannot exceed total amount");
        }

        Purchase saved = purchases.save(purchase);
        if (inventoryWasReceived || saved.getPurchaseStatus() == PurchaseStatus.RECEIVED) {
            receiveInventory(saved);
            saved.setInventoryReceived(true);
            saved = purchases.save(saved);
        }
        return response(saved);
    }

    @Transactional
    public PurchaseResponse updateStatus(Long id, PurchaseStatusRequest request) {
        Purchase purchase = purchases.findById(id).orElseThrow(() -> new EntityNotFoundException("Purchase not found"));
        if (purchase.getPurchaseStatus() == request.status()) {
            if (request.status() == PurchaseStatus.RECEIVED && purchase.isInventoryReceived()) {
                throw new IllegalArgumentException("Purchase has already been received");
            }
            return response(purchase);
        }
        if (request.status() == PurchaseStatus.RECEIVED) {
            if (purchase.isInventoryReceived()) {
                throw new IllegalArgumentException("Purchase has already been received");
            }
            receiveInventory(purchase);
            purchase.setInventoryReceived(true);
        }
        if (request.status() == PurchaseStatus.CANCELLED && purchase.isInventoryReceived()) {
            reverseInventory(purchase);
            purchase.setInventoryReceived(false);
        }
        purchase.setPurchaseStatus(request.status());
        return response(purchases.save(purchase));
    }

    @Transactional
    public PurchaseResponse updatePaymentStatus(Long id, PurchasePaymentStatusRequest request) {
        Purchase purchase = purchases.findById(id).orElseThrow(() -> new EntityNotFoundException("Purchase not found"));
        purchase.setPaymentStatus(request.status());
        return response(purchases.save(purchase));
    }

    private void receiveInventory(Purchase purchase) {
        purchase.getItems().forEach(item -> inventory.record(new InventoryTransactionRequest(
                item.getVariant().getId(),
                InventoryTransactionType.PURCHASE,
                item.getQuantity(),
                item.getUnitCost(),
                "PURCHASE",
                purchase.getId() == null ? null : purchase.getId().toString(),
                "Purchase " + purchase.getPurchaseId(),
                purchase.getPurchaseDate())));
    }

    private void reverseInventory(Purchase purchase) {
        purchase.getItems().forEach(item -> inventory.record(new InventoryTransactionRequest(
                item.getVariant().getId(),
                InventoryTransactionType.ADJUSTMENT_OUT,
                item.getQuantity(),
                item.getUnitCost(),
                "PURCHASE_REVERSAL",
                purchase.getId() == null ? null : purchase.getId().toString(),
                "Reversal for purchase " + purchase.getPurchaseId(),
                purchase.getPurchaseDate())));
    }

    private void applyHeader(Purchase purchase, PurchaseRequest request) {
        var supplier = suppliers.findById(request.supplierId()).orElseThrow(() -> new EntityNotFoundException("Supplier not found"));
        if (!supplier.isActive()) {
            throw new IllegalArgumentException("Supplier is inactive");
        }
        purchase.setPurchaseId(request.purchaseId().trim());
        purchase.setSupplier(supplier);
        purchase.setPurchaseDate(request.purchaseDate());
        purchase.setInvoiceNumber(request.invoiceNumber().trim());
        purchase.setInvoiceDate(request.invoiceDate());
        purchase.setOtherCharges(request.otherCharges());
        purchase.setPaidAmount(request.paidAmount());
        purchase.setPaymentStatus(request.paymentStatus());
        purchase.setRemarks(request.remarks());
    }

    private PurchaseResponse response(Purchase purchase) {
        return new PurchaseResponse(purchase.getId(), purchase.getPurchaseId(), purchase.getSupplier().getId(), purchase.getSupplier().getName(),
                purchase.getPurchaseDate(), purchase.getInvoiceNumber(), purchase.getInvoiceDate(), purchase.getSubtotal(),
                purchase.getTax(), purchase.getOtherCharges(), purchase.getTotalAmount(), purchase.getPaymentStatus().name(),
                purchase.getPaidAmount(), purchase.getDueAmount(), purchase.getPurchaseStatus().name(), purchase.isInventoryReceived());
    }

    private PurchaseDetailResponse detail(Purchase purchase) {
        return new PurchaseDetailResponse(
                purchase.getId(),
                purchase.getPurchaseId(),
                purchase.getSupplier().getId(),
                purchase.getSupplier().getName(),
                purchase.getSupplier().getSupplierId(),
                purchase.getPurchaseDate(),
                purchase.getInvoiceNumber(),
                purchase.getInvoiceDate(),
                purchase.getSubtotal(),
                purchase.getTax(),
                purchase.getOtherCharges(),
                purchase.getTotalAmount(),
                purchase.getPaymentStatus().name(),
                purchase.getPaidAmount(),
                purchase.getDueAmount(),
                purchase.getPurchaseStatus().name(),
                purchase.isInventoryReceived(),
                purchase.getRemarks(),
                purchase.getItems().stream().map(item -> new PurchaseItemResponse(
                        item.getId(),
                        item.getVariant().getId(),
                        item.getVariant().getProduct().getProductName(),
                        item.getVariant().getColor().getName(),
                        item.getVariant().getSize().getName(),
                        item.getSkuSnapshot(),
                        item.getQuantity(),
                        item.getUnitCost(),
                        item.getTax(),
                        item.getLineTotal())).toList());
    }
}
