package com.codethatmakessense.shop.report;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reports")
public class ReportController {

    private final SalesReport sales;
    private final ReturnsRateReport returnsRate;

    public ReportController(SalesReport sales, ReturnsRateReport returnsRate) {
        this.sales = sales;
        this.returnsRate = returnsRate;
    }

    @GetMapping("/sales")
    public List<DailySales> sales() {
        return sales.dailySales();
    }

    @GetMapping("/returns-rate")
    public List<ReturnsRate> returnsRate() {
        return returnsRate.bySku();
    }
}
