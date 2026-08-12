package com.ecommerce.commerceapi.reports.api;

import com.ecommerce.commerceapi.reports.domain.ReportPeriod;
import com.ecommerce.commerceapi.reports.service.ReportService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {
    private final ReportService service;

    public ReportController(ReportService service) {
        this.service = service;
    }

    @GetMapping("/financial")
    public FinancialReportResponse financial(@RequestParam(required = false) ReportPeriod period,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return service.financial(period, fromDate, toDate);
    }

    @GetMapping("/marketplaces")
    public List<MarketplacePerformanceResponse> marketplaces(@RequestParam(required = false) ReportPeriod period,
                                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return service.marketplaces(period, fromDate, toDate);
    }

    @GetMapping("/expenses")
    public List<ExpenseBreakdownResponse> expenses(@RequestParam(required = false) ReportPeriod period,
                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return service.expenses(period, fromDate, toDate);
    }

    @GetMapping("/products")
    public List<ProductPerformanceResponse> products(@RequestParam(required = false) ReportPeriod period,
                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return service.products(period, fromDate, toDate);
    }
}
