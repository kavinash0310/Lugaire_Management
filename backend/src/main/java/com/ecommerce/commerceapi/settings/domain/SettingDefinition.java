package com.ecommerce.commerceapi.settings.domain;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum SettingDefinition {
    BUSINESS_NAME("business_name", SettingType.STRING, "Business/Store Name", "LUGAIRE", "BUSINESS"),
    BRAND_NAME("brand_name", SettingType.STRING, "Brand Name", "LUGAIRE", "BUSINESS"),
    BUSINESS_EMAIL("business_email", SettingType.STRING, "Business Email", "", "BUSINESS"),
    BUSINESS_PHONE("business_phone", SettingType.STRING, "Business Phone", "", "BUSINESS"),
    BUSINESS_ADDRESS("business_address", SettingType.STRING, "Business Address", "", "BUSINESS"),
    CURRENCY("currency", SettingType.STRING, "Currency", "INR", "GENERAL"),
    CURRENCY_SYMBOL("currency_symbol", SettingType.STRING, "Currency Symbol", "₹", "GENERAL"),
    DATE_FORMAT("date_format", SettingType.STRING, "Date Format", "dd/MM/yyyy", "GENERAL"),
    DEFAULT_LOW_STOCK_THRESHOLD("default_low_stock_threshold", SettingType.NUMBER, "Default Low Stock Threshold", "5", "INVENTORY"),
    ALLOW_NEGATIVE_STOCK("allow_negative_stock", SettingType.BOOLEAN, "Allow Negative Stock", "false", "INVENTORY"),
    DEFAULT_ORDER_STATUS("default_order_status", SettingType.STRING, "Default Order Status", "PENDING", "ORDERS"),
    DEFAULT_PAYMENT_STATUS("default_payment_status", SettingType.STRING, "Default Payment Status", "PENDING", "ORDERS"),
    DEFAULT_TAX_RATE("default_tax_rate", SettingType.NUMBER, "Default Tax Rate", "0", "FINANCIAL");

    private final String key;
    private final SettingType type;
    private final String description;
    private final String defaultValue;
    private final String section;

    SettingDefinition(String key, SettingType type, String description, String defaultValue, String section) {
        this.key = key;
        this.type = type;
        this.description = description;
        this.defaultValue = defaultValue;
        this.section = section;
    }

    public String key() {
        return key;
    }

    public SettingType type() {
        return type;
    }

    public String description() {
        return description;
    }

    public String defaultValue() {
        return defaultValue;
    }

    public String section() {
        return section;
    }

    public static Optional<SettingDefinition> fromKey(String key) {
        if (key == null) {
            return Optional.empty();
        }
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(definition -> definition.key.equals(normalized)).findFirst();
    }
}
