package com.ecommerce.commerceapi.orders.repository;
import com.ecommerce.commerceapi.orders.domain.OrderItem;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    @Query("""
            select item from OrderItem item
            join fetch item.order orderEntity
            join fetch item.variant variant
            join fetch variant.product product
            join fetch variant.color color
            join fetch variant.size size
            where orderEntity.orderDate between :fromDate and :toDate
            """)
    List<OrderItem> findForReporting(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);
}
