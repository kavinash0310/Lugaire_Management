package com.ecommerce.commerceapi.importexport.service;

import com.ecommerce.commerceapi.expenses.domain.Expense;
import com.ecommerce.commerceapi.expenses.domain.ExpensePaymentMethod;
import com.ecommerce.commerceapi.expenses.domain.ExpensePaymentStatus;
import com.ecommerce.commerceapi.expenses.repository.ExpenseRepository;
import com.ecommerce.commerceapi.importexport.api.ImportExportError;
import com.ecommerce.commerceapi.importexport.api.ImportExportFormat;
import com.ecommerce.commerceapi.importexport.api.ImportExportModule;
import com.ecommerce.commerceapi.importexport.api.ImportExportResult;
import com.ecommerce.commerceapi.inventory.domain.InventoryTransaction;
import com.ecommerce.commerceapi.inventory.domain.InventoryTransactionType;
import com.ecommerce.commerceapi.inventory.repository.InventoryTransactionRepository;
import com.ecommerce.commerceapi.masters.domain.Brand;
import com.ecommerce.commerceapi.masters.domain.Category;
import com.ecommerce.commerceapi.masters.domain.Color;
import com.ecommerce.commerceapi.masters.domain.ExpenseCategory;
import com.ecommerce.commerceapi.masters.domain.Size;
import com.ecommerce.commerceapi.masters.repository.BrandRepository;
import com.ecommerce.commerceapi.masters.repository.CategoryRepository;
import com.ecommerce.commerceapi.masters.repository.ColorRepository;
import com.ecommerce.commerceapi.masters.repository.ExpenseCategoryRepository;
import com.ecommerce.commerceapi.masters.repository.SizeRepository;
import com.ecommerce.commerceapi.orders.domain.Order;
import com.ecommerce.commerceapi.orders.domain.OrderItem;
import com.ecommerce.commerceapi.orders.repository.OrderRepository;
import com.ecommerce.commerceapi.products.domain.Product;
import com.ecommerce.commerceapi.products.domain.ProductStatus;
import com.ecommerce.commerceapi.products.domain.ProductVariant;
import com.ecommerce.commerceapi.products.domain.SourcingType;
import com.ecommerce.commerceapi.products.repository.ProductRepository;
import com.ecommerce.commerceapi.products.repository.ProductVariantRepository;
import com.ecommerce.commerceapi.purchases.domain.Purchase;
import com.ecommerce.commerceapi.purchases.domain.PurchaseItem;
import com.ecommerce.commerceapi.purchases.repository.PurchaseRepository;
import com.ecommerce.commerceapi.suppliers.domain.Supplier;
import com.ecommerce.commerceapi.suppliers.repository.SupplierRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImportExportService {
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final Pattern EXPENSE_NUMBER_PATTERN = Pattern.compile("^EXP-(\\d{4})-(\\d{6})$");
    private static final long MAX_UPLOAD_BYTES = 5L * 1024L * 1024L;
    private static final List<String> CSV_CONTENT_TYPES = List.of("text/csv", "application/csv");
    private static final List<String> XLSX_CONTENT_TYPES = List.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ProductRepository products;
    private final ProductVariantRepository variants;
    private final BrandRepository brands;
    private final CategoryRepository categories;
    private final ColorRepository colors;
    private final SizeRepository sizes;
    private final OrderRepository orders;
    private final PurchaseRepository purchases;
    private final ExpenseRepository expenses;
    private final ExpenseCategoryRepository expenseCategories;
    private final SupplierRepository suppliers;
    private final InventoryTransactionRepository inventoryTransactions;
    @PersistenceContext private EntityManager entityManager;

    public ImportExportService(
            ProductRepository products,
            ProductVariantRepository variants,
            BrandRepository brands,
            CategoryRepository categories,
            ColorRepository colors,
            SizeRepository sizes,
            OrderRepository orders,
            PurchaseRepository purchases,
            ExpenseRepository expenses,
            ExpenseCategoryRepository expenseCategories,
            SupplierRepository suppliers,
            InventoryTransactionRepository inventoryTransactions) {
        this.products = products;
        this.variants = variants;
        this.brands = brands;
        this.categories = categories;
        this.colors = colors;
        this.sizes = sizes;
        this.orders = orders;
        this.purchases = purchases;
        this.expenses = expenses;
        this.expenseCategories = expenseCategories;
        this.suppliers = suppliers;
        this.inventoryTransactions = inventoryTransactions;
    }

    @Transactional(readOnly = true)
    public byte[] export(ImportExportModule module, ImportExportFormat format) {
        return writeTable(exportTable(module), format);
    }

    @Transactional(readOnly = true)
    public byte[] template(ImportExportModule module, ImportExportFormat format) {
        if (!module.importable()) {
            throw new IllegalArgumentException(module.label() + " does not have an import template");
        }
        return writeTable(templateTable(module), format);
    }

    @Transactional
    public ImportExportResult importFile(ImportExportModule module, MultipartFile file) {
        if (!module.importable()) {
            throw new IllegalArgumentException(module.label() + " does not support import");
        }
        TableInput input = readTable(file);
        ImportOutcome outcome = new ImportOutcome(module.slug());
        for (int index = 0; index < input.rows().size(); index++) {
            int rowNumber = index + 2;
            Map<String, String> row = input.rows().get(index);
            try {
                switch (module) {
                    case PRODUCTS -> outcome.record(importProductRow(row));
                    case INVENTORY -> outcome.record(importInventoryRow(row));
                    case EXPENSES -> outcome.record(importExpenseRow(row));
                    default -> throw new IllegalArgumentException(module.label() + " does not support import");
                }
            } catch (Exception exception) {
                outcome.fail(rowNumber, exception.getMessage());
            }
        }
        return outcome.toResult();
    }

    private TableData exportTable(ImportExportModule module) {
        return switch (module) {
            case PRODUCTS -> exportProducts();
            case INVENTORY -> exportInventory();
            case ORDERS -> exportOrders();
            case PURCHASES -> exportPurchases();
            case EXPENSES -> exportExpenses();
        };
    }

    private TableData templateTable(ImportExportModule module) {
        return switch (module) {
            case PRODUCTS -> new TableData("products", productImportHeaders(), List.of());
            case INVENTORY -> new TableData("inventory", inventoryImportHeaders(), List.of());
            case EXPENSES -> new TableData("expenses", expenseImportHeaders(), List.of());
            default -> throw new IllegalArgumentException(module.label() + " does not support import");
        };
    }

    private TableData exportProducts() {
        List<List<String>> rows = new ArrayList<>();
        for (Product product : products.findAllByOrderByDesignNumberAsc()) {
            for (ProductVariant variant : product.getVariants()) {
                rows.add(row(
                        text(product.getId()),
                        text(product.getDesignNumber()),
                        text(product.getProductName()),
                        text(product.getBrand().getCode()),
                        text(product.getBrand().getName()),
                        text(product.getCategory().getCode()),
                        text(product.getCategory().getName()),
                        text(product.getCategory().getGroupName()),
                        text(product.getDescription()),
                        text(product.getFabric()),
                        text(product.getFit()),
                        text(product.getNeckType()),
                        text(product.getSleeveType()),
                        text(product.getGender()),
                        text(product.getSourcingType()),
                        money(product.getProductCost()),
                        money(product.getSellingPrice()),
                        money(product.getMrp()),
                        text(variant.getId()),
                        text(variant.getSku()),
                        text(variant.getColor().getCode()),
                        text(variant.getColor().getName()),
                        text(variant.getSize().getCode()),
                        text(variant.getSize().getName()),
                        money(variant.getCostPrice()),
                        money(variant.getSellingPrice()),
                        money(variant.getMrp()),
                        text(variant.getReorderLevel()),
                        text(product.getStatus().name()),
                        text(variant.isActive()),
                        text(product.getCreatedAt()),
                        text(product.getUpdatedAt())));
            }
        }
        return new TableData("products", productExportHeaders(), rows);
    }

    private TableData exportInventory() {
        List<ProductVariant> loadedVariants = variants.searchInventory("");
        if (loadedVariants.isEmpty()) {
            return new TableData("inventory", inventoryExportHeaders(), List.of());
        }
        Map<Long, Integer> balances = balancesByVariantIds(loadedVariants.stream().map(ProductVariant::getId).toList());
        List<List<String>> rows = new ArrayList<>();
        for (ProductVariant variant : loadedVariants) {
            int currentStock = balances.getOrDefault(variant.getId(), 0);
            BigDecimal unitCost = safe(variant.getCostPrice());
            BigDecimal inventoryValue = unitCost.multiply(BigDecimal.valueOf(currentStock)).setScale(2, RoundingMode.HALF_UP);
            Product product = variant.getProduct();
            rows.add(row(
                    text(variant.getId()),
                    text(variant.getSku()),
                    text(product.getDesignNumber()),
                    text(product.getProductName()),
                    text(product.getBrand().getCode()),
                    text(product.getBrand().getName()),
                    text(product.getCategory().getCode()),
                    text(product.getCategory().getName()),
                    text(variant.getColor().getCode()),
                    text(variant.getColor().getName()),
                    text(variant.getSize().getCode()),
                    text(variant.getSize().getName()),
                    text(currentStock),
                    text(variant.getReorderLevel()),
                    money(unitCost),
                    money(inventoryValue),
                    text(product.getStatus().name()),
                    text(variant.isActive())));
        }
        return new TableData("inventory", inventoryExportHeaders(), rows);
    }

    private TableData exportOrders() {
        List<List<String>> rows = new ArrayList<>();
        for (Order order : orders.findAllByOrderByOrderDateDescIdDesc()) {
            if (order.getItems().isEmpty()) {
                rows.add(orderRow(order, null));
                continue;
            }
            for (OrderItem item : order.getItems()) {
                rows.add(orderRow(order, item));
            }
        }
        return new TableData("orders", orderExportHeaders(), rows);
    }

    private TableData exportPurchases() {
        List<List<String>> rows = new ArrayList<>();
        for (Purchase purchase : purchases.findAllByOrderByPurchaseDateDescIdDesc()) {
            if (purchase.getItems().isEmpty()) {
                rows.add(purchaseRow(purchase, null));
                continue;
            }
            for (PurchaseItem item : purchase.getItems()) {
                rows.add(purchaseRow(purchase, item));
            }
        }
        return new TableData("purchases", purchaseExportHeaders(), rows);
    }

    private TableData exportExpenses() {
        List<List<String>> rows = new ArrayList<>();
        for (Expense expense : expenses.findAllByOrderByExpenseDateDescIdDesc()) {
            rows.add(row(
                    text(expense.getExpenseNumber()),
                    text(expense.getExpenseDate()),
                    text(expense.getCategory().getCode()),
                    text(expense.getCategory().getName()),
                    text(expense.getSupplier() == null ? null : expense.getSupplier().getSupplierId()),
                    text(expense.getSupplier() == null ? null : expense.getSupplier().getName()),
                    text(expense.getDescription()),
                    money(expense.getAmount()),
                    money(expense.getTaxAmount()),
                    money(expense.getTotalAmount()),
                    text(expense.getPaymentMethod().name()),
                    text(expense.getPaymentStatus().name()),
                    text(expense.getReferenceNumber()),
                    text(expense.getRemarks()),
                    text(expense.getCreatedAt()),
                    text(expense.getUpdatedAt())));
        }
        return new TableData("expenses", expenseExportHeaders(), rows);
    }

    private List<String> orderRow(Order order, OrderItem item) {
        ProductVariant variant = item == null ? null : item.getVariant();
        return row(
                text(order.getOrderId()),
                text(order.getMarketplace().getCode()),
                text(order.getMarketplace().getName()),
                text(order.getOrderDate()),
                text(order.getCustomerName()),
                text(order.getCustomerPhone()),
                text(order.getShippingAddress()),
                text(order.getCity()),
                text(order.getState()),
                text(order.getPincode()),
                text(order.getTrackingNumber()),
                text(order.getCourierPartner()),
                text(order.getRemarks()),
                text(order.getOrderStatus().name()),
                text(order.getPaymentStatus().name()),
                text(order.isInventoryDeducted()),
                money(order.getTotalOrderValue()),
                money(order.getCommission()),
                money(order.getShippingCharge()),
                money(order.getOtherCharges()),
                money(order.getNetAmount()),
                text(item == null ? null : item.getSkuSnapshot()),
                text(variant == null ? null : variant.getProduct().getProductName()),
                text(variant == null ? null : variant.getColor().getCode()),
                text(variant == null ? null : variant.getColor().getName()),
                text(variant == null ? null : variant.getSize().getCode()),
                text(variant == null ? null : variant.getSize().getName()),
                text(item == null ? null : item.getQuantity()),
                money(item == null ? null : item.getSellingPrice()),
                money(item == null ? null : item.getLineTotal()));
    }

    private List<String> purchaseRow(Purchase purchase, PurchaseItem item) {
        ProductVariant variant = item == null ? null : item.getVariant();
        return row(
                text(purchase.getPurchaseId()),
                text(purchase.getSupplier().getSupplierId()),
                text(purchase.getSupplier().getName()),
                text(purchase.getPurchaseDate()),
                text(purchase.getInvoiceNumber()),
                text(purchase.getInvoiceDate()),
                text(purchase.getPurchaseStatus().name()),
                text(purchase.getPaymentStatus().name()),
                text(purchase.isInventoryReceived()),
                text(purchase.getRemarks()),
                text(item == null ? null : item.getSkuSnapshot()),
                text(variant == null ? null : variant.getProduct().getProductName()),
                text(variant == null ? null : variant.getColor().getCode()),
                text(variant == null ? null : variant.getColor().getName()),
                text(variant == null ? null : variant.getSize().getCode()),
                text(variant == null ? null : variant.getSize().getName()),
                text(item == null ? null : item.getQuantity()),
                money(item == null ? null : item.getUnitCost()),
                money(item == null ? null : item.getTax()),
                money(item == null ? null : item.getLineTotal()),
                money(purchase.getSubtotal()),
                money(purchase.getTotalAmount()),
                money(purchase.getPaidAmount()),
                money(purchase.getDueAmount()),
                money(purchase.getOtherCharges()));
    }

    private ImportExportResult importProductRow(Map<String, String> row) {
        String productName = required(row, "productName");
        String brandCode = required(row, "brandCode");
        String categoryCode = required(row, "categoryCode");
        String colorCode = required(row, "colorCode");
        String sizeCode = required(row, "sizeCode");
        String sourcingType = required(row, "sourcingType");
        BigDecimal productCost = requiredDecimal(row, "productCost");
        BigDecimal sellingPrice = requiredDecimal(row, "sellingPrice");
        BigDecimal mrp = requiredDecimal(row, "mrp");
        BigDecimal variantCost = decimal(row, "variantCostPrice", productCost);
        BigDecimal variantSelling = decimal(row, "variantSellingPrice", sellingPrice);
        BigDecimal variantMrp = decimal(row, "variantMrp", mrp);
        int reorderLevel = integer(row, "reorderLevel", 0);
        String sku = trimToNull(value(row, "sku"));
        Long requestedDesignNumber = longValue(row, "designNumber");
        boolean productActive = booleanValue(row, "productStatus", true);
        boolean variantActive = booleanValue(row, "variantActive", true);

        Brand brand = activeBrand(brandCode);
        Category category = activeCategory(categoryCode);
        Color color = activeColor(colorCode);
        Size size = activeSize(sizeCode);

        Product product = null;
        boolean created = false;
        if (requestedDesignNumber != null) {
            product = products.findByDesignNumber(requestedDesignNumber).orElse(null);
        }
        ProductVariant existingVariant = sku == null ? null : variants.findBySkuIgnoreCase(sku).orElse(null);
        if (product == null && existingVariant != null) {
            product = existingVariant.getProduct();
        }
        if (product == null) {
            product = new Product();
            created = true;
            product.setDesignNumber(requestedDesignNumber == null ? nextProductDesignNumber() : requestedDesignNumber);
        } else if (requestedDesignNumber != null && !requestedDesignNumber.equals(product.getDesignNumber())) {
            throw new IllegalArgumentException("Design number already belongs to another product");
        }
        if (existingVariant != null && !existingVariant.getProduct().getId().equals(product.getId())) {
            throw new IllegalArgumentException("SKU already belongs to another product");
        }

        product.setProductName(productName.trim());
        product.setBrand(brand);
        product.setCategory(category);
        product.setDescription(trimToNull(value(row, "description")));
        product.setFabric(trimToNull(value(row, "fabric")));
        product.setFit(trimToNull(value(row, "fit")));
        product.setNeckType(trimToNull(value(row, "neckType")));
        product.setSleeveType(trimToNull(value(row, "sleeveType")));
        product.setGender(trimToNull(value(row, "gender")));
        product.setSourcingType(SourcingType.valueOf(sourcingType.trim().toUpperCase(Locale.ROOT)));
        product.setProductCost(productCost);
        product.setSellingPrice(sellingPrice);
        product.setMrp(mrp);
        product.setStatus(productActive ? ProductStatus.ACTIVE : ProductStatus.INACTIVE);

        ProductVariant variant = existingVariant == null ? new ProductVariant() : existingVariant;
        boolean variantCreated = existingVariant == null;
        if (variantCreated) {
            variant.setProduct(product);
            product.getVariants().add(variant);
        }
        String resolvedSku = sku == null ? generatedSku(brand, category, product.getDesignNumber(), color, size) : sku;
        variant.setSku(resolvedSku);
        variant.setColor(color);
        variant.setSize(size);
        variant.setCostPrice(variantCost);
        variant.setSellingPrice(variantSelling);
        variant.setMrp(variantMrp);
        variant.setReorderLevel(reorderLevel);
        variant.setActive(variantActive);

        products.save(product);
        syncProductDesignSequence(product.getDesignNumber());
        boolean createdRow = created || variantCreated;
        return new ImportExportResult("products", 1, createdRow ? 1 : 0, createdRow ? 0 : 1, 0, List.of());
    }

    private ImportExportResult importInventoryRow(Map<String, String> row) {
        String sku = required(row, "sku");
        ProductVariant variant = variants.findBySkuIgnoreCase(sku.trim())
                .orElseThrow(() -> new IllegalArgumentException("SKU not found: " + sku));
        InventoryTransactionType transactionType = InventoryTransactionType.valueOf(required(row, "transactionType").trim().toUpperCase(Locale.ROOT));
        int quantity = integer(row, "quantity", -1);
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        BigDecimal unitCost = decimal(row, "unitCost", null);
        String referenceType = defaultText(row, "referenceType", "IMPORT");
        String referenceId = trimToNull(value(row, "referenceId"));
        String notes = trimToNull(value(row, "notes"));
        String remarks = trimToNull(value(row, "remarks"));
        LocalDate transactionDate = localDate(row, "transactionDate");
        int previousStock = inventoryTransactions.balance(variant.getId());
        if (!transactionType.isInbound() && previousStock < quantity) {
            throw new IllegalArgumentException("Insufficient stock for SKU " + sku);
        }
        int newStock = transactionType.isInbound() ? previousStock + quantity : previousStock - quantity;
        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setVariant(variant);
        transaction.setTransactionType(transactionType);
        transaction.setQuantity(quantity);
        transaction.setUnitCost(unitCost);
        transaction.setReferenceType(referenceType);
        transaction.setReferenceId(referenceId);
        transaction.setNotes(notes);
        transaction.setRemarks(remarks);
        transaction.setPreviousStock(previousStock);
        transaction.setNewStock(newStock);
        transaction.setTransactionDate(transactionDate);
        inventoryTransactions.save(transaction);
        return new ImportExportResult("inventory", 1, 1, 0, 0, List.of());
    }

    private ImportExportResult importExpenseRow(Map<String, String> row) {
        String expenseNumber = trimToNull(value(row, "expenseNumber"));
        LocalDate expenseDate = localDate(row, "expenseDate");
        ExpenseCategory category = activeExpenseCategory(required(row, "categoryCode"));
        Supplier supplier = supplier(row);
        String description = required(row, "description");
        BigDecimal amount = requiredDecimal(row, "amount");
        BigDecimal taxAmount = decimal(row, "taxAmount", BigDecimal.ZERO);
        BigDecimal totalAmount = requiredDecimal(row, "totalAmount");
        ExpensePaymentMethod paymentMethod = ExpensePaymentMethod.valueOf(required(row, "paymentMethod").trim().toUpperCase(Locale.ROOT));
        ExpensePaymentStatus paymentStatus = ExpensePaymentStatus.valueOf(required(row, "paymentStatus").trim().toUpperCase(Locale.ROOT));
        if (amount.compareTo(BigDecimal.ZERO) < 0 || taxAmount.compareTo(BigDecimal.ZERO) < 0 || totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Expense amounts cannot be negative");
        }
        if (amount.add(taxAmount).compareTo(totalAmount) != 0) {
            throw new IllegalArgumentException("Expense total must equal amount plus tax");
        }

        Expense expense = new Expense();
        expense.setExpenseNumber(resolveExpenseNumber(expenseNumber, expenseDate));
        expense.setExpenseDate(expenseDate);
        expense.setCategory(category);
        expense.setSupplier(supplier);
        expense.setDescription(description.trim());
        expense.setAmount(amount);
        expense.setTaxAmount(taxAmount);
        expense.setTotalAmount(totalAmount);
        expense.setPaymentMethod(paymentMethod);
        expense.setPaymentStatus(paymentStatus);
        expense.setReferenceNumber(trimToNull(value(row, "referenceNumber")));
        expense.setRemarks(trimToNull(value(row, "remarks")));
        expenses.save(expense);
        syncExpenseSequenceIfNecessary(expense.getExpenseNumber());
        return new ImportExportResult("expenses", 1, 1, 0, 0, List.of());
    }

    private String resolveExpenseNumber(String provided, LocalDate expenseDate) {
        if (provided != null) {
            ensureExpenseNumberAvailable(provided, null);
            return provided;
        }
        return generateExpenseNumber(expenseDate);
    }

    private void ensureExpenseNumberAvailable(String expenseNumber, Long currentId) {
        expenses.findByExpenseNumberIgnoreCase(expenseNumber).ifPresent(existing -> {
            if (currentId == null || !existing.getId().equals(currentId)) {
                throw new IllegalArgumentException("Expense number already exists");
            }
        });
    }

    private String generateExpenseNumber(LocalDate expenseDate) {
        String year = String.valueOf(expenseDate.getYear());
        Integer maxSequence = expenses.findMaxSequenceForYear(year);
        int nextSequence = (maxSequence == null ? 0 : maxSequence) + 1;
        return "EXP-" + year + "-" + String.format("%06d", nextSequence);
    }

    private void syncExpenseSequenceIfNecessary(String expenseNumber) {
        Matcher matcher = EXPENSE_NUMBER_PATTERN.matcher(expenseNumber);
        if (!matcher.matches()) {
            return;
        }
        String year = matcher.group(1);
        int sequence = Integer.parseInt(matcher.group(2));
        Integer currentMax = expenses.findMaxSequenceForYear(year);
        if (sequence > (currentMax == null ? 0 : currentMax)) {
            expenses.flush();
        }
    }

    private void syncProductDesignSequence(Long designNumber) {
        Number current = (Number) entityManager.createNativeQuery("select last_value from product_design_number_seq").getSingleResult();
        if (designNumber > current.longValue()) {
            entityManager.createNativeQuery("select setval('product_design_number_seq', :value, true)")
                    .setParameter("value", designNumber)
                    .getSingleResult();
        }
    }

    private Long nextProductDesignNumber() {
        return ((Number) entityManager.createNativeQuery("SELECT nextval('product_design_number_seq')").getSingleResult()).longValue();
    }

    private String generatedSku(Brand brand, Category category, Long designNumber, Color color, Size size) {
        return brand.getCode()
                + "-"
                + category.getCode()
                + "-"
                + String.format("%04d", designNumber)
                + "-"
                + color.getCode()
                + "-"
                + size.getCode();
    }

    private Brand activeBrand(String code) {
        Brand brand = brands.findByCodeIgnoreCase(code).orElseThrow(() -> new IllegalArgumentException("Brand not found: " + code));
        if (!brand.isActive()) {
            throw new IllegalArgumentException("Brand must be active: " + code);
        }
        return brand;
    }

    private Category activeCategory(String code) {
        Category category = categories.findByCodeIgnoreCase(code).orElseThrow(() -> new IllegalArgumentException("Category not found: " + code));
        if (!category.isActive()) {
            throw new IllegalArgumentException("Category must be active: " + code);
        }
        return category;
    }

    private Color activeColor(String code) {
        Color color = colors.findByCodeIgnoreCase(code).orElseThrow(() -> new IllegalArgumentException("Color not found: " + code));
        if (!color.isActive()) {
            throw new IllegalArgumentException("Color must be active: " + code);
        }
        return color;
    }

    private Size activeSize(String code) {
        Size size = sizes.findByCodeIgnoreCase(code).orElseThrow(() -> new IllegalArgumentException("Size not found: " + code));
        if (!size.isActive()) {
            throw new IllegalArgumentException("Size must be active: " + code);
        }
        return size;
    }

    private ExpenseCategory activeExpenseCategory(String code) {
        ExpenseCategory category = expenseCategories.findByCodeIgnoreCase(code).orElseThrow(() -> new IllegalArgumentException("Expense category not found: " + code));
        if (!category.isActive()) {
            throw new IllegalArgumentException("Expense category must be active: " + code);
        }
        return category;
    }

    private Supplier supplier(Map<String, String> row) {
        String supplierCode = trimToNull(value(row, "supplierCode"));
        if (supplierCode == null) {
            return null;
        }
        Supplier supplier = suppliers.findBySupplierIdIgnoreCase(supplierCode)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + supplierCode));
        if (!supplier.isActive()) {
            throw new IllegalArgumentException("Supplier must be active: " + supplierCode);
        }
        return supplier;
    }

    private TableInput readTable(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        if (file.getSize() > MAX_UPLOAD_BYTES) {
            throw new IllegalArgumentException("Uploaded file must be 5 MB or smaller");
        }
        String filename = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        String contentType = Optional.ofNullable(file.getContentType()).orElse("").toLowerCase(Locale.ROOT);
        try {
            if (isCsv(filename, contentType)) {
                return readCsv(file);
            }
            if (isXlsx(filename, contentType)) {
                return readXlsx(file);
            }
            throw new IllegalArgumentException("Only CSV and XLSX files are supported");
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read uploaded file", exception);
        }
    }

    private boolean isCsv(String filename, String contentType) {
        return filename.endsWith(".csv") || CSV_CONTENT_TYPES.contains(contentType);
    }

    private boolean isXlsx(String filename, String contentType) {
        return filename.endsWith(".xlsx") || XLSX_CONTENT_TYPES.contains(contentType);
    }

    private TableInput readCsv(MultipartFile file) throws IOException {
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        if (content.isBlank()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }
        List<List<String>> rows = parseCsv(content);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }
        List<String> headers = rows.get(0);
        List<Map<String, String>> dataRows = new ArrayList<>();
        for (int index = 1; index < rows.size(); index++) {
            dataRows.add(rowMap(headers, rows.get(index)));
        }
        return new TableInput(headers, dataRows);
    }

    private TableInput readXlsx(MultipartFile file) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getNumberOfSheets() == 0 ? workbook.createSheet("Sheet1") : workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                throw new IllegalArgumentException("Uploaded file is empty");
            }
            DataFormatter formatter = new DataFormatter();
            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(formatter.formatCellValue(cell));
            }
            if (headers.stream().allMatch(String::isBlank)) {
                throw new IllegalArgumentException("Uploaded file is empty");
            }
            List<Map<String, String>> rows = new ArrayList<>();
            for (int rowIndex = sheet.getFirstRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                List<String> values = new ArrayList<>();
                for (int columnIndex = 0; columnIndex < headers.size(); columnIndex++) {
                    Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    values.add(cell == null ? "" : formatter.formatCellValue(cell));
                }
                rows.add(rowMap(headers, values));
            }
            return new TableInput(headers, rows);
        }
    }

    private List<List<String>> parseCsv(String content) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < content.length(); index++) {
            char character = content.charAt(index);
            if (quoted) {
                if (character == '"') {
                    if (index + 1 < content.length() && content.charAt(index + 1) == '"') {
                        cell.append('"');
                        index++;
                    } else {
                        quoted = false;
                    }
                } else {
                    cell.append(character);
                }
            } else if (character == '"') {
                quoted = true;
            } else if (character == ',') {
                row.add(cell.toString());
                cell.setLength(0);
            } else if (character == '\n') {
                row.add(cell.toString());
                rows.add(new ArrayList<>(row));
                row.clear();
                cell.setLength(0);
            } else if (character != '\r') {
                cell.append(character);
            }
        }
        row.add(cell.toString());
        if (!row.isEmpty()) {
            rows.add(row);
        }
        return rows;
    }

    private Map<String, String> rowMap(List<String> headers, List<String> values) {
        Map<String, String> row = new HashMap<>();
        for (int index = 0; index < headers.size(); index++) {
            row.put(normalizeHeader(headers.get(index)), index < values.size() ? values.get(index) : "");
        }
        return row;
    }

    private byte[] writeTable(TableData table, ImportExportFormat format) {
        try {
            if (format == ImportExportFormat.CSV) {
                return writeCsv(table).getBytes(StandardCharsets.UTF_8);
            }
            return writeXlsx(table);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to generate export file", exception);
        }
    }

    private String writeCsv(TableData table) {
        StringBuilder builder = new StringBuilder();
        builder.append(csvRow(table.headers()));
        for (List<String> row : table.rows()) {
            builder.append('\n').append(csvRow(row));
        }
        return builder.toString();
    }

    private String csvRow(List<String> values) {
        List<String> cells = new ArrayList<>();
        for (String value : values) {
            String cell = value == null ? "" : value;
            if (cell.contains("\"")) {
                cell = cell.replace("\"", "\"\"");
            }
            if (cell.contains(",") || cell.contains("\n") || cell.contains("\"")) {
                cell = '"' + cell + '"';
            }
            cells.add(cell);
        }
        return String.join(",", cells);
    }

    private byte[] writeXlsx(TableData table) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(table.sheetName());
            Row headerRow = sheet.createRow(0);
            for (int columnIndex = 0; columnIndex < table.headers().size(); columnIndex++) {
                headerRow.createCell(columnIndex, CellType.STRING).setCellValue(table.headers().get(columnIndex));
            }
            for (int rowIndex = 0; rowIndex < table.rows().size(); rowIndex++) {
                Row sheetRow = sheet.createRow(rowIndex + 1);
                List<String> values = table.rows().get(rowIndex);
                for (int columnIndex = 0; columnIndex < values.size(); columnIndex++) {
                    sheetRow.createCell(columnIndex, CellType.STRING).setCellValue(values.get(columnIndex) == null ? "" : values.get(columnIndex));
                }
            }
            for (int columnIndex = 0; columnIndex < table.headers().size(); columnIndex++) {
                sheet.autoSizeColumn(columnIndex);
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private List<String> productExportHeaders() {
        return List.of(
                "productId",
                "designNumber",
                "productName",
                "brandCode",
                "brandName",
                "categoryCode",
                "categoryName",
                "categoryGroup",
                "description",
                "fabric",
                "fit",
                "neckType",
                "sleeveType",
                "gender",
                "sourcingType",
                "productCost",
                "sellingPrice",
                "mrp",
                "variantId",
                "sku",
                "colorCode",
                "colorName",
                "sizeCode",
                "sizeName",
                "variantCostPrice",
                "variantSellingPrice",
                "variantMrp",
                "reorderLevel",
                "productStatus",
                "variantActive",
                "createdAt",
                "updatedAt");
    }

    private List<String> productImportHeaders() {
        return List.of(
                "designNumber",
                "productName",
                "brandCode",
                "categoryCode",
                "description",
                "fabric",
                "fit",
                "neckType",
                "sleeveType",
                "gender",
                "sourcingType",
                "productCost",
                "sellingPrice",
                "mrp",
                "colorCode",
                "sizeCode",
                "sku",
                "variantCostPrice",
                "variantSellingPrice",
                "variantMrp",
                "reorderLevel",
                "productStatus",
                "variantActive");
    }

    private List<String> inventoryExportHeaders() {
        return List.of(
                "variantId",
                "sku",
                "designNumber",
                "productName",
                "brandCode",
                "brandName",
                "categoryCode",
                "categoryName",
                "colorCode",
                "colorName",
                "sizeCode",
                "sizeName",
                "currentStock",
                "reorderLevel",
                "unitCost",
                "inventoryValue",
                "productStatus",
                "variantActive");
    }

    private List<String> inventoryImportHeaders() {
        return List.of(
                "sku",
                "transactionType",
                "quantity",
                "unitCost",
                "referenceType",
                "referenceId",
                "notes",
                "remarks",
                "transactionDate");
    }

    private List<String> orderExportHeaders() {
        return List.of(
                "orderId",
                "marketplaceCode",
                "marketplaceName",
                "orderDate",
                "customerName",
                "customerPhone",
                "shippingAddress",
                "city",
                "state",
                "pincode",
                "trackingNumber",
                "courierPartner",
                "remarks",
                "orderStatus",
                "paymentStatus",
                "inventoryDeducted",
                "totalOrderValue",
                "commission",
                "shippingCharge",
                "otherCharges",
                "netAmount",
                "sku",
                "productName",
                "colorCode",
                "colorName",
                "sizeCode",
                "sizeName",
                "quantity",
                "sellingPrice",
                "lineTotal");
    }

    private List<String> purchaseExportHeaders() {
        return List.of(
                "purchaseId",
                "supplierCode",
                "supplierName",
                "purchaseDate",
                "invoiceNumber",
                "invoiceDate",
                "purchaseStatus",
                "paymentStatus",
                "inventoryReceived",
                "remarks",
                "sku",
                "productName",
                "colorCode",
                "colorName",
                "sizeCode",
                "sizeName",
                "quantity",
                "unitCost",
                "tax",
                "lineTotal",
                "subtotal",
                "totalAmount",
                "paidAmount",
                "dueAmount",
                "otherCharges");
    }

    private List<String> expenseExportHeaders() {
        return List.of(
                "expenseNumber",
                "expenseDate",
                "categoryCode",
                "categoryName",
                "supplierCode",
                "supplierName",
                "description",
                "amount",
                "taxAmount",
                "totalAmount",
                "paymentMethod",
                "paymentStatus",
                "referenceNumber",
                "remarks",
                "createdAt",
                "updatedAt");
    }

    private List<String> expenseImportHeaders() {
        return List.of(
                "expenseNumber",
                "expenseDate",
                "categoryCode",
                "supplierCode",
                "description",
                "amount",
                "taxAmount",
                "totalAmount",
                "paymentMethod",
                "paymentStatus",
                "referenceNumber",
                "remarks");
    }

    private Map<Long, Integer> balancesByVariantIds(List<Long> variantIds) {
        if (variantIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Integer> balances = new HashMap<>();
        for (Object[] row : inventoryTransactions.balancesByVariantIds(variantIds)) {
            Long variantId = ((Number) row[0]).longValue();
            Integer currentStock = ((Number) row[1]).intValue();
            balances.put(variantId, currentStock);
        }
        return balances;
    }

    private String required(Map<String, String> row, String key) {
        String value = trimToNull(value(row, key));
        if (value == null) {
            throw new IllegalArgumentException(key + " is required");
        }
        return value;
    }

    private Long longValue(Map<String, String> row, String key) {
        String value = trimToNull(value(row, key));
        return value == null ? null : Long.valueOf(value);
    }

    private int integer(Map<String, String> row, String key, int defaultValue) {
        String value = trimToNull(value(row, key));
        return value == null ? defaultValue : Integer.parseInt(value);
    }

    private BigDecimal requiredDecimal(Map<String, String> row, String key) {
        String value = required(row, key);
        return new BigDecimal(value);
    }

    private BigDecimal decimal(Map<String, String> row, String key, BigDecimal defaultValue) {
        String value = trimToNull(value(row, key));
        return value == null ? defaultValue : new BigDecimal(value);
    }

    private LocalDate localDate(Map<String, String> row, String key) {
        return LocalDate.parse(required(row, key), ISO_DATE);
    }

    private boolean booleanValue(Map<String, String> row, String key, boolean defaultValue) {
        String value = trimToNull(value(row, key));
        if (value == null) {
            return defaultValue;
        }
        return value.equalsIgnoreCase("true")
                || value.equalsIgnoreCase("yes")
                || value.equalsIgnoreCase("y")
                || value.equals("1")
                || value.equalsIgnoreCase("active");
    }

    private String defaultText(Map<String, String> row, String key, String defaultValue) {
        String value = trimToNull(value(row, key));
        return value == null ? defaultValue : value;
    }

    private String value(Map<String, String> row, String key) {
        String normalized = normalizeHeader(key);
        return row.get(normalized);
    }

    private String normalizeHeader(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String text(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros().toPlainString();
        }
        return value.toString();
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String money(BigDecimal value) {
        return safe(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private List<String> row(String... values) {
        List<String> row = new ArrayList<>(values.length);
        for (String value : values) {
            row.add(value == null ? "" : value);
        }
        return row;
    }

    private record TableData(String sheetName, List<String> headers, List<List<String>> rows) {
    }

    private record TableInput(List<String> headers, List<Map<String, String>> rows) {
    }

    private final class ImportOutcome {
        private final String module;
        private int processed;
        private int created;
        private int updated;
        private final List<ImportExportError> errors = new ArrayList<>();

        private ImportOutcome(String module) {
            this.module = module;
        }

        private void record(ImportExportResult rowResult) {
            processed += rowResult.processed();
            created += rowResult.created();
            updated += rowResult.updated();
            errors.addAll(rowResult.errors());
        }

        private void fail(int rowNumber, String message) {
            processed++;
            errors.add(new ImportExportError(rowNumber, message));
        }

        private ImportExportResult toResult() {
            int failed = errors.size();
            return new ImportExportResult(module, processed, created, updated, failed, List.copyOf(errors));
        }
    }
}
