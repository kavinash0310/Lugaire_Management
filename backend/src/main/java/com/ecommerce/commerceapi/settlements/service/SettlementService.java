package com.ecommerce.commerceapi.settlements.service;

import com.ecommerce.commerceapi.orders.domain.Order;
import com.ecommerce.commerceapi.orders.domain.OrderPlatform;
import com.ecommerce.commerceapi.orders.domain.PaymentStatus;
import com.ecommerce.commerceapi.orders.repository.OrderRepository;
import com.ecommerce.commerceapi.settlements.api.*;
import com.ecommerce.commerceapi.settlements.domain.ReconciliationStatus;
import com.ecommerce.commerceapi.settlements.domain.Settlement;
import com.ecommerce.commerceapi.settlements.domain.SettlementItem;
import com.ecommerce.commerceapi.settlements.domain.SettlementStatus;
import com.ecommerce.commerceapi.settlements.repository.SettlementItemRepository;
import com.ecommerce.commerceapi.settlements.repository.SettlementRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettlementService {
    private final SettlementRepository settlements;
    private final SettlementItemRepository settlementItems;
    private final OrderRepository orders;

    public SettlementService(SettlementRepository settlements, SettlementItemRepository settlementItems, OrderRepository orders) {
        this.settlements = settlements;
        this.settlementItems = settlementItems;
        this.orders = orders;
    }

    @Transactional(readOnly = true)
    public SettlementPageResponse list(String search, OrderPlatform platform, SettlementStatus status,
                                       ReconciliationStatus reconciliationStatus, LocalDate fromDate, LocalDate toDate,
                                       int page, int size) {
        var results = settlements.search(search == null ? "" : search, platform, status, reconciliationStatus, fromDate, toDate, PageRequest.of(page, Math.min(size, 100)));
        return new SettlementPageResponse(results.map(this::summary).toList(), results.getNumber(), results.getSize(), results.getTotalElements(), results.getTotalPages());
    }

    @Transactional(readOnly = true)
    public SettlementDetailResponse findById(Long id) {
        return detail(settlements.findById(id).orElseThrow(() -> new EntityNotFoundException("Settlement not found")));
    }

    @Transactional
    public SettlementDetailResponse create(SettlementRequest request) {
        ensureSettlementIdAvailable(request.settlementId(), null);
        Settlement settlement = new Settlement();
        applyHeader(settlement, request);
        applyItems(settlement, request.items(), Set.of());
        recalculate(settlement);
        validateProvidedTotals(request, settlement);
        settlement = settlements.save(settlement);
        applyOrderPaymentStatus(settlement);
        return detail(settlement);
    }

    @Transactional
    public SettlementDetailResponse update(Long id, SettlementRequest request) {
        Settlement settlement = settlements.findById(id).orElseThrow(() -> new EntityNotFoundException("Settlement not found"));
        Set<Long> existingOrderIds = settlement.getItems().stream().map(item -> item.getOrder().getId()).collect(java.util.stream.Collectors.toSet());
        resetOrderPaymentStatus(settlement.getItems());
        settlement.getItems().clear();
        ensureSettlementIdAvailable(request.settlementId(), settlement.getId());
        applyHeader(settlement, request);
        applyItems(settlement, request.items(), existingOrderIds);
        recalculate(settlement);
        validateProvidedTotals(request, settlement);
        settlement = settlements.save(settlement);
        applyOrderPaymentStatus(settlement);
        return detail(settlement);
    }

    @Transactional
    public SettlementDetailResponse updateStatus(Long id, SettlementStatusRequest request) {
        Settlement settlement = settlements.findById(id).orElseThrow(() -> new EntityNotFoundException("Settlement not found"));
        if (!SettlementStatusTransitions.allows(settlement.getStatus(), request.status())) {
            throw new IllegalArgumentException("Invalid settlement status transition");
        }
        validateStatusAgainstAmounts(settlement, request.status());
        settlement.setStatus(request.status());
        settlement = settlements.save(settlement);
        applyOrderPaymentStatus(settlement);
        return detail(settlement);
    }

    @Transactional
    public SettlementDetailResponse reconcile(Long id) {
        Settlement settlement = settlements.findById(id).orElseThrow(() -> new EntityNotFoundException("Settlement not found"));
        recalculate(settlement);
        settlement.setStatus(determineStatus(settlement.getReceivedAmount(), settlement.getNetAmount()));
        if (settlement.getReconciliationStatus() == ReconciliationStatus.MATCHED && settlement.getStatus() == SettlementStatus.RECEIVED) {
            settlement.setStatus(SettlementStatus.RECONCILED);
        }
        settlement = settlements.save(settlement);
        applyOrderPaymentStatus(settlement);
        return detail(settlement);
    }

    private void applyHeader(Settlement settlement, SettlementRequest request) {
        if (request.settlementPeriodEnd().isBefore(request.settlementPeriodStart())) {
            throw new IllegalArgumentException("Settlement period end must be on or after the start date");
        }
        settlement.setSettlementId(request.settlementId().trim());
        settlement.setPlatform(request.platform());
        settlement.setSettlementDate(request.settlementDate());
        settlement.setSettlementPeriodStart(request.settlementPeriodStart());
        settlement.setSettlementPeriodEnd(request.settlementPeriodEnd());
        settlement.setRemarks(request.remarks());
    }

    private void applyItems(Settlement settlement, List<SettlementRequest.Item> requestItems, Set<Long> permittedOrderIds) {
        Set<Long> seenOrderIds = new HashSet<>();
        for (SettlementRequest.Item requestItem : requestItems) {
            if (!seenOrderIds.add(requestItem.orderId())) {
                throw new IllegalArgumentException("Duplicate order selected in settlement items");
            }
            if (settlementItems.existsByOrder_Id(requestItem.orderId()) && !permittedOrderIds.contains(requestItem.orderId())) {
                throw new IllegalArgumentException("This order is already attached to a settlement");
            }
            Order order = orders.findById(requestItem.orderId()).orElseThrow(() -> new EntityNotFoundException("Order not found"));
            SettlementItem item = new SettlementItem();
            item.setSettlement(settlement);
            item.setOrder(order);
            item.setOrderIdSnapshot(order.getOrderId());
            item.setOrderDateSnapshot(order.getOrderDate());
            item.setGrossOrderAmount(requestItem.grossOrderAmount());
            item.setMarketplaceFee(requestItem.marketplaceFee());
            item.setShippingCharge(requestItem.shippingCharge());
            item.setReturnCharge(requestItem.returnCharge());
            item.setOtherCharge(requestItem.otherCharge());
            item.setSettledAmount(requestItem.settledAmount());
            item.setRemarks(requestItem.remarks());
            settlement.getItems().add(item);
        }
    }

    private void recalculate(Settlement settlement) {
        BigDecimal grossAmount = BigDecimal.ZERO;
        BigDecimal marketplaceFees = BigDecimal.ZERO;
        BigDecimal shippingCharges = BigDecimal.ZERO;
        BigDecimal returnCharges = BigDecimal.ZERO;
        BigDecimal otherCharges = BigDecimal.ZERO;
        BigDecimal receivedAmount = BigDecimal.ZERO;

        for (SettlementItem item : settlement.getItems()) {
            BigDecimal expected = item.getGrossOrderAmount()
                    .subtract(item.getMarketplaceFee())
                    .subtract(item.getShippingCharge())
                    .subtract(item.getReturnCharge())
                    .subtract(item.getOtherCharge());
            if (expected.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Expected net settlement cannot be negative");
            }
            BigDecimal difference = item.getSettledAmount().subtract(expected);
            item.setExpectedNetSettlement(expected);
            item.setDifference(difference);
            item.setReconciliationStatus(item.getSettledAmount().compareTo(BigDecimal.ZERO) == 0
                    ? ReconciliationStatus.UNMATCHED
                    : difference.compareTo(BigDecimal.ZERO) == 0 ? ReconciliationStatus.MATCHED : ReconciliationStatus.MISMATCH);

            grossAmount = grossAmount.add(item.getGrossOrderAmount());
            marketplaceFees = marketplaceFees.add(item.getMarketplaceFee());
            shippingCharges = shippingCharges.add(item.getShippingCharge());
            returnCharges = returnCharges.add(item.getReturnCharge());
            otherCharges = otherCharges.add(item.getOtherCharge());
            receivedAmount = receivedAmount.add(item.getSettledAmount());
        }

        settlement.setGrossAmount(grossAmount);
        settlement.setMarketplaceFees(marketplaceFees);
        settlement.setShippingCharges(shippingCharges);
        settlement.setReturnCharges(returnCharges);
        settlement.setOtherCharges(otherCharges);
        settlement.setReceivedAmount(receivedAmount);
        settlement.setNetAmount(grossAmount.subtract(marketplaceFees).subtract(shippingCharges).subtract(returnCharges).subtract(otherCharges));
        if (settlement.getNetAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Settlement net amount cannot be negative");
        }
        settlement.setReconciliationStatus(determineReconciliationStatus(settlement));
        if (settlement.getReconciliationStatus() == ReconciliationStatus.MATCHED
                && settlement.getReceivedAmount().compareTo(settlement.getNetAmount()) == 0) {
            settlement.setStatus(SettlementStatus.RECONCILED);
        } else if (settlement.getReceivedAmount().compareTo(BigDecimal.ZERO) == 0) {
            settlement.setStatus(SettlementStatus.PENDING);
        } else if (settlement.getReceivedAmount().compareTo(settlement.getNetAmount()) < 0) {
            settlement.setStatus(SettlementStatus.PARTIALLY_RECEIVED);
        } else {
            settlement.setStatus(SettlementStatus.RECEIVED);
        }
    }

    private void validateProvidedTotals(SettlementRequest request, Settlement settlement) {
        if (request.grossAmount().compareTo(settlement.getGrossAmount()) != 0
                || request.marketplaceFees().compareTo(settlement.getMarketplaceFees()) != 0
                || request.shippingCharges().compareTo(settlement.getShippingCharges()) != 0
                || request.returnCharges().compareTo(settlement.getReturnCharges()) != 0
                || request.otherCharges().compareTo(settlement.getOtherCharges()) != 0
                || request.receivedAmount().compareTo(settlement.getReceivedAmount()) != 0) {
            throw new IllegalArgumentException("Settlement totals must match the settlement items");
        }
    }

    private ReconciliationStatus determineReconciliationStatus(Settlement settlement) {
        if (settlement.getReceivedAmount().compareTo(BigDecimal.ZERO) == 0) {
            return ReconciliationStatus.UNMATCHED;
        }
        boolean allMatched = settlement.getItems().stream().allMatch(item -> item.getReconciliationStatus() == ReconciliationStatus.MATCHED);
        return settlement.getReceivedAmount().compareTo(settlement.getNetAmount()) == 0 && allMatched
                ? ReconciliationStatus.MATCHED
                : ReconciliationStatus.MISMATCH;
    }

    private SettlementStatus determineStatus(BigDecimal receivedAmount, BigDecimal netAmount) {
        if (receivedAmount.compareTo(BigDecimal.ZERO) == 0) {
            return SettlementStatus.PENDING;
        }
        if (receivedAmount.compareTo(netAmount) < 0) {
            return SettlementStatus.PARTIALLY_RECEIVED;
        }
        return SettlementStatus.RECEIVED;
    }

    private void validateStatusAgainstAmounts(Settlement settlement, SettlementStatus requestedStatus) {
        if (requestedStatus == SettlementStatus.PENDING && settlement.getReceivedAmount().compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException("Received settlements cannot be marked pending");
        }
        if (requestedStatus == SettlementStatus.PARTIALLY_RECEIVED &&
                (settlement.getReceivedAmount().compareTo(BigDecimal.ZERO) <= 0 || settlement.getReceivedAmount().compareTo(settlement.getNetAmount()) >= 0)) {
            throw new IllegalArgumentException("Partial settlement requires a positive received amount below the net amount");
        }
        if (requestedStatus == SettlementStatus.RECEIVED && settlement.getReceivedAmount().compareTo(settlement.getNetAmount()) < 0) {
            throw new IllegalArgumentException("Received settlement requires the received amount to meet or exceed the net amount");
        }
        if (requestedStatus == SettlementStatus.RECONCILED && settlement.getReconciliationStatus() != ReconciliationStatus.MATCHED) {
            throw new IllegalArgumentException("Only matched settlements can be reconciled");
        }
    }

    private void ensureSettlementIdAvailable(String settlementId, Long currentId) {
        settlements.findBySettlementIdIgnoreCase(settlementId.trim()).ifPresent(existing -> {
            if (!Objects.equals(existing.getId(), currentId)) {
                throw new IllegalArgumentException("Settlement ID already exists");
            }
        });
    }

    private void resetOrderPaymentStatus(List<SettlementItem> items) {
        for (SettlementItem item : items) {
            Order order = item.getOrder();
            order.setPaymentStatus(PaymentStatus.PENDING);
            orders.save(order);
        }
    }

    private void applyOrderPaymentStatus(Settlement settlement) {
        for (SettlementItem item : settlement.getItems()) {
            Order order = item.getOrder();
            PaymentStatus paymentStatus = item.getReconciliationStatus() == ReconciliationStatus.MATCHED
                    ? PaymentStatus.PAID
                    : (item.getSettledAmount().compareTo(BigDecimal.ZERO) > 0 ? PaymentStatus.DEDUCTED : PaymentStatus.PENDING);
            order.setPaymentStatus(paymentStatus);
            orders.save(order);
        }
    }

    private SettlementResponse summary(Settlement settlement) {
        return new SettlementResponse(settlement.getId(), settlement.getSettlementId(), settlement.getPlatform().name(), settlement.getSettlementDate(),
                settlement.getSettlementPeriodStart(), settlement.getSettlementPeriodEnd(), settlement.getGrossAmount(), settlement.getMarketplaceFees(),
                settlement.getShippingCharges(), settlement.getReturnCharges(), settlement.getOtherCharges(), settlement.getNetAmount(),
                settlement.getReceivedAmount(), settlement.getReceivedAmount().subtract(settlement.getNetAmount()), settlement.getStatus().name(),
                settlement.getReconciliationStatus().name(), settlement.getRemarks());
    }

    private SettlementDetailResponse detail(Settlement settlement) {
        return new SettlementDetailResponse(settlement.getId(), settlement.getSettlementId(), settlement.getPlatform().name(), settlement.getSettlementDate(),
                settlement.getSettlementPeriodStart(), settlement.getSettlementPeriodEnd(), settlement.getGrossAmount(), settlement.getMarketplaceFees(),
                settlement.getShippingCharges(), settlement.getReturnCharges(), settlement.getOtherCharges(), settlement.getNetAmount(),
                settlement.getReceivedAmount(), settlement.getReceivedAmount().subtract(settlement.getNetAmount()), settlement.getStatus().name(),
                settlement.getReconciliationStatus().name(), settlement.getRemarks(), settlement.getItems().stream().map(this::itemResponse).toList());
    }

    private SettlementItemResponse itemResponse(SettlementItem item) {
        return new SettlementItemResponse(item.getId(), item.getOrder().getId(), item.getOrderIdSnapshot(), item.getOrderDateSnapshot(),
                item.getGrossOrderAmount(), item.getMarketplaceFee(), item.getShippingCharge(), item.getReturnCharge(), item.getOtherCharge(),
                item.getExpectedNetSettlement(), item.getSettledAmount(), item.getDifference(), item.getReconciliationStatus().name(), item.getRemarks());
    }
}
