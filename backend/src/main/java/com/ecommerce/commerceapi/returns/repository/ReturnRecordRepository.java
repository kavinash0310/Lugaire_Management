package com.ecommerce.commerceapi.returns.repository;

import com.ecommerce.commerceapi.orders.domain.OrderPlatform;
import com.ecommerce.commerceapi.returns.domain.ReturnRecord;
import com.ecommerce.commerceapi.returns.domain.ReturnStatus;
import com.ecommerce.commerceapi.returns.domain.ReturnType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface ReturnRecordRepository extends JpaRepository<ReturnRecord, Long> {
    @Query("select coalesce(sum(record.quantity), 0) from ReturnRecord record where record.orderItem.id = :orderItemId")
    int returnedQuantity(@Param("orderItemId") Long orderItemId);

    @Query("""
            select record from ReturnRecord record
            where (:search = '' or lower(record.order.orderId) like lower(concat('%', :search, '%')) or lower(record.variant.sku) like lower(concat('%', :search, '%')))
              and (:platform is null or record.order.platform = :platform)
              and (:type is null or record.type = :type)
              and (:status is null or record.status = :status)
              and (:fromDate is null or record.returnDate >= :fromDate)
              and (:toDate is null or record.returnDate <= :toDate)
            """)
    Page<ReturnRecord> search(@Param("search") String search,
                              @Param("platform") OrderPlatform platform,
                              @Param("type") ReturnType type,
                              @Param("status") ReturnStatus status,
                              @Param("fromDate") LocalDate fromDate,
                              @Param("toDate") LocalDate toDate,
                              Pageable pageable);

    List<ReturnRecord> findAllByReturnDateBetween(LocalDate fromDate, LocalDate toDate);
}
