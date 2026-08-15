package com.ecommerce.commerceapi.inventory.service;

import com.ecommerce.commerceapi.auth.security.AuthenticatedUser;
import com.ecommerce.commerceapi.audit.service.AuditLogService;
import com.ecommerce.commerceapi.inventory.api.InventoryAdjustmentRequest;
import com.ecommerce.commerceapi.inventory.api.InventoryAdjustmentType;
import com.ecommerce.commerceapi.inventory.api.InventoryDetailResponse;
import com.ecommerce.commerceapi.inventory.api.InventoryMovementResponse;
import com.ecommerce.commerceapi.inventory.api.InventoryPageResponse;
import com.ecommerce.commerceapi.inventory.api.InventorySort;
import com.ecommerce.commerceapi.inventory.api.InventoryStatus;
import com.ecommerce.commerceapi.inventory.api.InventorySummaryResponse;
import com.ecommerce.commerceapi.inventory.api.InventoryTransactionRequest;
import com.ecommerce.commerceapi.inventory.domain.InventoryTransaction;
import com.ecommerce.commerceapi.inventory.domain.InventoryTransactionType;
import com.ecommerce.commerceapi.inventory.repository.InventoryTransactionRepository;
import com.ecommerce.commerceapi.products.domain.ProductVariant;
import com.ecommerce.commerceapi.products.repository.ProductVariantRepository;
import com.ecommerce.commerceapi.settings.service.SystemSettingsService;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {
    private final InventoryTransactionRepository transactions;
    private final ProductVariantRepository variants;
    private final AuditLogService auditLogs;
    private final SystemSettingsService settings;

    public InventoryService(InventoryTransactionRepository transactions, ProductVariantRepository variants, AuditLogService auditLogs, SystemSettingsService settings) {
        this.transactions = transactions;
        this.variants = variants;
        this.auditLogs = auditLogs;
        this.settings = settings;
    }

    @Transactional
    public InventoryMovementResponse record(InventoryTransactionRequest request) {
        return record(request, currentUser());
    }

    @Transactional
    public InventoryMovementResponse record(InventoryTransactionRequest request, AuthenticatedUser currentUser) {
        ProductVariant variant = variants.findByIdForInventoryUpdate(request.variantId())
                .orElseThrow(() -> new EntityNotFoundException("Variant not found"));
        if (!variant.isActive()) {
            throw new IllegalArgumentException("Variant is inactive");
        }

        int previousStock = transactions.balance(request.variantId());
        if (!request.transactionType().isInbound() && !settings.allowNegativeStock() && previousStock < request.quantity()) {
            throw new IllegalArgumentException("Insufficient sellable stock");
        }

        int newStock = request.transactionType().isInbound()
                ? previousStock + request.quantity()
                : previousStock - request.quantity();

        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setVariant(variant);
        transaction.setTransactionType(request.transactionType());
        transaction.setQuantity(request.quantity());
        transaction.setUnitCost(request.unitCost());
        transaction.setReferenceType(request.referenceType());
        transaction.setReferenceId(request.referenceId());
        transaction.setNotes(request.notes());
        transaction.setRemarks(request.remarks());
        transaction.setPreviousStock(previousStock);
        transaction.setNewStock(newStock);
        transaction.setRecordedByUserId(currentUser == null ? null : currentUser.id());
        transaction.setRecordedByUserName(currentUser == null ? "SYSTEM" : currentUser.name());
        transaction.setTransactionDate(request.transactionDate());

        InventoryTransaction saved = transactions.save(transaction);
        auditLogs.log(
                currentUser,
                auditAction(saved.getTransactionType()),
                "INVENTORY",
                "InventoryTransaction",
                String.valueOf(saved.getId()),
                "Inventory " + saved.getTransactionType().displayName().toLowerCase() + " for " + variant.getSku(),
                Map.of(
                        "previousStock", String.valueOf(previousStock),
                        "quantity", String.valueOf(request.quantity()),
                        "transactionType", saved.getTransactionType().name()),
                Map.of(
                        "variantId", String.valueOf(variant.getId()),
                        "sku", variant.getSku(),
                        "previousStock", String.valueOf(previousStock),
                        "newStock", String.valueOf(newStock),
                        "transactionType", saved.getTransactionType().name()));
        return toMovement(saved);
    }

    @Transactional
    public InventoryMovementResponse adjust(InventoryAdjustmentRequest request) {
        return adjust(request, currentUser());
    }

    @Transactional
    public InventoryMovementResponse adjust(InventoryAdjustmentRequest request, AuthenticatedUser currentUser) {
        InventoryTransactionType transactionType = request.adjustmentType() == InventoryAdjustmentType.ADD_STOCK
                ? InventoryTransactionType.ADJUSTMENT_IN
                : InventoryTransactionType.ADJUSTMENT_OUT;
        return record(new InventoryTransactionRequest(
                request.variantId(),
                transactionType,
                request.quantity(),
                null,
                "MANUAL_ADJUSTMENT",
                null,
                request.reason().trim(),
                request.remarks(),
                request.transactionDate()), currentUser);
    }

    @Transactional(readOnly = true)
    public int balance(Long variantId) {
        return transactions.balance(variantId);
    }

    @Transactional(readOnly = true)
    public InventoryPageResponse list(String query, InventoryStatus status, InventorySort sortBy, int page, int size) {
        List<InventorySummaryResponse> summaries = summarize(query);
        if (status != null) {
            summaries = summaries.stream().filter(summary -> summary.status().equals(status.name())).toList();
        }
        summaries = summaries.stream().sorted(comparator(sortBy)).toList();
        return paginate(summaries, page, size);
    }

    @Transactional(readOnly = true)
    public InventoryPageResponse lowStock(String query, InventorySort sortBy, int page, int size) {
        return list(query, InventoryStatus.LOW_STOCK, sortBy, page, size);
    }

    @Transactional(readOnly = true)
    public InventoryPageResponse outOfStock(String query, InventorySort sortBy, int page, int size) {
        return list(query, InventoryStatus.OUT_OF_STOCK, sortBy, page, size);
    }

    @Transactional(readOnly = true)
    public InventoryDetailResponse detail(Long variantId) {
        ProductVariant variant = variants.findInventoryViewById(variantId)
                .orElseThrow(() -> new EntityNotFoundException("Variant not found"));
        InventorySummaryResponse summary = toSummary(variant, balance(variantId));
        List<InventoryMovementResponse> movements = transactions.findByVariantIdOrderByTransactionDateDescCreatedAtDesc(variantId)
                .stream()
                .map(this::toMovement)
                .toList();
        return new InventoryDetailResponse(summary, movements);
    }

    @Transactional(readOnly = true)
    public List<InventoryMovementResponse> movements(Long variantId) {
        return transactions.findByVariantIdOrderByTransactionDateDescCreatedAtDesc(variantId)
                .stream()
                .map(this::toMovement)
                .toList();
    }

    private List<InventorySummaryResponse> summarize(String query) {
        List<ProductVariant> variants = this.variants.searchInventory(normalize(query));
        if (variants.isEmpty()) {
            return List.of();
        }

        int defaultThreshold = settings.defaultLowStockThreshold();
        Map<Long, Integer> balances = balancesByVariantIds(variants.stream().map(ProductVariant::getId).toList());
        return variants.stream()
                .map(variant -> toSummary(variant, balances.getOrDefault(variant.getId(), 0), defaultThreshold))
                .toList();
    }

    private Map<Long, Integer> balancesByVariantIds(List<Long> variantIds) {
        Map<Long, Integer> balances = new HashMap<>();
        for (Object[] row : transactions.balancesByVariantIds(variantIds)) {
            Long variantId = ((Number) row[0]).longValue();
            Integer currentStock = ((Number) row[1]).intValue();
            balances.put(variantId, currentStock);
        }
        return balances;
    }

    private InventorySummaryResponse toSummary(ProductVariant variant, int currentStock, int defaultThreshold) {
        BigDecimal unitCost = safe(variant.getCostPrice());
        BigDecimal inventoryValue = unitCost.multiply(BigDecimal.valueOf(currentStock)).setScale(2, RoundingMode.HALF_UP);
        int threshold = variant.getReorderLevel() > 0 ? variant.getReorderLevel() : defaultThreshold;
        return new InventorySummaryResponse(
                variant.getId(),
                variant.getSku(),
                variant.getProduct().getProductName(),
                variant.getProduct().getBrand().getName(),
                variant.getProduct().getCategory().getName(),
                variant.getColor().getName(),
                variant.getSize().getName(),
                currentStock,
                threshold,
                statusFor(currentStock, threshold).name(),
                unitCost,
                inventoryValue);
    }

    private InventoryMovementResponse toMovement(InventoryTransaction transaction) {
        ProductVariant variant = transaction.getVariant();
        return new InventoryMovementResponse(
                transaction.getId(),
                transaction.getTransactionDate(),
                transaction.getCreatedAt(),
                variant.getSku(),
                variant.getProduct().getProductName(),
                transaction.getTransactionType().displayName(),
                transaction.getQuantity(),
                transaction.getPreviousStock(),
                transaction.getNewStock(),
                transaction.getReferenceType(),
                transaction.getReferenceId(),
                transaction.getNotes(),
                transaction.getRemarks(),
                transaction.getRecordedByUserId(),
                transaction.getRecordedByUserName(),
                transaction.getUnitCost());
    }

    private InventoryStatus statusFor(int currentStock, int threshold) {
        if (currentStock <= 0) {
            return InventoryStatus.OUT_OF_STOCK;
        }
        if (threshold > 0 && currentStock <= threshold) {
            return InventoryStatus.LOW_STOCK;
        }
        return InventoryStatus.IN_STOCK;
    }

    private Comparator<InventorySummaryResponse> comparator(InventorySort sortBy) {
        InventorySort selected = sortBy == null ? InventorySort.SKU_ASC : sortBy;
        return switch (selected) {
            case STOCK_ASC -> Comparator.comparingInt(InventorySummaryResponse::currentStock)
                    .thenComparing(InventorySummaryResponse::sku, String.CASE_INSENSITIVE_ORDER);
            case STOCK_DESC -> Comparator.comparingInt(InventorySummaryResponse::currentStock)
                    .reversed()
                    .thenComparing(InventorySummaryResponse::sku, String.CASE_INSENSITIVE_ORDER);
            case PRODUCT_ASC -> Comparator.comparing(InventorySummaryResponse::productName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(InventorySummaryResponse::sku, String.CASE_INSENSITIVE_ORDER);
            case SKU_ASC -> Comparator.comparing(InventorySummaryResponse::sku, String.CASE_INSENSITIVE_ORDER);
        };
    }

    private InventoryPageResponse paginate(List<InventorySummaryResponse> items, int page, int size) {
        int effectiveSize = Math.max(size, 1);
        int fromIndex = Math.min(Math.max(page, 0) * effectiveSize, items.size());
        int toIndex = Math.min(fromIndex + effectiveSize, items.size());
        int totalPages = items.isEmpty() ? 0 : (int) Math.ceil(items.size() / (double) effectiveSize);
        return new InventoryPageResponse(items.subList(fromIndex, toIndex), Math.max(page, 0), effectiveSize, items.size(), totalPages);
    }

    private String normalize(String query) {
        return query == null ? "" : query.trim().toLowerCase();
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String auditAction(InventoryTransactionType transactionType) {
        return switch (transactionType) {
            case SALE, DAMAGE -> "STOCK_OUT";
            case ADJUSTMENT_IN, ADJUSTMENT_OUT -> "ADJUSTMENT";
            case OPENING_STOCK, PURCHASE, MANUFACTURED, RETURN -> "STOCK_IN";
        };
    }

    private AuthenticatedUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        return principal instanceof AuthenticatedUser authenticatedUser ? authenticatedUser : null;
    }
}
