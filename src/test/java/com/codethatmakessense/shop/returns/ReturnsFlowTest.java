package com.codethatmakessense.shop.returns;

import static org.assertj.core.api.Assertions.assertThat;

import com.codethatmakessense.shop.order.LineRequest;
import com.codethatmakessense.shop.order.Order;
import com.codethatmakessense.shop.order.OrderService;
import com.codethatmakessense.shop.returns.application.ReturnService;
import com.codethatmakessense.shop.shared.Money;
import com.codethatmakessense.shop.shared.OrderId;
import com.codethatmakessense.shop.shared.Quantity;
import com.codethatmakessense.shop.returns.domain.ReturnId;
import com.codethatmakessense.shop.returns.domain.ReturnRequest;
import com.codethatmakessense.shop.returns.domain.ReturnRequestRepository;
import com.codethatmakessense.shop.returns.domain.ReturnStatus;
import com.codethatmakessense.shop.shared.Sku;
import com.codethatmakessense.shop.stock.StockService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ReturnsFlowTest {

    @Autowired
    OrderService orderService;

    @Autowired
    StockService stock;

    @Autowired
    ReturnService returns;

    @Autowired
    ReturnRequestRepository requests;

    @Test
    void refundsAShippedItem() {
        stock.receive("BOOK-1", 10);
        Order order = orderService.place("viktor@example.com", List.of(new LineRequest("BOOK-1", 2, 1_500)));
        orderService.pay(order.getId(), "PAY-42");
        orderService.ship(order.getId(), "DHL", "TRACK-1");

        ReturnId id = returns.request(new OrderId(order.getId()), new Sku("BOOK-1"), new Quantity(1));
        returns.approve(id);
        returns.receive(id);
        returns.refund(id);

        ReturnRequest refunded = requests.findById(id).orElseThrow();
        assertThat(refunded.status()).isEqualTo(ReturnStatus.REFUNDED);
        assertThat(refunded.refundAmount()).isEqualTo(new Money(1_500));
    }
}
