package com.ecommerce.commerceapi.masters.repository;

import com.ecommerce.commerceapi.masters.domain.Color;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ColorRepository extends JpaRepository<Color, Long> {
    Optional<Color> findByCodeIgnoreCase(String code);
}
