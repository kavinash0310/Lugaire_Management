package com.ecommerce.commerceapi.orders.service;

import com.ecommerce.commerceapi.orders.domain.OrderStatus;
import java.util.Set;

final class OrderStatusTransitions {
    private OrderStatusTransitions() {}

    static boolean allows(OrderStatus from, OrderStatus to) {
        if (from == to) return true;
        return switch (from) {
            case PENDING -> Set.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED).contains(to);
            case PROCESSING -> Set.of(OrderStatus.ON_THE_WAY, OrderStatus.CANCELLED).contains(to);
            case ON_THE_WAY -> Set.of(OrderStatus.DELIVERED, OrderStatus.RTO).contains(to);
            case DELIVERED -> to == OrderStatus.RETURNED;
            default -> false;
        };
    }
}
