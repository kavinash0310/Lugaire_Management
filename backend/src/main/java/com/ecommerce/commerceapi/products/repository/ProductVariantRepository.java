package com.ecommerce.commerceapi.products.repository;

import com.ecommerce.commerceapi.products.domain.ProductVariant;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    Optional<ProductVariant> findBySkuIgnoreCase(String sku);

    Optional<ProductVariant> findBySkuIgnoreCaseAndIdNot(String sku, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select variant from ProductVariant variant where variant.id = :id")
    Optional<ProductVariant> findByIdForInventoryUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {
            "product",
            "product.brand",
            "product.category",
            "color",
            "size"
    })
    @Query("""
            select variant from ProductVariant variant
            join variant.product product
            join product.brand brand
            join product.category category
            join variant.color color
            join variant.size size
            where (:query = '' or lower(variant.sku) like lower(concat('%', :query, '%'))
               or lower(product.productName) like lower(concat('%', :query, '%'))
               or lower(brand.name) like lower(concat('%', :query, '%'))
               or lower(brand.code) like lower(concat('%', :query, '%'))
               or lower(category.name) like lower(concat('%', :query, '%'))
               or lower(category.code) like lower(concat('%', :query, '%'))
               or lower(color.name) like lower(concat('%', :query, '%'))
               or lower(color.code) like lower(concat('%', :query, '%'))
               or lower(size.name) like lower(concat('%', :query, '%'))
               or lower(size.code) like lower(concat('%', :query, '%')))
            order by product.productName asc, color.name asc, size.name asc
            """)
    List<ProductVariant> searchInventory(@Param("query") String query);

    @EntityGraph(attributePaths = {
            "product",
            "product.brand",
            "product.category",
            "color",
            "size"
    })
    Optional<ProductVariant> findInventoryViewById(Long id);

    @EntityGraph(attributePaths = {
            "product",
            "product.brand",
            "product.category",
            "color",
            "size"
    })
    @Query("""
            select variant from ProductVariant variant
            join variant.product product
            join product.brand brand
            join product.category category
            join variant.color color
            join variant.size size
            where (:query = '' or lower(variant.sku) like lower(concat('%', :query, '%'))
               or lower(product.productName) like lower(concat('%', :query, '%'))
               or lower(brand.name) like lower(concat('%', :query, '%'))
               or lower(brand.code) like lower(concat('%', :query, '%'))
               or lower(category.name) like lower(concat('%', :query, '%'))
               or lower(category.code) like lower(concat('%', :query, '%'))
               or lower(color.name) like lower(concat('%', :query, '%'))
               or lower(color.code) like lower(concat('%', :query, '%'))
               or lower(size.name) like lower(concat('%', :query, '%'))
               or lower(size.code) like lower(concat('%', :query, '%')))
            order by product.productName asc, color.name asc, size.name asc
            """)
    List<ProductVariant> searchPricing(@Param("query") String query);

    @EntityGraph(attributePaths = {
            "product",
            "product.brand",
            "product.category",
            "color",
            "size"
    })
    Optional<ProductVariant> findPricingViewById(Long id);
}
