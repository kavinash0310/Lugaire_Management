package com.ecommerce.commerceapi.settings.service;

import com.ecommerce.commerceapi.audit.service.AuditLogService;
import com.ecommerce.commerceapi.orders.domain.OrderStatus;
import com.ecommerce.commerceapi.orders.domain.PaymentStatus;
import com.ecommerce.commerceapi.settings.api.SettingResponse;
import com.ecommerce.commerceapi.settings.api.SettingUpdateRequest;
import com.ecommerce.commerceapi.settings.domain.SettingDefinition;
import com.ecommerce.commerceapi.settings.domain.SettingType;
import com.ecommerce.commerceapi.settings.domain.SystemSetting;
import com.ecommerce.commerceapi.settings.repository.SystemSettingRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemSettingsService {
    private final SystemSettingRepository settings;
    private final AuditLogService auditLogs;

    public SystemSettingsService(SystemSettingRepository settings, AuditLogService auditLogs) {
        this.settings = settings;
        this.auditLogs = auditLogs;
    }

    @Transactional(readOnly = true)
    public List<SettingResponse> list() {
        return settings.findAll(Sort.by("settingKey")).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public SettingResponse findByKey(String key) {
        return toResponse(setting(key));
    }

    @Transactional
    public SettingResponse update(String key, SettingUpdateRequest request) {
        SettingDefinition definition = definition(key);
        SystemSetting setting = setting(key);
        String before = setting.getSettingValue();
        String normalized = normalize(definition, request.value());
        setting.setSettingValue(normalized);
        setting.setSettingType(definition.type());
        setting.setDescription(definition.description());
        SystemSetting saved = settings.save(setting);
        auditLogs.log(
                "UPDATE",
                "SETTINGS",
                "Setting",
                definition.key(),
                "Setting updated",
                Map.of("value", before),
                Map.of("value", saved.getSettingValue()));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public String businessName() {
        return valueOf("business_name");
    }

    @Transactional(readOnly = true)
    public String currencyCode() {
        return valueOf("currency");
    }

    @Transactional(readOnly = true)
    public String currencySymbol() {
        return valueOf("currency_symbol");
    }

    @Transactional(readOnly = true)
    public String dateFormat() {
        return valueOf("date_format");
    }

    @Transactional(readOnly = true)
    public int defaultLowStockThreshold() {
        return Integer.parseInt(valueOf("default_low_stock_threshold"));
    }

    @Transactional(readOnly = true)
    public boolean allowNegativeStock() {
        return Boolean.parseBoolean(valueOf("allow_negative_stock"));
    }

    @Transactional(readOnly = true)
    public OrderStatus defaultOrderStatus() {
        return OrderStatus.valueOf(valueOf("default_order_status"));
    }

    @Transactional(readOnly = true)
    public PaymentStatus defaultPaymentStatus() {
        return PaymentStatus.valueOf(valueOf("default_payment_status"));
    }

    @Transactional(readOnly = true)
    public BigDecimal defaultTaxRate() {
        return new BigDecimal(valueOf("default_tax_rate"));
    }

    private String valueOf(String key) {
        return setting(key).getSettingValue();
    }

    private SettingDefinition definition(String key) {
        return SettingDefinition.fromKey(key).orElseThrow(() -> new EntityNotFoundException("Setting not found"));
    }

    private SystemSetting setting(String key) {
        return settings.findBySettingKeyIgnoreCase(key)
                .orElseThrow(() -> new EntityNotFoundException("Setting not found"));
    }

    private SettingResponse toResponse(SystemSetting setting) {
        return new SettingResponse(setting.getSettingKey(), setting.getSettingValue(), setting.getSettingType(), setting.getDescription(), setting.getUpdatedAt());
    }

    private String normalize(SettingDefinition definition, String value) {
        if (value == null) {
            throw new IllegalArgumentException("Setting value cannot be blank");
        }
        String trimmed = value.trim();
        return switch (definition) {
            case BUSINESS_NAME, BRAND_NAME -> requireText(trimmed, definition.description());
            case BUSINESS_EMAIL, BUSINESS_PHONE, BUSINESS_ADDRESS -> trimmed;
            case CURRENCY -> normalizeCurrency(trimmed);
            case CURRENCY_SYMBOL -> requireText(trimmed, definition.description());
            case DATE_FORMAT -> requireText(trimmed, definition.description());
            case DEFAULT_LOW_STOCK_THRESHOLD -> normalizeInteger(trimmed, definition.description());
            case ALLOW_NEGATIVE_STOCK -> normalizeBoolean(trimmed);
            case DEFAULT_ORDER_STATUS -> normalizeEnum(trimmed, OrderStatus.class, definition.description());
            case DEFAULT_PAYMENT_STATUS -> normalizeEnum(trimmed, PaymentStatus.class, definition.description());
            case DEFAULT_TAX_RATE -> normalizeDecimal(trimmed, definition.description());
        };
    }

    private String requireText(String value, String label) {
        if (value.isBlank()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        return value;
    }

    private String normalizeCurrency(String value) {
        if (value.isBlank()) {
            throw new IllegalArgumentException("Currency cannot be blank");
        }
        String code = value.toUpperCase(Locale.ROOT);
        try {
            Currency.getInstance(code);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Currency is not supported");
        }
        return code;
    }

    private String normalizeInteger(String value, String label) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 0) {
                throw new IllegalArgumentException(label + " must be greater than or equal to zero");
            }
            return Integer.toString(parsed);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(label + " must be a whole number");
        }
    }

    private String normalizeDecimal(String value, String label) {
        try {
            BigDecimal parsed = new BigDecimal(value);
            if (parsed.compareTo(BigDecimal.ZERO) < 0 || parsed.compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException(label + " must be between 0 and 100");
            }
            return parsed.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(label + " must be a number");
        }
    }

    private String normalizeBoolean(String value) {
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return Boolean.toString(Boolean.parseBoolean(value));
        }
        throw new IllegalArgumentException("Allow Negative Stock must be true or false");
    }

    private <T extends Enum<T>> String normalizeEnum(String value, Class<T> enumType, String label) {
        if (value.isBlank()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        try {
            return Enum.valueOf(enumType, value.trim().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(label + " is not valid");
        }
    }
}
