package com.codethatmakessense.shop.report;

import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class SalesReport {

    private final JdbcClient jdbc;

    public SalesReport(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<DailySales> dailySales() {
        return jdbc.sql("""
                SELECT placed_on AS sales_day, COUNT(*) AS order_count, SUM(total_cents) AS total_cents
                FROM orders
                WHERE status <> 'CANCELLED'
                GROUP BY placed_on
                ORDER BY placed_on
                """)
                .query((row, index) -> new DailySales(
                        row.getDate("sales_day").toLocalDate(),
                        row.getLong("order_count"),
                        row.getLong("total_cents")))
                .list();
    }
}
