package com.ecommerce.commerceapi.importexport.api;

import java.util.Locale;

public enum ImportExportModule {
    PRODUCTS("products", "Products / SKUs", true),
    INVENTORY("inventory", "Inventory", true),
    ORDERS("orders", "Orders", false),
    PURCHASES("purchases", "Purchases", false),
    EXPENSES("expenses", "Expenses", true);

    private final String slug;
    private final String label;
    private final boolean importable;

    ImportExportModule(String slug, String label, boolean importable) {
        this.slug = slug;
        this.label = label;
        this.importable = importable;
    }

    public String slug() {
        return slug;
    }

    public String label() {
        return label;
    }

    public boolean importable() {
        return importable;
    }

    public static ImportExportModule from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Module is required");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (ImportExportModule module : values()) {
            if (module.slug.equals(normalized) || module.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return module;
            }
        }
        throw new IllegalArgumentException("Unsupported import/export module: " + value);
    }
}
