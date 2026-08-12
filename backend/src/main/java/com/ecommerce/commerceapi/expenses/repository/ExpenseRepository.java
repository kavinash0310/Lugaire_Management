package com.ecommerce.commerceapi.expenses.repository;

import com.ecommerce.commerceapi.expenses.domain.Expense;
import com.ecommerce.commerceapi.expenses.domain.ExpensePaymentMethod;
import com.ecommerce.commerceapi.expenses.domain.ExpensePaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    Optional<Expense> findByExpenseNumberIgnoreCase(String expenseNumber);

    @Query(value = """
            select coalesce(max(cast(substring(expense_number from 10 for 6) as integer)), 0)
            from expenses
            where expense_number like concat('EXP-', :year, '-%')
            """, nativeQuery = true)
    Integer findMaxSequenceForYear(@Param("year") String year);

    @Query("""
            select expense from Expense expense
            left join expense.category category
            left join expense.supplier supplier
            where (:search = '' or lower(expense.expenseNumber) like lower(concat('%', :search, '%'))
               or lower(expense.description) like lower(concat('%', :search, '%'))
               or lower(coalesce(expense.referenceNumber, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(category.name, '')) like lower(concat('%', :search, '%'))
               or lower(coalesce(supplier.name, '')) like lower(concat('%', :search, '%')))
              and (:categoryId is null or category.id = :categoryId)
              and (:supplierId is null or supplier.id = :supplierId)
              and (:paymentStatus is null or expense.paymentStatus = :paymentStatus)
              and (:paymentMethod is null or expense.paymentMethod = :paymentMethod)
              and (:fromDate is null or expense.expenseDate >= :fromDate)
              and (:toDate is null or expense.expenseDate <= :toDate)
              and (:minAmount is null or expense.totalAmount >= :minAmount)
              and (:maxAmount is null or expense.totalAmount <= :maxAmount)
            """)
    Page<Expense> search(@Param("search") String search,
                         @Param("categoryId") Long categoryId,
                         @Param("supplierId") Long supplierId,
                         @Param("paymentStatus") ExpensePaymentStatus paymentStatus,
                         @Param("paymentMethod") ExpensePaymentMethod paymentMethod,
                         @Param("fromDate") LocalDate fromDate,
                         @Param("toDate") LocalDate toDate,
                         @Param("minAmount") BigDecimal minAmount,
                         @Param("maxAmount") BigDecimal maxAmount,
                         Pageable pageable);

    List<Expense> findAllByExpenseDateBetween(LocalDate fromDate, LocalDate toDate);
}
