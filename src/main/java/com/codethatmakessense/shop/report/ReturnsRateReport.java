package com.codethatmakessense.shop.report;

import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class ReturnsRateReport {

    private final JdbcClient jdbc;

    public ReturnsRateReport(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<ReturnsRate> bySku() {
        return jdbc.sql("""
                SELECT s.sku, s.shipped, COALESCE(r.returned, 0) AS returned
                FROM (SELECT sku, SUM(quantity) AS shipped FROM shipment_lines GROUP BY sku) s
                LEFT JOIN (SELECT sku, SUM(quantity) AS returned
                           FROM return_requests
                           WHERE status <> 'REJECTED'
                           GROUP BY sku) r ON r.sku = s.sku
                ORDER BY s.sku
                """)
                .query((row, index) -> new ReturnsRate(
                        row.getString("sku"), row.getLong("shipped"), row.getLong("returned")))
                .list();
    }
}
