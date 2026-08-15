package com.ecommerce.commerceapi.marketplaces.repository;

import com.ecommerce.commerceapi.marketplaces.domain.Marketplace;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketplaceRepository extends JpaRepository<Marketplace, Long> {
    Optional<Marketplace> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
}
