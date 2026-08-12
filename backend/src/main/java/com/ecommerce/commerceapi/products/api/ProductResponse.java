package com.ecommerce.commerceapi.products.api;
import java.math.BigDecimal; import java.time.Instant; import java.util.List;
public record ProductResponse(Long id,String designNumber,String productName,Long brandId,String brandName,Long categoryId,String categoryName,String sourcingType,BigDecimal productCost,BigDecimal sellingPrice,BigDecimal mrp,String status,Instant createdAt,List<VariantResponse> variants){}
