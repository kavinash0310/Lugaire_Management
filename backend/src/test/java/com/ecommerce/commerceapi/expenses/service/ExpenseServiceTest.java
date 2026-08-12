package com.ecommerce.commerceapi.expenses.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ecommerce.commerceapi.expenses.api.ExpenseRequest;
import com.ecommerce.commerceapi.expenses.api.ExpensePaymentStatusRequest;
import com.ecommerce.commerceapi.expenses.domain.Expense;
import com.ecommerce.commerceapi.expenses.domain.ExpensePaymentMethod;
import com.ecommerce.commerceapi.expenses.domain.ExpensePaymentStatus;
import com.ecommerce.commerceapi.expenses.repository.ExpenseRepository;
import com.ecommerce.commerceapi.masters.domain.ExpenseCategory;
import com.ecommerce.commerceapi.masters.repository.ExpenseCategoryRepository;
import com.ecommerce.commerceapi.suppliers.domain.Supplier;
import com.ecommerce.commerceapi.suppliers.repository.SupplierRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ExpenseServiceTest {
    private final ExpenseRepository expenses = mock(ExpenseRepository.class);
    private final ExpenseCategoryRepository categories = mock(ExpenseCategoryRepository.class);
    private final SupplierRepository suppliers = mock(SupplierRepository.class);
    private final ExpenseService service = new ExpenseService(expenses, categories, suppliers);

    @BeforeEach
    void setUp() {
        when(expenses.save(any())).thenAnswer(invocation -> {
            Expense expense = invocation.getArgument(0);
            if (expense.getId() == null) {
                ReflectionTestUtils.setField(expense, "id", 1L);
            }
            return expense;
        });
        when(categories.findById(1L)).thenReturn(Optional.of(category(1L, "Packaging", "PKG")));
        when(categories.findById(2L)).thenReturn(Optional.of(category(2L, "Courier", "CUR")));
        when(suppliers.findById(2L)).thenReturn(Optional.of(supplier(2L, "Pack Pro", "SUP-001")));
        when(expenses.findMaxSequenceForYear("2026")).thenReturn(0);
    }

    @Test
    void createExpenseCalculatesTotal() {
        when(expenses.findByExpenseNumberIgnoreCase("EXP-2026-000001")).thenReturn(Optional.empty());

        var created = service.create(request(1L, null, BigDecimal.valueOf(500), BigDecimal.ZERO, BigDecimal.valueOf(500), ExpensePaymentStatus.PENDING));

        assertEquals("EXP-2026-000001", created.expenseNumber());
        assertEquals(BigDecimal.valueOf(500), created.totalAmount());
        assertEquals(ExpensePaymentStatus.PENDING.name(), created.paymentStatus());
    }

    @Test
    void createExpenseWithTaxCalculatesTotal() {
        when(expenses.findByExpenseNumberIgnoreCase("EXP-2026-000001")).thenReturn(Optional.empty());

        var created = service.create(request(1L, null, BigDecimal.valueOf(1000), BigDecimal.valueOf(180), BigDecimal.valueOf(1180), ExpensePaymentStatus.PENDING));

        assertEquals(BigDecimal.valueOf(1180), created.totalAmount());
    }

    @Test
    void negativeAmountIsRejected() {
        when(expenses.findByExpenseNumberIgnoreCase("EXP-2026-000001")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.create(request(1L, null, BigDecimal.valueOf(-100), BigDecimal.ZERO, BigDecimal.valueOf(-100), ExpensePaymentStatus.PENDING)));
    }

    @Test
    void negativeTaxIsRejected() {
        when(expenses.findByExpenseNumberIgnoreCase("EXP-2026-000001")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.create(request(1L, null, BigDecimal.valueOf(100), BigDecimal.valueOf(-50), BigDecimal.valueOf(50), ExpensePaymentStatus.PENDING)));
    }

    @Test
    void duplicateExpenseNumberIsRejected() {
        Expense existing = new Expense();
        ReflectionTestUtils.setField(existing, "id", 9L);
        existing.setExpenseNumber("EXP-2026-000001");
        when(expenses.findByExpenseNumberIgnoreCase("EXP-2026-000001")).thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class, () -> service.create(request(1L, null, BigDecimal.valueOf(500), BigDecimal.ZERO, BigDecimal.valueOf(500), ExpensePaymentStatus.PENDING)));
    }

    @Test
    void invalidCategoryIsRejected() {
        when(expenses.findByExpenseNumberIgnoreCase("EXP-2026-000001")).thenReturn(Optional.empty());
        when(categories.findById(99L)).thenReturn(Optional.empty());

        assertThrows(jakarta.persistence.EntityNotFoundException.class, () -> service.create(request(99L, null, BigDecimal.valueOf(500), BigDecimal.ZERO, BigDecimal.valueOf(500), ExpensePaymentStatus.PENDING)));
    }

    @Test
    void supplierIsStoredWhenProvided() {
        when(expenses.findByExpenseNumberIgnoreCase("EXP-2026-000001")).thenReturn(Optional.empty());

        var created = service.create(request(1L, 2L, BigDecimal.valueOf(500), BigDecimal.ZERO, BigDecimal.valueOf(500), ExpensePaymentStatus.PENDING));

        assertEquals(Long.valueOf(2L), created.supplierId());
        assertEquals("Pack Pro", created.supplierName());
    }

    @Test
    void paymentStatusCanBeUpdated() {
        Expense expense = expense(1L, "EXP-2026-000001", ExpensePaymentStatus.PENDING);
        when(expenses.findById(1L)).thenReturn(Optional.of(expense));

        var updated = service.updatePaymentStatus(1L, new ExpensePaymentStatusRequest(ExpensePaymentStatus.PAID));

        assertEquals(ExpensePaymentStatus.PAID.name(), updated.paymentStatus());
    }

    @Test
    void searchReturnsMatchingPage() {
        Expense expense = expense(1L, "EXP-2026-000001", ExpensePaymentStatus.PENDING);
        when(expenses.search(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(expense)));

        var page = service.list("pack", 1L, null, null, null, null, null, null, null, 0, 20);

        assertEquals(1, page.content().size());
    }

    private ExpenseRequest request(Long categoryId, Long supplierId, BigDecimal amount, BigDecimal tax, BigDecimal total, ExpensePaymentStatus paymentStatus) {
        return new ExpenseRequest(LocalDate.now(), categoryId, supplierId, "Packaging charges", amount, tax, total, ExpensePaymentMethod.CASH, paymentStatus, "REF-1", "Test");
    }

    private Expense expense(Long id, String expenseNumber, ExpensePaymentStatus paymentStatus) {
        ExpenseCategory category = category(id, "Packaging", "PKG");
        Expense expense = new Expense();
        ReflectionTestUtils.setField(expense, "id", id);
        expense.setExpenseNumber(expenseNumber);
        expense.setExpenseDate(LocalDate.now());
        expense.setCategory(category);
        expense.setDescription("Packaging charges");
        expense.setAmount(BigDecimal.valueOf(500));
        expense.setTaxAmount(BigDecimal.ZERO);
        expense.setTotalAmount(BigDecimal.valueOf(500));
        expense.setPaymentMethod(ExpensePaymentMethod.CASH);
        expense.setPaymentStatus(paymentStatus);
        return expense;
    }

    private ExpenseCategory category(Long id, String name, String code) {
        ExpenseCategory category = new ExpenseCategory();
        ReflectionTestUtils.setField(category, "id", id);
        category.setName(name);
        category.setCode(code);
        return category;
    }

    private Supplier supplier(Long id, String name, String supplierId) {
        Supplier supplier = new Supplier();
        ReflectionTestUtils.setField(supplier, "id", id);
        supplier.setName(name);
        supplier.setSupplierId(supplierId);
        return supplier;
    }
}
