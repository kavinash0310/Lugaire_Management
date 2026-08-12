package com.ecommerce.commerceapi.reports.service;

import com.ecommerce.commerceapi.expenses.domain.Expense;
import com.ecommerce.commerceapi.expenses.domain.ExpensePaymentStatus;
import com.ecommerce.commerceapi.expenses.repository.ExpenseRepository;
import com.ecommerce.commerceapi.orders.domain.Order;
import com.ecommerce.commerceapi.orders.domain.OrderPlatform;
import com.ecommerce.commerceapi.orders.domain.OrderStatus;
import com.ecommerce.commerceapi.orders.domain.PaymentStatus;
import com.ecommerce.commerceapi.orders.repository.OrderRepository;
import com.ecommerce.commerceapi.reports.api.CostMetricsResponse;
import com.ecommerce.commerceapi.reports.api.ExpenseBreakdownResponse;
import com.ecommerce.commerceapi.reports.api.ExpenseSummaryResponse;
import com.ecommerce.commerceapi.reports.api.FinancialReportResponse;
import com.ecommerce.commerceapi.reports.api.MarketplacePerformanceResponse;
import com.ecommerce.commerceapi.reports.api.ProductPerformanceResponse;
import com.ecommerce.commerceapi.reports.api.ProfitMetricsResponse;
import com.ecommerce.commerceapi.reports.api.RevenueMetricsResponse;
import com.ecommerce.commerceapi.reports.domain.ReportPeriod;
import com.ecommerce.commerceapi.returns.domain.ReturnRecord;
import com.ecommerce.commerceapi.returns.domain.ReturnType;
import com.ecommerce.commerceapi.returns.repository.ReturnRecordRepository;
import com.ecommerce.commerceapi.settlements.domain.Settlement;
import com.ecommerce.commerceapi.settlements.repository.SettlementRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {
    private final OrderRepository orders;
    private final SettlementRepository settlements;
    private final ExpenseRepository expenses;
    private final ReturnRecordRepository returns;

    public ReportService(OrderRepository orders, SettlementRepository settlements, ExpenseRepository expenses, ReturnRecordRepository returns) {
        this.orders = orders;
        this.settlements = settlements;
        this.expenses = expenses;
        this.returns = returns;
    }

    @Transactional(readOnly = true)
    public FinancialReportResponse financial(ReportPeriod period, LocalDate fromDate, LocalDate toDate) {
        ReportRange range = resolveRange(period, fromDate, toDate);
        return buildReport(range);
    }

    @Transactional(readOnly = true)
    public List<MarketplacePerformanceResponse> marketplaces(ReportPeriod period, LocalDate fromDate, LocalDate toDate) {
        return buildReport(resolveRange(period, fromDate, toDate)).marketplaces();
    }

    @Transactional(readOnly = true)
    public List<ExpenseBreakdownResponse> expenses(ReportPeriod period, LocalDate fromDate, LocalDate toDate) {
        return buildReport(resolveRange(period, fromDate, toDate)).expenseBreakdown();
    }

    @Transactional(readOnly = true)
    public List<ProductPerformanceResponse> products(ReportPeriod period, LocalDate fromDate, LocalDate toDate) {
        return buildReport(resolveRange(period, fromDate, toDate)).products();
    }

    private FinancialReportResponse buildReport(ReportRange range) {
        List<Order> orderList = orders.findAllByOrderDateBetween(range.fromDate(), range.toDate());
        List<Settlement> settlementList = settlements.findAllBySettlementDateBetween(range.fromDate(), range.toDate());
        List<Expense> expenseList = expenses.findAllByExpenseDateBetween(range.fromDate(), range.toDate());
        List<ReturnRecord> returnList = returns.findAllByReturnDateBetween(range.fromDate(), range.toDate());

        RevenueMetricsResponse revenue = revenue(orderList);
        CostMetricsResponse costMetrics = costs(orderList, settlementList, returnList);
        ExpenseSummaryResponse expenseSummary = expenseSummary(expenseList);
        ProfitMetricsResponse profit = profit(revenue, costMetrics, expenseSummary);

        return new FinancialReportResponse(
                range.period().name(),
                range.fromDate(),
                range.toDate(),
                revenue,
                costMetrics,
                expenseSummary,
                profit,
                marketplacePerformance(orderList, settlementList, returnList),
                expenseBreakdown(expenseList),
                productPerformance(orderList, returnList),
                Instant.now());
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
            if (expense.getPaymentStatus() == ExpensePaymentStatus.PAID) {
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

    private List<MarketplacePerformanceResponse> marketplacePerformance(List<Order> orderList, List<Settlement> settlementList, List<ReturnRecord> returnList) {
        Map<OrderPlatform, MarketplaceAccumulator> rows = new EnumMap<>(OrderPlatform.class);
        for (OrderPlatform platform : OrderPlatform.values()) {
            rows.put(platform, new MarketplaceAccumulator());
        }

        for (Order order : orderList) {
            if (!isRecognizedSale(order)) {
                continue;
            }
            MarketplaceAccumulator row = rows.get(order.getPlatform());
            row.orders++;
            row.sales = row.sales.add(safe(order.getTotalOrderValue()));
            for (var item : order.getItems()) {
                row.productCost = row.productCost.add(safe(item.getVariant().getCostPrice()).multiply(BigDecimal.valueOf(item.getQuantity())));
            }
        }

        for (Settlement settlement : settlementList) {
            MarketplaceAccumulator row = rows.get(settlement.getPlatform());
            row.marketplaceFees = row.marketplaceFees.add(safe(settlement.getMarketplaceFees()));
            row.shippingCharges = row.shippingCharges.add(safe(settlement.getShippingCharges()));
            row.returnCharges = row.returnCharges.add(safe(settlement.getReturnCharges()));
            row.settlements = row.settlements.add(safe(settlement.getReceivedAmount()));
            row.otherCharges = row.otherCharges.add(safe(settlement.getOtherCharges()));
        }

        for (ReturnRecord record : returnList) {
            MarketplaceAccumulator row = rows.get(record.getOrder().getPlatform());
            row.returnLoss = row.returnLoss.add(safe(record.getTotalLoss()));
        }

        List<MarketplacePerformanceResponse> performance = new ArrayList<>();
        for (OrderPlatform platform : OrderPlatform.values()) {
            MarketplaceAccumulator row = rows.get(platform);
            BigDecimal netRevenue = row.sales
                    .subtract(row.marketplaceFees)
                    .subtract(row.shippingCharges)
                    .subtract(row.returnCharges)
                    .subtract(row.otherCharges);
            BigDecimal profit = netRevenue.subtract(row.productCost).subtract(row.returnLoss);
            performance.add(new MarketplacePerformanceResponse(platform.name(), row.orders, row.sales,
                    row.marketplaceFees, row.shippingCharges, row.returnCharges, row.settlements, netRevenue, profit));
        }
        return performance;
    }

    private List<ExpenseBreakdownResponse> expenseBreakdown(List<Expense> expenseList) {
        Map<Long, ExpenseAccumulator> rows = new HashMap<>();
        BigDecimal total = expenseList.stream().map(Expense::getTotalAmount).map(this::safe).reduce(BigDecimal.ZERO, BigDecimal::add);
        for (Expense expense : expenseList) {
            Long categoryId = expense.getCategory().getId();
            ExpenseAccumulator row = rows.computeIfAbsent(categoryId, ignored -> new ExpenseAccumulator(expense.getCategory().getName()));
            row.amount = row.amount.add(safe(expense.getTotalAmount()));
        }
        return rows.entrySet().stream()
                .map(entry -> {
                    ExpenseAccumulator row = entry.getValue();
                    BigDecimal percentage = total.compareTo(BigDecimal.ZERO) == 0
                            ? BigDecimal.ZERO
                            : row.amount.divide(total, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
                    return new ExpenseBreakdownResponse(entry.getKey(), row.categoryName, row.amount, percentage);
                })
                .sorted(Comparator.comparing(ExpenseBreakdownResponse::amount).reversed())
                .toList();
    }

    private List<ProductPerformanceResponse> productPerformance(List<Order> orderList, List<ReturnRecord> returnList) {
        Map<Long, ProductAccumulator> rows = new HashMap<>();
        for (Order order : orderList) {
            if (!isRecognizedSale(order)) {
                continue;
            }
            for (var item : order.getItems()) {
                Long variantId = item.getVariant().getId();
                ProductAccumulator row = rows.computeIfAbsent(variantId, ignored -> new ProductAccumulator(
                        item.getVariant().getProduct().getId(),
                        item.getVariant().getProduct().getProductName(),
                        item.getSkuSnapshot()));
                row.unitsSold += item.getQuantity();
                row.revenue = row.revenue.add(safe(item.getLineTotal()));
                BigDecimal unitCost = safe(item.getVariant().getCostPrice());
                row.productCost = row.productCost.add(unitCost.multiply(BigDecimal.valueOf(item.getQuantity())));
            }
        }
        for (ReturnRecord record : returnList) {
            Long variantId = record.getVariant().getId();
            ProductAccumulator row = rows.computeIfAbsent(variantId, ignored -> new ProductAccumulator(
                    record.getVariant().getProduct().getId(),
                    record.getVariant().getProduct().getProductName(),
                    record.getVariant().getSku()));
            row.returnsLoss = row.returnsLoss.add(safe(record.getTotalLoss()));
        }

        return rows.values().stream()
                .map(row -> new ProductPerformanceResponse(
                        row.productId,
                        row.productName,
                        row.sku,
                        row.unitsSold,
                        row.revenue,
                        row.productCost,
                        row.returnsLoss,
                        row.revenue.subtract(row.productCost).subtract(row.returnsLoss)))
                .sorted(Comparator.comparing(ProductPerformanceResponse::revenue).reversed())
                .toList();
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
                    throw new IllegalArgumentException("Custom report period requires fromDate and toDate");
                }
                if (toDate.isBefore(fromDate)) {
                    throw new IllegalArgumentException("Report toDate must be on or after fromDate");
                }
                yield new ReportRange(selected, fromDate, toDate);
            }
        };
    }

    private boolean isRecognizedSale(Order order) {
        return order.getOrderStatus() == OrderStatus.DELIVERED
                || order.getOrderStatus() == OrderStatus.RETURNED
                || order.getOrderStatus() == OrderStatus.RTO;
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private record ReportRange(ReportPeriod period, LocalDate fromDate, LocalDate toDate) {}

    private static final class MarketplaceAccumulator {
        private long orders;
        private BigDecimal sales = BigDecimal.ZERO;
        private BigDecimal marketplaceFees = BigDecimal.ZERO;
        private BigDecimal shippingCharges = BigDecimal.ZERO;
        private BigDecimal returnCharges = BigDecimal.ZERO;
        private BigDecimal otherCharges = BigDecimal.ZERO;
        private BigDecimal settlements = BigDecimal.ZERO;
        private BigDecimal productCost = BigDecimal.ZERO;
        private BigDecimal returnLoss = BigDecimal.ZERO;
    }

    private static final class ExpenseAccumulator {
        private final String categoryName;
        private BigDecimal amount = BigDecimal.ZERO;

        private ExpenseAccumulator(String categoryName) {
            this.categoryName = categoryName;
        }
    }

    private static final class ProductAccumulator {
        private final Long productId;
        private final String productName;
        private final String sku;
        private long unitsSold;
        private BigDecimal revenue = BigDecimal.ZERO;
        private BigDecimal productCost = BigDecimal.ZERO;
        private BigDecimal returnsLoss = BigDecimal.ZERO;

        private ProductAccumulator(Long productId, String productName, String sku) {
            this.productId = productId;
            this.productName = productName;
            this.sku = sku;
        }
    }
}
