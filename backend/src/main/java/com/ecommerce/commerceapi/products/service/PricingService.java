package com.ecommerce.commerceapi.products.service;

import com.ecommerce.commerceapi.auth.security.AuthenticatedUser;
import com.ecommerce.commerceapi.inventory.repository.InventoryTransactionRepository;
import com.ecommerce.commerceapi.audit.service.AuditLogService;
import com.ecommerce.commerceapi.products.api.PricingDetailResponse;
import com.ecommerce.commerceapi.products.api.PricingHistoryResponse;
import com.ecommerce.commerceapi.products.api.PricingPageResponse;
import com.ecommerce.commerceapi.products.api.PricingSort;
import com.ecommerce.commerceapi.products.api.PricingSummaryResponse;
import com.ecommerce.commerceapi.products.api.PricingUpdateRequest;
import com.ecommerce.commerceapi.products.domain.Product;
import com.ecommerce.commerceapi.products.domain.ProductVariant;
import com.ecommerce.commerceapi.products.domain.ProductVariantPriceHistory;
import com.ecommerce.commerceapi.products.repository.ProductVariantPriceHistoryRepository;
import com.ecommerce.commerceapi.products.repository.ProductVariantRepository;
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
public class PricingService {
    private final ProductVariantRepository variants;
    private final ProductVariantPriceHistoryRepository history;
    private final InventoryTransactionRepository inventoryTransactions;
    private final AuditLogService auditLogs;

    public PricingService(
            ProductVariantRepository variants,
            ProductVariantPriceHistoryRepository history,
            InventoryTransactionRepository inventoryTransactions,
            AuditLogService auditLogs) {
        this.variants = variants;
        this.history = history;
        this.inventoryTransactions = inventoryTransactions;
        this.auditLogs = auditLogs;
    }

    @Transactional(readOnly = true)
    public PricingPageResponse list(String query, Long brandId, Long categoryId, PricingSort sortBy, int page, int size) {
        List<ProductVariant> results = variants.searchPricing(normalize(query));
        if (brandId != null) {
            results = results.stream().filter(variant -> variant.getProduct().getBrand().getId().equals(brandId)).toList();
        }
        if (categoryId != null) {
            results = results.stream().filter(variant -> variant.getProduct().getCategory().getId().equals(categoryId)).toList();
        }

        Map<Long, Integer> stockMap = balancesByVariantIds(results.stream().map(ProductVariant::getId).toList());
        List<PricingSummaryResponse> summaries = results.stream()
                .map(variant -> toSummary(variant, stockMap.getOrDefault(variant.getId(), 0)))
                .sorted(comparator(sortBy))
                .toList();
        return paginate(summaries, page, size);
    }

    @Transactional(readOnly = true)
    public PricingDetailResponse detail(Long variantId) {
        ProductVariant variant = variants.findPricingViewById(variantId)
                .orElseThrow(() -> new EntityNotFoundException("Variant not found"));
        return new PricingDetailResponse(
                toSummary(variant, inventoryTransactions.balance(variantId)),
                history.findAllByVariant_IdOrderByEffectiveDateDescCreatedAtDesc(variantId)
                        .stream()
                        .map(this::toHistory)
                        .toList());
    }

    @Transactional
    public PricingDetailResponse update(Long variantId, PricingUpdateRequest request) {
        ProductVariant variant = variants.findPricingViewById(variantId)
                .orElseThrow(() -> new EntityNotFoundException("Variant not found"));
        if (!variant.isActive()) {
            throw new IllegalArgumentException("Variant is inactive");
        }
        BigDecimal currentSellingPrice = safe(variant.getSellingPrice());
        BigDecimal newSellingPrice = safe(request.sellingPrice());
        if (currentSellingPrice.compareTo(newSellingPrice) == 0) {
            throw new IllegalArgumentException("Selling price is already set to this value");
        }

        variant.setSellingPrice(newSellingPrice);
        Product product = variant.getProduct();
        product.setSellingPrice(newSellingPrice);

        ProductVariantPriceHistory record = new ProductVariantPriceHistory();
        record.setVariant(variant);
        record.setPreviousSellingPrice(currentSellingPrice);
        record.setNewSellingPrice(newSellingPrice);
        record.setEffectiveDate(request.effectiveDate());
        record.setRemarks(normalizeRemarks(request.remarks()));
        AuthenticatedUser currentUser = currentUser();
        record.setRecordedByUserId(currentUser == null ? null : currentUser.id());
        record.setRecordedByUserName(currentUser == null ? "SYSTEM" : currentUser.name());
        history.save(record);
        auditLogs.log(
                currentUser,
                "UPDATE",
                "SKU",
                "ProductVariant",
                String.valueOf(variant.getId()),
                "SKU price updated for " + variant.getSku(),
                Map.of("sellingPrice", currentSellingPrice.toPlainString()),
                Map.of("sellingPrice", newSellingPrice.toPlainString(), "effectiveDate", request.effectiveDate().toString()));

        return detail(variantId);
    }

    @Transactional(readOnly = true)
    public List<PricingHistoryResponse> history(Long variantId) {
        return history.findAllByVariant_IdOrderByEffectiveDateDescCreatedAtDesc(variantId)
                .stream()
                .map(this::toHistory)
                .toList();
    }

    private PricingSummaryResponse toSummary(ProductVariant variant, int currentStock) {
        BigDecimal unitCost = safe(variant.getCostPrice());
        BigDecimal sellingPrice = safe(variant.getSellingPrice());
        BigDecimal grossProfit = sellingPrice.subtract(unitCost).setScale(2, RoundingMode.HALF_UP);
        BigDecimal marginPercent = sellingPrice.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : grossProfit.divide(sellingPrice, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
        BigDecimal inventoryValue = unitCost.multiply(BigDecimal.valueOf(currentStock)).setScale(2, RoundingMode.HALF_UP);

        return new PricingSummaryResponse(
                variant.getId(),
                variant.getSku(),
                variant.getProduct().getProductName(),
                variant.getProduct().getBrand().getName(),
                variant.getProduct().getCategory().getName(),
                variant.getColor().getName(),
                variant.getSize().getName(),
                variant.isActive(),
                unitCost,
                sellingPrice,
                grossProfit,
                marginPercent,
                currentStock,
                inventoryValue);
    }

    private PricingHistoryResponse toHistory(ProductVariantPriceHistory item) {
        return new PricingHistoryResponse(
                item.getId(),
                item.getPreviousSellingPrice(),
                item.getNewSellingPrice(),
                item.getEffectiveDate(),
                item.getRemarks(),
                item.getRecordedByUserId(),
                item.getRecordedByUserName(),
                item.getCreatedAt());
    }

    private Map<Long, Integer> balancesByVariantIds(List<Long> variantIds) {
        Map<Long, Integer> balances = new HashMap<>();
        if (variantIds.isEmpty()) {
            return balances;
        }
        for (Object[] row : inventoryTransactions.balancesByVariantIds(variantIds)) {
            Long variantId = ((Number) row[0]).longValue();
            Integer currentStock = ((Number) row[1]).intValue();
            balances.put(variantId, currentStock);
        }
        return balances;
    }

    private Comparator<PricingSummaryResponse> comparator(PricingSort sortBy) {
        PricingSort selected = sortBy == null ? PricingSort.SKU_ASC : sortBy;
        return switch (selected) {
            case PRODUCT_ASC -> Comparator.comparing(PricingSummaryResponse::productName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(PricingSummaryResponse::sku, String.CASE_INSENSITIVE_ORDER);
            case PRICE_ASC -> Comparator.comparing(PricingSummaryResponse::sellingPrice)
                    .thenComparing(PricingSummaryResponse::sku, String.CASE_INSENSITIVE_ORDER);
            case PRICE_DESC -> Comparator.comparing(PricingSummaryResponse::sellingPrice)
                    .reversed()
                    .thenComparing(PricingSummaryResponse::sku, String.CASE_INSENSITIVE_ORDER);
            case PROFIT_DESC -> Comparator.comparing(PricingSummaryResponse::grossProfit)
                    .reversed()
                    .thenComparing(PricingSummaryResponse::sku, String.CASE_INSENSITIVE_ORDER);
            case MARGIN_DESC -> Comparator.comparing(PricingSummaryResponse::marginPercent)
                    .reversed()
                    .thenComparing(PricingSummaryResponse::sku, String.CASE_INSENSITIVE_ORDER);
            case SKU_ASC -> Comparator.comparing(PricingSummaryResponse::sku, String.CASE_INSENSITIVE_ORDER);
        };
    }

    private PricingPageResponse paginate(List<PricingSummaryResponse> items, int page, int size) {
        int effectivePage = Math.max(page, 0);
        int effectiveSize = Math.max(size, 1);
        int fromIndex = Math.min(effectivePage * effectiveSize, items.size());
        int toIndex = Math.min(fromIndex + effectiveSize, items.size());
        int totalPages = items.isEmpty() ? 0 : (int) Math.ceil(items.size() / (double) effectiveSize);
        return new PricingPageResponse(items.subList(fromIndex, toIndex), effectivePage, effectiveSize, items.size(), totalPages);
    }

    private String normalize(String query) {
        return query == null ? "" : query.trim().toLowerCase();
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String normalizeRemarks(String remarks) {
        return remarks == null || remarks.trim().isEmpty() ? null : remarks.trim();
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
