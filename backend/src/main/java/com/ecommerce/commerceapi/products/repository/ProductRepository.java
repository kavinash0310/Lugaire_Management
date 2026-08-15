package com.ecommerce.commerceapi.products.repository;

import com.ecommerce.commerceapi.products.domain.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @EntityGraph(attributePaths = {"brand", "category", "variants", "variants.color", "variants.size"})
    Optional<Product> findWithDetailsById(Long id);

    Optional<Product> findByDesignNumber(Long designNumber);

    @EntityGraph(attributePaths = {"brand", "category", "variants", "variants.color", "variants.size"})
    List<Product> findAllByOrderByDesignNumberAsc();
}
