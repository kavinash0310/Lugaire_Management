package com.ecommerce.commerceapi.inventory.domain;

public enum InventoryTransactionType {
    OPENING_STOCK,
    PURCHASE,
    MANUFACTURED,
    SALE,
    RETURN,
    DAMAGE,
    ADJUSTMENT_IN,
    ADJUSTMENT_OUT;

    public boolean isInbound() {
        return this == OPENING_STOCK
                || this == PURCHASE
                || this == MANUFACTURED
                || this == RETURN
                || this == ADJUSTMENT_IN;
    }

    public String displayName() {
        return this == OPENING_STOCK ? "OPENING" : name();
    }
}
