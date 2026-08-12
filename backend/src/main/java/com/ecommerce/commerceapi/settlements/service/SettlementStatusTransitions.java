package com.ecommerce.commerceapi.settlements.service;

import com.ecommerce.commerceapi.settlements.domain.SettlementStatus;

final class SettlementStatusTransitions {
    private SettlementStatusTransitions() {}

    static boolean allows(SettlementStatus current, SettlementStatus requested) {
        if (current == requested) {
            return true;
        }
        return switch (current) {
            case PENDING -> requested == SettlementStatus.PARTIALLY_RECEIVED
                    || requested == SettlementStatus.RECEIVED
                    || requested == SettlementStatus.RECONCILED;
            case PARTIALLY_RECEIVED -> requested == SettlementStatus.RECEIVED
                    || requested == SettlementStatus.RECONCILED;
            case RECEIVED -> requested == SettlementStatus.RECONCILED;
            case RECONCILED -> false;
        };
    }
}
