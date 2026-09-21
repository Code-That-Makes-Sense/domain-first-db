package com.codethatmakessense.shop.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.codethatmakessense.shop.BootsSpring;

import com.codethatmakessense.shop.order.application.OrderService;
import com.codethatmakessense.shop.order.domain.CustomerEmail;
import com.codethatmakessense.shop.order.domain.OrderLine;
import com.codethatmakessense.shop.shared.Money;
import com.codethatmakessense.shop.shared.Quantity;
import com.codethatmakessense.shop.shared.OrderId;
import com.codethatmakessense.shop.shared.Sku;
import com.codethatmakessense.shop.stock.StockService;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@BootsSpring
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
        orderService.place(new CustomerEmail("a@example.com"), books(1), false);
        orderService.place(new CustomerEmail("b@example.com"), books(2), false);
        OrderId cancelled = orderService.place(new CustomerEmail("c@example.com"), books(1), false);
        orderService.cancel(cancelled, "changed my mind");
        entityManager.flush();

        List<DailySales> sales = report.dailySales();

        assertThat(sales).hasSize(1);
        assertThat(sales.getFirst().orderCount()).isEqualTo(2);
        assertThat(sales.getFirst().totalCents()).isEqualTo(4_500);
    }

    private static List<OrderLine> books(int quantity) {
        return List.of(new OrderLine(new Sku("BOOK-1"), new Quantity(quantity), new Money(1_500)));
    }
}
