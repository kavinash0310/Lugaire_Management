package com.ecommerce.commerceapi.products.service;

import com.ecommerce.commerceapi.audit.service.AuditLogService;
import com.ecommerce.commerceapi.masters.domain.Brand;
import com.ecommerce.commerceapi.masters.domain.Category;
import com.ecommerce.commerceapi.masters.domain.Color;
import com.ecommerce.commerceapi.masters.domain.MasterDataEntity;
import com.ecommerce.commerceapi.masters.domain.Size;
import com.ecommerce.commerceapi.masters.repository.BrandRepository;
import com.ecommerce.commerceapi.masters.repository.CategoryRepository;
import com.ecommerce.commerceapi.masters.repository.ColorRepository;
import com.ecommerce.commerceapi.masters.repository.SizeRepository;
import com.ecommerce.commerceapi.products.api.ProductRequest;
import com.ecommerce.commerceapi.products.api.ProductResponse;
import com.ecommerce.commerceapi.products.api.ProductStatusRequest;
import com.ecommerce.commerceapi.products.api.VariantResponse;
import com.ecommerce.commerceapi.products.domain.Product;
import com.ecommerce.commerceapi.products.domain.ProductStatus;
import com.ecommerce.commerceapi.products.domain.ProductVariant;
import com.ecommerce.commerceapi.products.repository.ProductRepository;
import com.ecommerce.commerceapi.settings.service.SystemSettingsService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
    private final ProductRepository products;
    private final BrandRepository brands;
    private final CategoryRepository categories;
    private final ColorRepository colors;
    private final SizeRepository sizes;
    private final AuditLogService auditLogs;
    private final SystemSettingsService settings;

    @PersistenceContext
    private EntityManager entityManager;

    public ProductService(
            ProductRepository products,
            BrandRepository brands,
            CategoryRepository categories,
            ColorRepository colors,
            SizeRepository sizes,
            AuditLogService auditLogs,
            SystemSettingsService settings) {
        this.products = products;
        this.brands = brands;
        this.categories = categories;
        this.colors = colors;
        this.sizes = sizes;
        this.auditLogs = auditLogs;
        this.settings = settings;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        return products.findAll().stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        return response(products.findWithDetailsById(id).orElseThrow(() -> new EntityNotFoundException("Product not found")));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Brand brand = active(brands.findById(request.brandId()).orElseThrow(() -> new EntityNotFoundException("Brand not found")), "Brand");
        Category category = active(categories.findById(request.categoryId()).orElseThrow(() -> new EntityNotFoundException("Category not found")), "Category");
        List<Color> selectedColors = activeColors(request.colorIds());
        List<Size> selectedSizes = activeSizes(request.sizeIds());
        int defaultReorderLevel = settings.defaultLowStockThreshold();

        Product product = new Product();
        apply(product, request, brand, category);
        product.setDesignNumber(((Number) entityManager.createNativeQuery("SELECT nextval('product_design_number_seq')").getSingleResult()).longValue());
        for (Color color : selectedColors) {
            for (Size size : selectedSizes) {
                ProductVariant variant = new ProductVariant();
                variant.setProduct(product);
                variant.setColor(color);
                variant.setSize(size);
                variant.setSku(brand.getCode() + "-" + category.getCode() + "-" + String.format("%04d", product.getDesignNumber()) + "-" + color.getCode() + "-" + size.getCode());
                variant.setCostPrice(request.productCost());
                variant.setSellingPrice(request.sellingPrice());
                variant.setMrp(request.mrp());
                variant.setReorderLevel(Optional.ofNullable(request.reorderLevel()).orElse(defaultReorderLevel));
                product.getVariants().add(variant);
            }
        }

        Product saved = products.save(product);
        auditLogs.log(
                "CREATE",
                "PRODUCT",
                "Product",
                String.valueOf(saved.getId()),
                "Product created with " + saved.getVariants().size() + " SKUs",
                null,
                productState(saved));
        return response(saved);
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = products.findWithDetailsById(id).orElseThrow(() -> new EntityNotFoundException("Product not found"));
        Map<String, Object> oldValue = productState(product);
        Brand brand = active(brands.findById(request.brandId()).orElseThrow(() -> new EntityNotFoundException("Brand not found")), "Brand");
        Category category = active(categories.findById(request.categoryId()).orElseThrow(() -> new EntityNotFoundException("Category not found")), "Category");
        if (!product.getBrand().getId().equals(brand.getId()) || !product.getCategory().getId().equals(category.getId())) {
            throw new IllegalArgumentException("Brand and category cannot be changed after SKU creation");
        }
        apply(product, request, brand, category);
        Product saved = products.save(product);
        auditLogs.log(
                "UPDATE",
                "PRODUCT",
                "Product",
                String.valueOf(saved.getId()),
                "Product updated",
                oldValue,
                productState(saved));
        return response(saved);
    }

    @Transactional
    public ProductResponse updateStatus(Long id, ProductStatus status) {
        Product product = products.findById(id).orElseThrow(() -> new EntityNotFoundException("Product not found"));
        ProductStatus previous = product.getStatus();
        product.setStatus(status);
        Product saved = products.save(product);
        auditLogs.log(
                status == ProductStatus.ACTIVE ? "ACTIVATE" : "DEACTIVATE",
                "PRODUCT",
                "Product",
                String.valueOf(saved.getId()),
                "Product status changed",
                Map.of("status", previous.name()),
                Map.of("status", saved.getStatus().name()));
        return response(saved);
    }

    private void apply(Product product, ProductRequest request, Brand brand, Category category) {
        product.setProductName(request.productName().trim());
        product.setBrand(brand);
        product.setCategory(category);
        product.setDescription(request.description());
        product.setFabric(request.fabric());
        product.setFit(request.fit());
        product.setNeckType(request.neckType());
        product.setSleeveType(request.sleeveType());
        product.setGender(request.gender());
        product.setSourcingType(request.sourcingType());
        product.setProductCost(request.productCost());
        product.setSellingPrice(request.sellingPrice());
        product.setMrp(request.mrp());
    }

    private List<Color> activeColors(List<Long> ids) {
        if (new HashSet<>(ids).size() != ids.size()) {
            throw new IllegalArgumentException("Colors must be unique");
        }
        List<Color> result = colors.findAllById(ids);
        if (result.size() != ids.size() || result.stream().anyMatch(v -> !v.isActive())) {
            throw new IllegalArgumentException("All selected colors must exist and be active");
        }
        return result;
    }

    private List<Size> activeSizes(List<Long> ids) {
        if (new HashSet<>(ids).size() != ids.size()) {
            throw new IllegalArgumentException("Sizes must be unique");
        }
        List<Size> result = sizes.findAllById(ids);
        if (result.size() != ids.size() || result.stream().anyMatch(v -> !v.isActive())) {
            throw new IllegalArgumentException("All selected sizes must exist and be active");
        }
        return result;
    }

    private <T extends MasterDataEntity> T active(T item, String label) {
        if (!item.isActive()) {
            throw new IllegalArgumentException(label + " must be active");
        }
        return item;
    }

    private ProductResponse response(Product product) {
        return new ProductResponse(
                product.getId(),
                String.format("%04d", product.getDesignNumber()),
                product.getProductName(),
                product.getBrand().getId(),
                product.getBrand().getName(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getSourcingType().name(),
                product.getProductCost(),
                product.getSellingPrice(),
                product.getMrp(),
                product.getStatus().name(),
                product.getCreatedAt(),
                product.getVariants().stream()
                        .map(variant -> new VariantResponse(
                                variant.getId(),
                                variant.getSku(),
                                variant.getColor().getId(),
                                variant.getColor().getName(),
                                variant.getSize().getId(),
                                variant.getSize().getName(),
                                variant.getCostPrice(),
                                variant.getSellingPrice(),
                                variant.getMrp(),
                                variant.getReorderLevel(),
                                variant.isActive()))
                        .toList());
    }

    private Map<String, Object> productState(Product product) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("designNumber", product.getDesignNumber());
        state.put("productName", product.getProductName());
        state.put("brand", product.getBrand() == null ? null : product.getBrand().getCode());
        state.put("category", product.getCategory() == null ? null : product.getCategory().getCode());
        state.put("description", product.getDescription());
        state.put("fabric", product.getFabric());
        state.put("fit", product.getFit());
        state.put("neckType", product.getNeckType());
        state.put("sleeveType", product.getSleeveType());
        state.put("gender", product.getGender());
        state.put("sourcingType", product.getSourcingType() == null ? null : product.getSourcingType().name());
        state.put("productCost", product.getProductCost());
        state.put("sellingPrice", product.getSellingPrice());
        state.put("mrp", product.getMrp());
        state.put("status", product.getStatus() == null ? null : product.getStatus().name());
        state.put("skuCount", product.getVariants() == null ? 0 : product.getVariants().size());
        return state;
    }
}
