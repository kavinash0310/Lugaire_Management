package com.ecommerce.commerceapi.dashboard.service;

import com.ecommerce.commerceapi.dashboard.api.DashboardBacklogResponse;
import com.ecommerce.commerceapi.dashboard.api.DashboardMarketplaceResponse;
import com.ecommerce.commerceapi.dashboard.api.DashboardStatusResponse;
import com.ecommerce.commerceapi.dashboard.api.DashboardSummaryResponse;
import com.ecommerce.commerceapi.dashboard.api.DashboardTrendPointResponse;
import com.ecommerce.commerceapi.expenses.domain.Expense;
import com.ecommerce.commerceapi.expenses.repository.ExpenseRepository;
import com.ecommerce.commerceapi.inventory.api.InventoryPageResponse;
import com.ecommerce.commerceapi.inventory.api.InventorySort;
import com.ecommerce.commerceapi.inventory.service.InventoryService;
import com.ecommerce.commerceapi.orders.domain.Order;
import com.ecommerce.commerceapi.orders.domain.OrderStatus;
import com.ecommerce.commerceapi.orders.domain.PaymentStatus;
import com.ecommerce.commerceapi.orders.repository.OrderRepository;
import com.ecommerce.commerceapi.reports.api.CostMetricsResponse;
import com.ecommerce.commerceapi.reports.api.ExpenseBreakdownResponse;
import com.ecommerce.commerceapi.reports.api.ExpenseSummaryResponse;
import com.ecommerce.commerceapi.reports.api.ProfitMetricsResponse;
import com.ecommerce.commerceapi.reports.api.RevenueMetricsResponse;
import com.ecommerce.commerceapi.reports.domain.ReportPeriod;
import com.ecommerce.commerceapi.settlements.domain.Settlement;
import com.ecommerce.commerceapi.settlements.domain.SettlementStatus;
import com.ecommerce.commerceapi.settlements.repository.SettlementRepository;
import com.ecommerce.commerceapi.returns.domain.ReturnRecord;
import com.ecommerce.commerceapi.returns.domain.ReturnType;
import com.ecommerce.commerceapi.returns.repository.ReturnRecordRepository;
import com.ecommerce.commerceapi.settings.service.SystemSettingsService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
    private final OrderRepository orders;
    private final SettlementRepository settlements;
    private final ExpenseRepository expenses;
    private final ReturnRecordRepository returns;
    private final InventoryService inventory;
    private final SystemSettingsService settings;

    public DashboardService(
            OrderRepository orders,
            SettlementRepository settlements,
            ExpenseRepository expenses,
            ReturnRecordRepository returns,
            InventoryService inventory,
            SystemSettingsService settings) {
        this.orders = orders;
        this.settlements = settlements;
        this.expenses = expenses;
        this.returns = returns;
        this.inventory = inventory;
        this.settings = settings;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary(ReportPeriod period, LocalDate fromDate, LocalDate toDate) {
        ReportRange range = resolveRange(period, fromDate, toDate);

        List<Order> orderList = orders.findAllByOrderDateBetween(range.fromDate(), range.toDate());
        List<Settlement> settlementList = settlements.findAllBySettlementDateBetween(range.fromDate(), range.toDate());
        List<Expense> expenseList = expenses.findAllByExpenseDateBetween(range.fromDate(), range.toDate());
        List<ReturnRecord> returnList = returns.findAllByReturnDateBetween(range.fromDate(), range.toDate());
        InventoryPageResponse lowStock = inventory.lowStock("", InventorySort.SKU_ASC, 0, 10);

        Map<LocalDate, BigDecimal> salesTrend = new TreeMap<>();
        Map<LocalDate, BigDecimal> profitTrend = new TreeMap<>();

        for (LocalDate date : eachDate(range.fromDate(), range.toDate())) {
            salesTrend.put(date, BigDecimal.ZERO);
            profitTrend.put(date, BigDecimal.ZERO);
        }

        Map<LocalDate, BigDecimal> settlementCostByDate = new TreeMap<>();
        Map<LocalDate, BigDecimal> returnLossByDate = new TreeMap<>();
        Map<LocalDate, BigDecimal> expenseCostByDate = new TreeMap<>();

        for (Order order : orderList) {
            if (isRecognizedSale(order)) {
                salesTrend.computeIfPresent(order.getOrderDate(), (date, amount) -> amount.add(safe(order.getTotalOrderValue())));
            }
        }

        for (Settlement settlement : settlementList) {
            BigDecimal charges = safe(settlement.getMarketplaceFees())
                    .add(safe(settlement.getShippingCharges()))
                    .add(safe(settlement.getReturnCharges()))
                    .add(safe(settlement.getOtherCharges()));
            settlementCostByDate.merge(settlement.getSettlementDate(), charges, BigDecimal::add);
        }

        for (ReturnRecord record : returnList) {
            returnLossByDate.merge(record.getReturnDate(), safe(record.getTotalLoss()), BigDecimal::add);
        }

        for (Expense expense : expenseList) {
            expenseCostByDate.merge(expense.getExpenseDate(), safe(expense.getTotalAmount()), BigDecimal::add);
        }

        RevenueMetricsResponse revenue = revenue(orderList);
        CostMetricsResponse costs = costs(orderList, settlementList, returnList);
        ExpenseSummaryResponse expenseSummary = expenseSummary(expenseList);
        ProfitMetricsResponse profitSummary = profit(revenue, costs, expenseSummary);

        for (LocalDate date : salesTrend.keySet()) {
            BigDecimal profit = salesTrend.get(date)
                    .subtract(settlementCostByDate.getOrDefault(date, BigDecimal.ZERO))
                    .subtract(returnLossByDate.getOrDefault(date, BigDecimal.ZERO))
                    .subtract(expenseCostByDate.getOrDefault(date, BigDecimal.ZERO));
            profitTrend.put(date, profit);
        }

        long pendingPayments = orderList.stream()
                .filter(order -> order.getPaymentStatus() != PaymentStatus.PAID)
                .count();
        long pendingSettlements = settlementList.stream()
                .filter(settlement -> settlement.getStatus() != SettlementStatus.RECONCILED)
                .count();

        return new DashboardSummaryResponse(
                settings.businessName(),
                settings.currencySymbol(),
                range.period().name(),
                range.fromDate(),
                range.toDate(),
                revenue,
                costs,
                expenseSummary,
                profitSummary,
                new DashboardBacklogResponse(pendingPayments, pendingSettlements),
                returnsSummary(revenue),
                salesTrend.entrySet().stream().map(entry -> new DashboardTrendPointResponse(entry.getKey(), entry.getValue(), 0L)).toList(),
                profitTrend.entrySet().stream().map(entry -> new DashboardTrendPointResponse(entry.getKey(), entry.getValue(), 0L)).toList(),
                orderStatusBreakdown(orderList),
                marketplacePerformance(orderList, settlementList, returnList),
                lowStock.content(),
                expenseBreakdown(expenseList),
                java.time.Instant.now());
    }

    private List<DashboardStatusResponse> orderStatusBreakdown(List<Order> orderList) {
        Map<OrderStatus, Long> counts = new LinkedHashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            counts.put(status, 0L);
        }
        for (Order order : orderList) {
            counts.computeIfPresent(order.getOrderStatus(), (status, count) -> count + 1);
        }
        return counts.entrySet().stream()
                .map(entry -> new DashboardStatusResponse(entry.getKey().name(), entry.getValue()))
                .toList();
    }

    private DashboardSummaryResponse.ReturnsSummaryResponse returnsSummary(RevenueMetricsResponse revenue) {
        long totalOrders = Math.max(revenue.orders(), 1L);
        BigDecimal returnRate = BigDecimal.valueOf(revenue.returnedOrders())
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP);
        BigDecimal rtoRate = BigDecimal.valueOf(revenue.rtoOrders())
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP);
        return new DashboardSummaryResponse.ReturnsSummaryResponse(
                revenue.returnedOrders(),
                revenue.rtoOrders(),
                returnRate,
                rtoRate);
    }

    private List<DashboardMarketplaceResponse> marketplacePerformance(List<Order> orderList, List<Settlement> settlementList, List<ReturnRecord> returnList) {
        Map<String, MarketplaceAccumulator> rows = new LinkedHashMap<>();

        for (Order order : orderList) {
            String marketplace = order.getMarketplace().getCode();
            MarketplaceAccumulator row = rows.computeIfAbsent(marketplace, ignored -> new MarketplaceAccumulator());
            row.orders++;
            if (isRecognizedSale(order)) {
                row.sales = row.sales.add(safe(order.getTotalOrderValue()));
            }
            if (order.getOrderStatus() == OrderStatus.RETURNED || order.getOrderStatus() == OrderStatus.RTO) {
                row.returnsRto++;
            }
        }

        for (Settlement settlement : settlementList) {
            String marketplace = settlement.getMarketplace().getCode();
            MarketplaceAccumulator row = rows.computeIfAbsent(marketplace, ignored -> new MarketplaceAccumulator());
            row.marketplaceFees = row.marketplaceFees.add(safe(settlement.getMarketplaceFees()));
            row.shippingCharges = row.shippingCharges.add(safe(settlement.getShippingCharges()));
            row.returnCharges = row.returnCharges.add(safe(settlement.getReturnCharges()));
            row.settlements = row.settlements.add(safe(settlement.getReceivedAmount()));
            row.otherCharges = row.otherCharges.add(safe(settlement.getOtherCharges()));
        }

        for (ReturnRecord record : returnList) {
            String marketplace = record.getOrder().getMarketplace().getCode();
            MarketplaceAccumulator row = rows.computeIfAbsent(marketplace, ignored -> new MarketplaceAccumulator());
            row.returnLoss = row.returnLoss.add(safe(record.getTotalLoss()));
        }

        List<DashboardMarketplaceResponse> performance = new ArrayList<>();
        for (Map.Entry<String, MarketplaceAccumulator> entry : rows.entrySet()) {
            MarketplaceAccumulator row = entry.getValue();
            BigDecimal netRevenue = row.sales
                    .subtract(row.marketplaceFees)
                    .subtract(row.shippingCharges)
                    .subtract(row.returnCharges)
                    .subtract(row.otherCharges);
            BigDecimal profit = netRevenue.subtract(row.returnLoss);
            performance.add(new DashboardMarketplaceResponse(
                    entry.getKey(),
                    row.orders,
                    row.sales,
                    row.returnsRto,
                    row.marketplaceFees,
                    row.shippingCharges,
                    row.returnCharges,
                    row.settlements,
                    netRevenue,
                    profit));
        }
        return performance;
    }

    private RevenueMetricsResponse revenue(List<Order> orderList) {
        BigDecimal deliveredSales = BigDecimal.ZERO;
        BigDecimal returnedSales = BigDecimal.ZERO;
        BigDecimal rtoSales = BigDecimal.ZERO;
        BigDecimal cancelledSales = BigDecimal.ZERO;
        long deliveredOrders = 0;
        long returnedOrders = 0;
        long rtoOrders = 0;
        long cancelledOrders = 0;

        for (Order order : orderList) {
            BigDecimal total = safe(order.getTotalOrderValue());
            switch (order.getOrderStatus()) {
                case DELIVERED -> {
                    deliveredOrders++;
                    deliveredSales = deliveredSales.add(total);
                }
                case RETURNED -> {
                    returnedOrders++;
                    returnedSales = returnedSales.add(total);
                }
                case RTO -> {
                    rtoOrders++;
                    rtoSales = rtoSales.add(total);
                }
                case CANCELLED -> {
                    cancelledOrders++;
                    cancelledSales = cancelledSales.add(total);
                }
                default -> {
                }
            }
        }

        BigDecimal grossSales = deliveredSales.add(returnedSales).add(rtoSales);
        return new RevenueMetricsResponse(grossSales, deliveredSales, returnedSales, rtoSales, cancelledSales,
                orderList.size(), deliveredOrders, returnedOrders, rtoOrders, cancelledOrders);
    }

    private CostMetricsResponse costs(List<Order> orderList, List<Settlement> settlementList, List<ReturnRecord> returnList) {
        BigDecimal productCost = BigDecimal.ZERO;
        for (Order order : orderList) {
            if (!isRecognizedSale(order)) {
                continue;
            }
            for (var item : order.getItems()) {
                productCost = productCost.add(safe(item.getVariant().getCostPrice()).multiply(BigDecimal.valueOf(item.getQuantity())));
            }
        }

        BigDecimal marketplaceFees = BigDecimal.ZERO;
        BigDecimal shippingCharges = BigDecimal.ZERO;
        BigDecimal returnCharges = BigDecimal.ZERO;
        BigDecimal otherCharges = BigDecimal.ZERO;
        BigDecimal settlementReceived = BigDecimal.ZERO;
        for (Settlement settlement : settlementList) {
            marketplaceFees = marketplaceFees.add(safe(settlement.getMarketplaceFees()));
            shippingCharges = shippingCharges.add(safe(settlement.getShippingCharges()));
            returnCharges = returnCharges.add(safe(settlement.getReturnCharges()));
            otherCharges = otherCharges.add(safe(settlement.getOtherCharges()));
            settlementReceived = settlementReceived.add(safe(settlement.getReceivedAmount()));
        }

        BigDecimal returnLoss = returnList.stream().map(ReturnRecord::getTotalLoss).map(this::safe).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal settlementDifference = settlementList.stream()
                .map(settlement -> safe(settlement.getReceivedAmount()).subtract(safe(settlement.getNetAmount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CostMetricsResponse(productCost, returnLoss, marketplaceFees, shippingCharges, returnCharges, otherCharges, settlementReceived, settlementDifference);
    }

    private ExpenseSummaryResponse expenseSummary(List<Expense> expenseList) {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal paid = BigDecimal.ZERO;
        BigDecimal pending = BigDecimal.ZERO;
        for (Expense expense : expenseList) {
            total = total.add(safe(expense.getTotalAmount()));
            if (expense.getPaymentStatus() == PaymentStatus.PAID) {
                paid = paid.add(safe(expense.getTotalAmount()));
            } else {
                pending = pending.add(safe(expense.getTotalAmount()));
            }
        }
        return new ExpenseSummaryResponse(total, paid, pending);
    }

    private ProfitMetricsResponse profit(RevenueMetricsResponse revenue, CostMetricsResponse costs, ExpenseSummaryResponse expenseSummary) {
        BigDecimal grossProfit = revenue.grossSales()
                .subtract(costs.productCost())
                .subtract(costs.marketplaceFees())
                .subtract(costs.shippingCharges())
                .subtract(costs.returnCharges())
                .subtract(costs.otherCharges())
                .subtract(costs.returnLoss());
        BigDecimal netProfit = grossProfit.subtract(expenseSummary.totalExpenses());
        BigDecimal profitMargin = revenue.grossSales().compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : netProfit.divide(revenue.grossSales(), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
        return new ProfitMetricsResponse(grossProfit, netProfit, profitMargin);
    }

    private boolean isRecognizedSale(Order order) {
        return order.getOrderStatus() == OrderStatus.DELIVERED
                || order.getOrderStatus() == OrderStatus.RETURNED
                || order.getOrderStatus() == OrderStatus.RTO;
    }

    private ReportRange resolveRange(ReportPeriod period, LocalDate fromDate, LocalDate toDate) {
        ReportPeriod selected = period == null ? ReportPeriod.THIS_MONTH : period;
        LocalDate today = LocalDate.now();
        return switch (selected) {
            case TODAY -> new ReportRange(selected, today, today);
            case THIS_WEEK -> new ReportRange(selected, today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), today);
            case THIS_MONTH -> new ReportRange(selected, today.withDayOfMonth(1), today);
            case PREVIOUS_MONTH -> {
                LocalDate firstDayOfThisMonth = today.withDayOfMonth(1);
                LocalDate lastDayOfPreviousMonth = firstDayOfThisMonth.minusDays(1);
                yield new ReportRange(selected, lastDayOfPreviousMonth.withDayOfMonth(1), lastDayOfPreviousMonth);
            }
            case CUSTOM -> {
                if (fromDate == null || toDate == null) {
                    throw new IllegalArgumentException("Custom dashboard period requires fromDate and toDate");
                }
                if (toDate.isBefore(fromDate)) {
                    throw new IllegalArgumentException("Dashboard toDate must be on or after fromDate");
                }
                yield new ReportRange(selected, fromDate, toDate);
            }
        };
    }

    private List<LocalDate> eachDate(LocalDate fromDate, LocalDate toDate) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate current = fromDate;
        while (!current.isAfter(toDate)) {
            dates.add(current);
            current = current.plusDays(1);
        }
        return dates;
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static final class MarketplaceAccumulator {
        private long orders;
        private long returnsRto;
        private BigDecimal sales = BigDecimal.ZERO;
        private BigDecimal marketplaceFees = BigDecimal.ZERO;
        private BigDecimal shippingCharges = BigDecimal.ZERO;
        private BigDecimal returnCharges = BigDecimal.ZERO;
        private BigDecimal otherCharges = BigDecimal.ZERO;
        private BigDecimal settlements = BigDecimal.ZERO;
        private BigDecimal returnLoss = BigDecimal.ZERO;
    }

    private record ReportRange(ReportPeriod period, LocalDate fromDate, LocalDate toDate) {
    }
}
