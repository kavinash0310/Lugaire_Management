package com.ecommerce.commerceapi.settlements.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ecommerce.commerceapi.audit.service.AuditLogService;
import com.ecommerce.commerceapi.marketplaces.domain.Marketplace;
import com.ecommerce.commerceapi.marketplaces.repository.MarketplaceRepository;
import com.ecommerce.commerceapi.orders.domain.Order;
import com.ecommerce.commerceapi.orders.domain.PaymentStatus;
import com.ecommerce.commerceapi.orders.repository.OrderRepository;
import com.ecommerce.commerceapi.settlements.api.SettlementRequest;
import com.ecommerce.commerceapi.settlements.domain.Settlement;
import com.ecommerce.commerceapi.settlements.repository.SettlementItemRepository;
import com.ecommerce.commerceapi.settlements.repository.SettlementRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SettlementServiceTest {
    private final SettlementRepository settlements = mock(SettlementRepository.class);
    private final SettlementItemRepository settlementItems = mock(SettlementItemRepository.class);
    private final OrderRepository orders = mock(OrderRepository.class);
    private final MarketplaceRepository marketplaces = mock(MarketplaceRepository.class);
    private final AuditLogService auditLogs = mock(AuditLogService.class);
    private final SettlementService service = new SettlementService(settlements, settlementItems, orders, marketplaces, auditLogs);

    @BeforeEach
    void setUp() {
        when(marketplaces.findById(1L)).thenReturn(Optional.of(marketplace(1L, "MEESHO")));
        when(settlements.save(any())).thenAnswer(invocation -> {
            Settlement settlement = invocation.getArgument(0);
            if (settlement.getId() == null) {
                org.springframework.test.util.ReflectionTestUtils.setField(settlement, "id", 1L);
            }
            return settlement;
        });
        when(orders.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void exactSettlementMatchesAndMarksOrderPaid() {
        when(settlements.findBySettlementIdIgnoreCase("SET-MEE-2026-0001")).thenReturn(Optional.empty());
        when(orders.findById(1L)).thenReturn(Optional.of(order(1L, "MSH-20260805-0001")));
        when(settlementItems.existsByOrder_Id(1L)).thenReturn(false);

        var created = service.create(request("SET-MEE-2026-0001", BigDecimal.valueOf(798), BigDecimal.valueOf(120), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(678)));

        assertEquals("MATCHED", created.reconciliationStatus());
        assertEquals("RECONCILED", created.status());
        assertEquals(BigDecimal.ZERO, created.difference());
        assertEquals(PaymentStatus.PAID.name(), orders.findById(1L).orElseThrow().getPaymentStatus().name());
    }

    @Test
    void duplicateSettlementIdIsRejected() {
        Settlement existing = new Settlement();
        org.springframework.test.util.ReflectionTestUtils.setField(existing, "id", 9L);
        existing.setSettlementId("SET-MEE-2026-0001");
        when(settlements.findBySettlementIdIgnoreCase("SET-MEE-2026-0001")).thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class, () -> service.create(request("SET-MEE-2026-0001", BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN)));
    }

    @Test
    void invalidOrderIsRejected() {
        when(settlements.findBySettlementIdIgnoreCase("SET-MEE-2026-0002")).thenReturn(Optional.empty());
        when(orders.findById(99L)).thenReturn(Optional.empty());

        assertThrows(jakarta.persistence.EntityNotFoundException.class, () -> service.create(requestWithOrder("SET-MEE-2026-0002", 99L)));
    }

    @Test
    void mismatchSettlementKeepsDifferenceVisible() {
        when(settlements.findBySettlementIdIgnoreCase("SET-MEE-2026-0003")).thenReturn(Optional.empty());
        when(orders.findById(1L)).thenReturn(Optional.of(order(1L, "MSH-20260805-0001")));
        when(settlementItems.existsByOrder_Id(1L)).thenReturn(false);

        var created = service.create(request("SET-MEE-2026-0003", BigDecimal.valueOf(798), BigDecimal.valueOf(120), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(650)));

        assertEquals("MISMATCH", created.reconciliationStatus());
        assertEquals(BigDecimal.valueOf(-28), created.difference());
        assertEquals(PaymentStatus.DEDUCTED.name(), orders.findById(1L).orElseThrow().getPaymentStatus().name());
    }

    @Test
    void partialSettlementStaysPartial() {
        when(settlements.findBySettlementIdIgnoreCase("SET-MEE-2026-0004")).thenReturn(Optional.empty());
        when(orders.findById(1L)).thenReturn(Optional.of(order(1L, "MSH-20260805-0001")));
        when(settlementItems.existsByOrder_Id(1L)).thenReturn(false);

        var created = service.create(request("SET-MEE-2026-0004", BigDecimal.valueOf(1000), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(600)));

        assertEquals("PARTIALLY_RECEIVED", created.status());
        assertEquals("MISMATCH", created.reconciliationStatus());
        assertEquals(PaymentStatus.DEDUCTED.name(), orders.findById(1L).orElseThrow().getPaymentStatus().name());
    }

    private SettlementRequest request(String settlementId, BigDecimal gross, BigDecimal fees, BigDecimal shipping, BigDecimal returns, BigDecimal other, BigDecimal received) {
        return new SettlementRequest(settlementId, 1L, LocalDate.now(), LocalDate.now().minusDays(7), LocalDate.now(), gross, fees, shipping, returns, other, received, "Test settlement", List.of(new SettlementRequest.Item(1L, gross, fees, shipping, returns, other, received, "Order item")));
    }

    private SettlementRequest requestWithOrder(String settlementId, Long orderId) {
        return new SettlementRequest(settlementId,1L , LocalDate.now(), LocalDate.now().minusDays(7), LocalDate.now(), BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN, "Test settlement", List.of(new SettlementRequest.Item(orderId, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN, "Order item")));
    }

    private Order order(Long id, String orderId) {
        Order order = new Order();
        org.springframework.test.util.ReflectionTestUtils.setField(order, "id", id);
        order.setOrderId(orderId);
        order.setMarketplace(marketplace(1L, "MEESHO"));
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setOrderDate(LocalDate.now());
        return order;
    }

    private Marketplace marketplace(Long id, String code) {
        Marketplace marketplace = new Marketplace();
        org.springframework.test.util.ReflectionTestUtils.setField(marketplace, "id", id);
        marketplace.setCode(code);
        marketplace.setName(code);
        marketplace.setActive(true);
        return marketplace;
    }
}
