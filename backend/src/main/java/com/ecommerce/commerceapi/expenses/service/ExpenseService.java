package com.ecommerce.commerceapi.expenses.service;

import com.ecommerce.commerceapi.expenses.api.ExpensePageResponse;
import com.ecommerce.commerceapi.expenses.api.ExpensePaymentStatusRequest;
import com.ecommerce.commerceapi.expenses.api.ExpenseRequest;
import com.ecommerce.commerceapi.expenses.api.ExpenseResponse;
import com.ecommerce.commerceapi.expenses.domain.Expense;
import com.ecommerce.commerceapi.expenses.domain.ExpensePaymentMethod;
import com.ecommerce.commerceapi.expenses.domain.ExpensePaymentStatus;
import com.ecommerce.commerceapi.expenses.repository.ExpenseRepository;
import com.ecommerce.commerceapi.masters.domain.ExpenseCategory;
import com.ecommerce.commerceapi.masters.repository.ExpenseCategoryRepository;
import com.ecommerce.commerceapi.suppliers.domain.Supplier;
import com.ecommerce.commerceapi.suppliers.repository.SupplierRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpenseService {
    private final ExpenseRepository expenses;
    private final ExpenseCategoryRepository categories;
    private final SupplierRepository suppliers;

    public ExpenseService(ExpenseRepository expenses, ExpenseCategoryRepository categories, SupplierRepository suppliers) {
        this.expenses = expenses;
        this.categories = categories;
        this.suppliers = suppliers;
    }

    @Transactional(readOnly = true)
    public ExpensePageResponse list(String search, Long categoryId, Long supplierId, ExpensePaymentStatus paymentStatus,
                                    ExpensePaymentMethod paymentMethod, LocalDate fromDate, LocalDate toDate,
                                    BigDecimal minAmount, BigDecimal maxAmount, int page, int size) {
        var results = expenses.search(search == null ? "" : search, categoryId, supplierId, paymentStatus, paymentMethod, fromDate, toDate, minAmount, maxAmount,
                PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "expenseDate").and(Sort.by(Sort.Direction.DESC, "id"))));
        return new ExpensePageResponse(results.map(this::response).toList(), results.getNumber(), results.getSize(), results.getTotalElements(), results.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ExpenseResponse findById(Long id) {
        return response(expenses.findById(id).orElseThrow(() -> new EntityNotFoundException("Expense not found")));
    }

    @Transactional
    public ExpenseResponse create(ExpenseRequest request) {
        Expense expense = new Expense();
        apply(expense, request, true);
        expense.setExpenseNumber(generateExpenseNumber(request.expenseDate()));
        ensureExpenseNumberAvailable(expense.getExpenseNumber(), null);
        return response(expenses.save(expense));
    }

    @Transactional
    public ExpenseResponse update(Long id, ExpenseRequest request) {
        Expense expense = expenses.findById(id).orElseThrow(() -> new EntityNotFoundException("Expense not found"));
        ExpenseRequest normalized = request;
        if (expense.getPaymentStatus() == ExpensePaymentStatus.PAID && hasFinancialChanges(expense, request)) {
            throw new IllegalArgumentException("Paid expenses cannot change financial details");
        }
        apply(expense, normalized, false);
        return response(expense);
    }

    @Transactional
    public ExpenseResponse updatePaymentStatus(Long id, ExpensePaymentStatusRequest request) {
        Expense expense = expenses.findById(id).orElseThrow(() -> new EntityNotFoundException("Expense not found"));
        expense.setPaymentStatus(request.paymentStatus());
        return response(expense);
    }

    private void apply(Expense expense, ExpenseRequest request, boolean creating) {
        if (request.amount().compareTo(BigDecimal.ZERO) < 0 || request.taxAmount().compareTo(BigDecimal.ZERO) < 0 || request.totalAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Expense amounts cannot be negative");
        }
        BigDecimal expectedTotal = request.amount().add(request.taxAmount());
        if (request.totalAmount().compareTo(expectedTotal) != 0) {
            throw new IllegalArgumentException("Expense total must equal amount plus tax");
        }
        if (request.description().isBlank()) {
            throw new IllegalArgumentException("Expense description is required");
        }
        ExpenseCategory category = categories.findById(request.categoryId()).orElseThrow(() -> new EntityNotFoundException("Expense category not found"));
        Supplier supplier = request.supplierId() == null ? null : suppliers.findById(request.supplierId()).orElseThrow(() -> new EntityNotFoundException("Supplier not found"));
        expense.setExpenseDate(request.expenseDate());
        expense.setCategory(category);
        expense.setSupplier(supplier);
        expense.setDescription(request.description().trim());
        expense.setAmount(request.amount());
        expense.setTaxAmount(request.taxAmount());
        expense.setTotalAmount(expectedTotal);
        expense.setPaymentMethod(request.paymentMethod());
        expense.setPaymentStatus(request.paymentStatus());
        expense.setReferenceNumber(emptyToNull(request.referenceNumber()));
        expense.setRemarks(emptyToNull(request.remarks()));
        if (!creating && expense.getExpenseNumber() != null) {
            ensureExpenseNumberAvailable(expense.getExpenseNumber(), expense.getId());
        }
    }

    private boolean hasFinancialChanges(Expense expense, ExpenseRequest request) {
        return !Objects.equals(expense.getExpenseDate(), request.expenseDate())
                || !Objects.equals(expense.getCategory().getId(), request.categoryId())
                || !Objects.equals(expense.getSupplier() == null ? null : expense.getSupplier().getId(), request.supplierId())
                || expense.getAmount().compareTo(request.amount()) != 0
                || expense.getTaxAmount().compareTo(request.taxAmount()) != 0
                || expense.getTotalAmount().compareTo(request.totalAmount()) != 0
                || expense.getPaymentMethod() != request.paymentMethod();
    }

    private void ensureExpenseNumberAvailable(String expenseNumber, Long currentId) {
        expenses.findByExpenseNumberIgnoreCase(expenseNumber).ifPresent(existing -> {
            if (!Objects.equals(existing.getId(), currentId)) {
                throw new IllegalArgumentException("Expense number already exists");
            }
        });
    }

    private String generateExpenseNumber(LocalDate expenseDate) {
        String year = Year.of(expenseDate.getYear()).toString();
        Integer maxSequence = expenses.findMaxSequenceForYear(year);
        int nextSequence = (maxSequence == null ? 0 : maxSequence) + 1;
        return "EXP-" + year + "-" + String.format("%06d", nextSequence);
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ExpenseResponse response(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getExpenseNumber(),
                expense.getExpenseDate(),
                expense.getCategory().getId(),
                expense.getCategory().getName(),
                expense.getSupplier() == null ? null : expense.getSupplier().getId(),
                expense.getSupplier() == null ? null : expense.getSupplier().getName(),
                expense.getDescription(),
                expense.getAmount(),
                expense.getTaxAmount(),
                expense.getTotalAmount(),
                expense.getPaymentMethod().name(),
                expense.getPaymentStatus().name(),
                expense.getReferenceNumber(),
                expense.getRemarks(),
                expense.getCreatedAt(),
                expense.getUpdatedAt());
    }
}
