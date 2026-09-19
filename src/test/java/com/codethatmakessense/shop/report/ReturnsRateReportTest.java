package com.codethatmakessense.shop.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.codethatmakessense.shop.order.LineRequest;
import com.codethatmakessense.shop.order.OrderService;
import com.codethatmakessense.shop.returns.application.ReturnService;
import com.codethatmakessense.shop.shared.OrderId;
import com.codethatmakessense.shop.shared.Quantity;
import com.codethatmakessense.shop.shared.Sku;
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
        Long orderId = orderService.place("viktor@example.com", List.of(new LineRequest("BOOK-1", 4, 1_500)));
        orderService.pay(orderId, "PAY-42");
        orderService.ship(orderId, "DHL", "TRACK-1");
        returns.request(new OrderId(orderId), new Sku("BOOK-1"), new Quantity(1));
        entityManager.flush();

        List<ReturnsRate> rates = report.bySku();

        assertThat(rates).hasSize(1);
        assertThat(rates.getFirst().rate()).isCloseTo(0.25, within(0.001));
    }
}
