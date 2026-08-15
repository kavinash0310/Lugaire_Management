package com.ecommerce.commerceapi.orders.service;

import com.ecommerce.commerceapi.marketplaces.domain.Marketplace;
import com.ecommerce.commerceapi.marketplaces.repository.MarketplaceRepository;
import com.ecommerce.commerceapi.orders.domain.Order;
import com.ecommerce.commerceapi.orders.api.OrderResponse;
import com.ecommerce.commerceapi.orders.api.OrderRequest;
import com.ecommerce.commerceapi.orders.api.OrderUpdateRequest;
import java.math.BigDecimal;
import com.ecommerce.commerceapi.orders.domain.OrderItem;
import com.ecommerce.commerceapi.products.repository.ProductVariantRepository;
import com.ecommerce.commerceapi.inventory.api.InventoryTransactionRequest;
import com.ecommerce.commerceapi.inventory.domain.InventoryTransactionType;
import com.ecommerce.commerceapi.inventory.service.InventoryService;
import com.ecommerce.commerceapi.audit.service.AuditLogService;
import com.ecommerce.commerceapi.orders.domain.OrderStatus;
import com.ecommerce.commerceapi.orders.domain.PaymentStatus;
import com.ecommerce.commerceapi.returns.repository.ReturnRecordRepository;
import java.time.LocalDate;
import com.ecommerce.commerceapi.orders.repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final OrderRepository orders;
    private final MarketplaceRepository marketplaces;
    private final ProductVariantRepository variants;
    private final InventoryService inventory;
    private final ReturnRecordRepository returns;
    private final AuditLogService auditLogs;

    public OrderService(OrderRepository orders, MarketplaceRepository marketplaces, ProductVariantRepository variants, InventoryService inventory, ReturnRecordRepository returns, AuditLogService auditLogs) { this.orders = orders; this.marketplaces = marketplaces; this.variants = variants; this.inventory = inventory; this.returns = returns; this.auditLogs = auditLogs; }

    @Transactional(readOnly = true)
    public OrderResponse findById(Long id) {
        Order order = orders.findById(id).orElseThrow(() -> new EntityNotFoundException("Order not found"));
        return new OrderResponse(order.getId(), order.getOrderId(), order.getMarketplace().getCode(), order.getOrderDate(), order.getOrderStatus().name(), order.getPaymentStatus().name(), order.isInventoryDeducted());
    }

    @Transactional(readOnly = true)
    public com.ecommerce.commerceapi.orders.api.OrderDetailResponse findDetailById(Long id) {
        Order order = orders.findById(id).orElseThrow(() -> new EntityNotFoundException("Order not found"));
        var items = order.getItems().stream().map(item -> {
            int returnedQuantity = countReturnedQuantity(item.getId());
            return new com.ecommerce.commerceapi.orders.api.OrderItemResponse(
                    item.getId(),
                    item.getVariant().getId(),
                    item.getVariant().getProduct().getProductName(),
                    item.getVariant().getColor().getName(),
                    item.getVariant().getSize().getName(),
                    item.getSkuSnapshot(),
                    item.getQuantity(),
                    returnedQuantity,
                    item.getQuantity() - returnedQuantity,
                    item.getSellingPrice(),
                    item.getLineTotal());
        }).toList();
        return new com.ecommerce.commerceapi.orders.api.OrderDetailResponse(order.getId(), order.getOrderId(), order.getMarketplace().getCode(), order.getOrderDate(), order.getOrderStatus().name(), order.getPaymentStatus().name(), order.getCustomerName(), order.getCustomerPhone(), order.getShippingAddress(), order.getCity(), order.getState(), order.getPincode(), order.getTotalOrderValue(), order.getCommission(), order.getShippingCharge(), order.getOtherCharges(), order.getNetAmount(), items);
    }

    private int countReturnedQuantity(Long orderItemId) {
        return returns.returnedQuantity(orderItemId);
    }

    @Transactional(readOnly = true)
    public com.ecommerce.commerceapi.orders.api.OrderPageResponse list(String search, Long marketplaceId, OrderStatus status, int page, int size) {
        var results = orders.search(search == null ? "" : search, marketplaceId, status, org.springframework.data.domain.PageRequest.of(page, size));
        return new com.ecommerce.commerceapi.orders.api.OrderPageResponse(results.map(order -> new OrderResponse(order.getId(), order.getOrderId(), order.getMarketplace().getCode(), order.getOrderDate(), order.getOrderStatus().name(), order.getPaymentStatus().name(), order.isInventoryDeducted())).toList(), results.getNumber(), results.getSize(), results.getTotalElements(), results.getTotalPages());
    }

    @Transactional
    public OrderResponse create(OrderRequest request) {
        Marketplace marketplace = marketplaces.findById(request.marketplaceId())
                .orElseThrow(() -> new EntityNotFoundException("Marketplace not found"));
        if (!marketplace.isActive()) {
            throw new IllegalArgumentException("Marketplace is inactive");
        }
        if (orders.findByMarketplaceIdAndOrderId(request.marketplaceId(), request.orderId().trim()).isPresent()) {
            throw new IllegalArgumentException("An order with this marketplace and order ID already exists");
        }
        Order order = new Order();
        order.setOrderId(request.orderId().trim());
        order.setMarketplace(marketplace);
        order.setOrderDate(request.orderDate());
        order.setCustomerName(request.customerName());
        order.setCustomerPhone(request.customerPhone());
        order.setShippingAddress(request.shippingAddress());
        order.setCity(request.city());
        order.setState(request.state());
        order.setPincode(request.pincode());
        order.setCommission(request.commission());
        order.setShippingCharge(request.shippingCharge());
        order.setOtherCharges(request.otherCharges());
        BigDecimal total = request.items().stream().map(item -> item.sellingPrice().multiply(BigDecimal.valueOf(item.quantity()))).reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalOrderValue(total);
        order.setNetAmount(total.subtract(request.commission()).subtract(request.shippingCharge()).subtract(request.otherCharges()));
        request.items().forEach(requestItem -> { var variant = variants.findById(requestItem.variantId()).orElseThrow(() -> new EntityNotFoundException("Variant not found")); if (!variant.isActive()) throw new IllegalArgumentException("Variant is inactive"); OrderItem item = new OrderItem(); item.setOrder(order); item.setVariant(variant); item.setSkuSnapshot(variant.getSku()); item.setProductNameSnapshot(variant.getProduct().getProductName()); item.setQuantity(requestItem.quantity()); item.setSellingPrice(requestItem.sellingPrice()); item.setLineTotal(requestItem.sellingPrice().multiply(BigDecimal.valueOf(requestItem.quantity()))); order.getItems().add(item); });
        Order saved = orders.save(order);
        auditLogs.log(
                "CREATE",
                "ORDER",
                "Order",
                String.valueOf(saved.getId()),
                "Order created",
                null,
                orderState(saved));
        return findById(saved.getId());
    }

    @Transactional
    public OrderResponse update(Long id, OrderUpdateRequest request) {
        Order order = orders.findById(id).orElseThrow(() -> new EntityNotFoundException("Order not found"));
        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Cancelled orders cannot be edited");
        }
        Map<String, Object> oldValue = orderState(order);

        if (order.isInventoryDeducted()) {
            reverseSale(order, "ORDER_EDIT_REVERSAL", "Reversing order edit " + order.getOrderId());
            order.setInventoryDeducted(false);
        }

        order.setOrderDate(request.orderDate());
        order.setCustomerName(request.customerName());
        order.setCustomerPhone(request.customerPhone());
        order.setShippingAddress(request.shippingAddress());
        order.setCity(request.city());
        order.setState(request.state());
        order.setPincode(request.pincode());
        order.setCommission(request.commission());
        order.setShippingCharge(request.shippingCharge());
        order.setOtherCharges(request.otherCharges());
        order.getItems().clear();

        BigDecimal total = BigDecimal.ZERO;
        for (OrderUpdateRequest.Item requestItem : request.items()) {
            var variant = variants.findById(requestItem.variantId()).orElseThrow(() -> new EntityNotFoundException("Variant not found"));
            if (!variant.isActive()) {
                throw new IllegalArgumentException("Variant is inactive");
            }
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setVariant(variant);
            item.setSkuSnapshot(variant.getSku());
            item.setProductNameSnapshot(variant.getProduct().getProductName());
            item.setQuantity(requestItem.quantity());
            item.setSellingPrice(requestItem.sellingPrice());
            item.setLineTotal(requestItem.sellingPrice().multiply(BigDecimal.valueOf(requestItem.quantity())));
            total = total.add(item.getLineTotal());
            order.getItems().add(item);
        }
        order.setTotalOrderValue(total);
        order.setNetAmount(total.subtract(request.commission()).subtract(request.shippingCharge()).subtract(request.otherCharges()));

        if (order.getOrderStatus() == OrderStatus.PROCESSING) {
            recordSale(order);
            order.setInventoryDeducted(true);
        }
        auditLogs.log(
                "UPDATE",
                "ORDER",
                "Order",
                String.valueOf(order.getId()),
                "Order updated",
                oldValue,
                orderState(order));
        return findById(id);
    }

    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatus status) {
        Order order = orders.findById(id).orElseThrow(() -> new EntityNotFoundException("Order not found"));
        if (!OrderStatusTransitions.allows(order.getOrderStatus(), status)) {
            throw new IllegalArgumentException("Invalid order status transition");
        }
        OrderStatus previous = order.getOrderStatus();
        boolean inventoryDeductedBefore = order.isInventoryDeducted();
        if (status == OrderStatus.PROCESSING && !order.isInventoryDeducted()) {
            recordSale(order);
            order.setInventoryDeducted(true);
        }
        if (status == OrderStatus.CANCELLED && order.isInventoryDeducted()) {
            reverseSale(order, "ORDER_CANCELLATION", "Cancelled order " + order.getOrderId());
            order.setInventoryDeducted(false);
        }
        order.setOrderStatus(status);
        auditLogs.log(
                "STATUS_CHANGE",
                "ORDER",
                "Order",
                String.valueOf(order.getId()),
                "Order status changed",
                Map.of("orderStatus", previous.name(), "inventoryDeducted", String.valueOf(inventoryDeductedBefore)),
                Map.of("orderStatus", order.getOrderStatus().name(), "inventoryDeducted", String.valueOf(order.isInventoryDeducted())));
        return findById(id);
    }

    @Transactional
    public OrderResponse updatePaymentStatus(Long id, PaymentStatus status) {
        Order order = orders.findById(id).orElseThrow(() -> new EntityNotFoundException("Order not found"));
        PaymentStatus previous = order.getPaymentStatus();
        order.setPaymentStatus(status);
        auditLogs.log(
                "STATUS_CHANGE",
                "ORDER",
                "Order",
                String.valueOf(order.getId()),
                "Order payment status changed",
                Map.of("paymentStatus", previous.name()),
                Map.of("paymentStatus", order.getPaymentStatus().name()));
        return findById(id);
    }

    private void recordSale(Order order) {
        order.getItems().forEach(item -> inventory.record(new InventoryTransactionRequest(item.getVariant().getId(), InventoryTransactionType.SALE, item.getQuantity(), null, "ORDER", order.getId().toString(), "Order " + order.getOrderId(), null, LocalDate.now())));
    }

    private void reverseSale(Order order, String referenceType, String remarks) {
        order.getItems().forEach(item -> inventory.record(new InventoryTransactionRequest(item.getVariant().getId(), InventoryTransactionType.ADJUSTMENT_IN, item.getQuantity(), null, referenceType, order.getId().toString(), remarks, null, LocalDate.now())));
    }

    private Map<String, Object> orderState(Order order) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("orderId", order.getOrderId());
        state.put("marketplace", order.getMarketplace() == null ? null : order.getMarketplace().getCode());
        state.put("orderDate", order.getOrderDate());
        state.put("customerName", order.getCustomerName());
        state.put("status", order.getOrderStatus() == null ? null : order.getOrderStatus().name());
        state.put("paymentStatus", order.getPaymentStatus() == null ? null : order.getPaymentStatus().name());
        state.put("inventoryDeducted", order.isInventoryDeducted());
        state.put("totalOrderValue", order.getTotalOrderValue());
        state.put("commission", order.getCommission());
        state.put("shippingCharge", order.getShippingCharge());
        state.put("otherCharges", order.getOtherCharges());
        state.put("netAmount", order.getNetAmount());
        state.put("itemCount", order.getItems() == null ? 0 : order.getItems().size());
        return state;
    }
}
