package com.codethatmakessense.shop.report;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reports")
public class ReportController {

    private final SalesReport sales;

    public ReportController(SalesReport sales) {
        this.sales = sales;
    }

    @GetMapping("/sales")
    public List<DailySales> sales() {
        return sales.dailySales();
    }
}
