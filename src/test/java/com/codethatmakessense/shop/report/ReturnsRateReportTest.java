package com.codethatmakessense.shop.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.codethatmakessense.shop.order.LineRequest;
import com.codethatmakessense.shop.order.Order;
import com.codethatmakessense.shop.order.OrderService;
import com.codethatmakessense.shop.returns.application.ReturnService;
import com.codethatmakessense.shop.returns.domain.OrderId;
import com.codethatmakessense.shop.returns.domain.Quantity;
import com.codethatmakessense.shop.returns.domain.Sku;
import com.codethatmakessense.shop.stock.StockService;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ReturnsRateReportTest {

    @Autowired
    OrderService orderService;

    @Autowired
    StockService stock;

    @Autowired
    ReturnService returns;

    @Autowired
    ReturnsRateReport report;

    @Autowired
    EntityManager entityManager;

    @Test
    void relatesWhatCameBackToWhatShipped() {
        stock.receive("BOOK-1", 10);
        Order order = orderService.place("viktor@example.com", List.of(new LineRequest("BOOK-1", 4, 1_500)));
        orderService.pay(order.getId(), "PAY-42");
        orderService.ship(order.getId(), "DHL", "TRACK-1");
        returns.request(new OrderId(order.getId()), new Sku("BOOK-1"), new Quantity(1));
        entityManager.flush();

        List<ReturnsRate> rates = report.bySku();

        assertThat(rates).hasSize(1);
        assertThat(rates.getFirst().rate()).isCloseTo(0.25, within(0.001));
    }
}
