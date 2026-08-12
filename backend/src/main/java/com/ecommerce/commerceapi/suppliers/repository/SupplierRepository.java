package com.ecommerce.commerceapi.suppliers.repository;

import com.ecommerce.commerceapi.suppliers.domain.Supplier;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    Optional<Supplier> findBySupplierIdIgnoreCase(String supplierId);

    @Query("""
            select supplier from Supplier supplier
            where (:search = '' or lower(supplier.name) like lower(concat('%', :search, '%'))
               or lower(supplier.supplierId) like lower(concat('%', :search, '%'))
               or lower(coalesce(supplier.contactPerson, '')) like lower(concat('%', :search, '%')))
              and (:active is null or supplier.active = :active)
            """)
    Page<Supplier> search(@Param("search") String search, @Param("active") Boolean active, Pageable pageable);
}
