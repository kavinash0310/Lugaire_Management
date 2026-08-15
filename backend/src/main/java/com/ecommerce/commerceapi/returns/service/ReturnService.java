package com.ecommerce.commerceapi.returns.service;

import com.ecommerce.commerceapi.inventory.api.InventoryTransactionRequest;
import com.ecommerce.commerceapi.inventory.domain.InventoryTransactionType;
import com.ecommerce.commerceapi.inventory.service.InventoryService;
import com.ecommerce.commerceapi.audit.service.AuditLogService;
import com.ecommerce.commerceapi.orders.domain.OrderItem;
import com.ecommerce.commerceapi.orders.repository.OrderItemRepository;
import com.ecommerce.commerceapi.orders.domain.OrderStatus;
import com.ecommerce.commerceapi.returns.api.ReturnCreateRequest;
import com.ecommerce.commerceapi.returns.api.ReturnResponse;
import com.ecommerce.commerceapi.returns.api.ReturnStatusRequest;
import com.ecommerce.commerceapi.returns.domain.ReturnRecord;
import com.ecommerce.commerceapi.returns.domain.ReturnStatus;
import com.ecommerce.commerceapi.returns.domain.ReturnType;
import com.ecommerce.commerceapi.returns.repository.ReturnRecordRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReturnService {
    private final ReturnRecordRepository records;
    private final OrderItemRepository orderItems;
    private final InventoryService inventory;
    private final AuditLogService auditLogs;

    public ReturnService(ReturnRecordRepository records, OrderItemRepository orderItems, InventoryService inventory, AuditLogService auditLogs) {
        this.records = records;
        this.orderItems = orderItems;
        this.inventory = inventory;
        this.auditLogs = auditLogs;
    }

    @Transactional
    public ReturnResponse create(ReturnCreateRequest request) {
        OrderItem orderItem = orderItems.findById(request.orderItemId())
                .orElseThrow(() -> new EntityNotFoundException("Order item not found"));
        if (request.type() == ReturnType.RETURNED && orderItem.getOrder().getOrderStatus() != OrderStatus.DELIVERED) {
            throw new IllegalArgumentException("A returned item must belong to a delivered order");
        }
        if (request.type() == ReturnType.RTO && orderItem.getOrder().getOrderStatus() != OrderStatus.ON_THE_WAY) {
            throw new IllegalArgumentException("An RTO item must belong to an order on the way");
        }
        if (records.returnedQuantity(orderItem.getId()) + request.quantity() > orderItem.getQuantity()) {
            throw new IllegalArgumentException("Return quantity exceeds the original ordered quantity");
        }
        BigDecimal productLoss = orderItem.getVariant().getCostPrice().multiply(BigDecimal.valueOf(request.quantity()));
        ReturnRecord record = new ReturnRecord();
        record.setOrder(orderItem.getOrder());
        record.setOrderItem(orderItem);
        record.setVariant(orderItem.getVariant());
        record.setType(request.type());
        record.setReason(request.reason().trim());
        record.setQuantity(request.quantity());
        record.setProductCost(productLoss);
        record.setShippingLoss(request.shippingLoss());
        record.setOtherLoss(request.otherLoss());
        record.setTotalLoss(productLoss.add(request.shippingLoss()).add(request.otherLoss()));
        record.setResellable(request.resellable());
        record.setReturnDate(request.returnDate());
        record.setRemarks(request.remarks());
        ReturnRecord saved = records.save(record);
        auditLogs.log(
                "CREATE",
                "RETURN",
                "ReturnRecord",
                String.valueOf(saved.getId()),
                "Return record created",
                null,
                returnState(saved));
        return response(saved);
    }

    @Transactional
    public ReturnResponse updateStatus(Long id, ReturnStatusRequest request) {
        ReturnRecord record = records.findById(id).orElseThrow(() -> new EntityNotFoundException("Return record not found"));
        if (!allows(record.getStatus(), request.status())) {
            throw new IllegalArgumentException("Invalid return status transition");
        }
        if (request.status() == ReturnStatus.RECEIVED && record.isResellable() && !record.isInventoryRestored()) {
            inventory.record(new InventoryTransactionRequest(record.getVariant().getId(), InventoryTransactionType.RETURN,
                    record.getQuantity(), record.getVariant().getCostPrice(), "RETURN_RECORD", record.getId().toString(),
                    "Resellable " + record.getType() + " for order " + record.getOrder().getOrderId(), null, LocalDate.now()));
            record.setInventoryRestored(true);
        }
        if (request.status() == ReturnStatus.RECEIVED) {
            record.setReceivedDate(request.receivedDate() == null ? LocalDate.now() : request.receivedDate());
        }
        ReturnStatus previous = record.getStatus();
        record.setStatus(request.status());
        auditLogs.log(
                "STATUS_CHANGE",
                "RETURN",
                "ReturnRecord",
                String.valueOf(record.getId()),
                "Return status changed",
                Map.of("status", previous.name(), "inventoryRestored", String.valueOf(record.isInventoryRestored())),
                Map.of("status", record.getStatus().name(), "inventoryRestored", String.valueOf(record.isInventoryRestored())));
        return response(record);
    }

    @Transactional(readOnly = true)
    public ReturnResponse findById(Long id) {
        return response(records.findById(id).orElseThrow(() -> new EntityNotFoundException("Return record not found")));
    }

    @Transactional(readOnly = true)
    public Page<ReturnResponse> list(String search, Long marketplaceId, ReturnType type, ReturnStatus status,
                                     LocalDate fromDate, LocalDate toDate, int page, int size) {
        return records.search(search == null ? "" : search, marketplaceId, type, status, fromDate, toDate,
                        PageRequest.of(page, Math.min(size, 100)))
                .map(this::response);
    }

    private boolean allows(ReturnStatus current, ReturnStatus next) {
        return current == next || current == ReturnStatus.INITIATED && next == ReturnStatus.RECEIVED
                || current == ReturnStatus.RECEIVED && next == ReturnStatus.INSPECTED
                || current == ReturnStatus.INSPECTED && next == ReturnStatus.COMPLETED;
    }

    private ReturnResponse response(ReturnRecord record) {
        return new ReturnResponse(
                record.getId(),
                record.getOrder().getOrderId(),
                record.getOrder().getMarketplace().getCode(),
                record.getOrder().getOrderDate(),
                record.getOrder().getOrderStatus().name(),
                record.getOrderItem().getId(),
                record.getVariant().getProduct().getProductName(),
                record.getVariant().getColor().getName(),
                record.getVariant().getSize().getName() + (record.getVariant().getSize().getSizeType() == null ? "" : " " + record.getVariant().getSize().getSizeType()),
                record.getVariant().getSku(),
                record.getType().name(),
                record.getReason(),
                record.getQuantity(),
                record.getProductCost(),
                record.getShippingLoss(),
                record.getOtherLoss(),
                record.getTotalLoss(),
                record.isResellable(),
                record.getStatus().name(),
                record.getReturnDate(),
                record.getReceivedDate(),
                record.getRemarks(),
                record.isInventoryRestored());
    }

    private Map<String, Object> returnState(ReturnRecord record) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("orderId", record.getOrder() == null ? null : record.getOrder().getOrderId());
        state.put("orderItemId", record.getOrderItem() == null ? null : record.getOrderItem().getId());
        state.put("variantSku", record.getVariant() == null ? null : record.getVariant().getSku());
        state.put("type", record.getType() == null ? null : record.getType().name());
        state.put("reason", record.getReason());
        state.put("quantity", record.getQuantity());
        state.put("status", record.getStatus() == null ? null : record.getStatus().name());
        state.put("resellable", record.isResellable());
        state.put("inventoryRestored", record.isInventoryRestored());
        return state;
    }
}
