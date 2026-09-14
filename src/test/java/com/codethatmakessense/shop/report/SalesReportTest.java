package com.codethatmakessense.shop.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.codethatmakessense.shop.order.LineRequest;
import com.codethatmakessense.shop.order.Order;
import com.codethatmakessense.shop.order.OrderService;
import com.codethatmakessense.shop.stock.StockService;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class SalesReportTest {

    @Autowired
    OrderService orderService;

    @Autowired
    StockService stock;

    @Autowired
    SalesReport report;

    @Autowired
    EntityManager entityManager;

    @Test
    void sumsTheOrdersOfADayAndSkipsCancelledOnes() {
        stock.receive("BOOK-1", 10);
        orderService.place("a@example.com", List.of(new LineRequest("BOOK-1", 1, 1_500)));
        orderService.place("b@example.com", List.of(new LineRequest("BOOK-1", 2, 1_500)));
        Order cancelled = orderService.place("c@example.com", List.of(new LineRequest("BOOK-1", 1, 1_500)));
        orderService.cancel(cancelled.getId(), "changed my mind");
        entityManager.flush();

        List<DailySales> sales = report.dailySales();

        assertThat(sales).hasSize(1);
        assertThat(sales.getFirst().orderCount()).isEqualTo(2);
        assertThat(sales.getFirst().totalCents()).isEqualTo(4_500);
    }
}
