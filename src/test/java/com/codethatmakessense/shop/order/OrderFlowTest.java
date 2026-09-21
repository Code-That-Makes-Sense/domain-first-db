package com.codethatmakessense.shop.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codethatmakessense.shop.order.application.OrderService;
import com.codethatmakessense.shop.order.domain.CustomerEmail;
import com.codethatmakessense.shop.order.domain.Order;
import com.codethatmakessense.shop.order.domain.OrderLine;
import com.codethatmakessense.shop.order.domain.OrderRepository;
import com.codethatmakessense.shop.order.domain.OrderStatus;
import com.codethatmakessense.shop.order.domain.ShipmentLine;
import com.codethatmakessense.shop.shared.Money;
import com.codethatmakessense.shop.shared.OrderId;
import com.codethatmakessense.shop.shared.Quantity;
import com.codethatmakessense.shop.shared.Sku;
import com.codethatmakessense.shop.stock.StockService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class OrderFlowTest {

    static final Sku BOOK = new Sku("BOOK-1");
    static final Sku MUG = new Sku("MUG-7");

    @Autowired
    OrderService orderService;

    @Autowired
    OrderRepository orders;

    @Autowired
    StockService stock;

    @Test
    void placesPaysShipsAndRefusesToCancel() {
        stock.receive("BOOK-1", 10);
        stock.receive("MUG-7", 5);

        OrderId id = orderService.place(new CustomerEmail("viktor@example.com"), List.of(
                new OrderLine(BOOK, new Quantity(2), new Money(1_500)),
                new OrderLine(MUG, new Quantity(1), new Money(900))), false);
        orderService.pay(id, "PAY-42");
        orderService.ship(id, "DHL", "TRACK-1", List.of(new ShipmentLine(BOOK, new Quantity(2))));

        Order order = orders.findById(id).orElseThrow();
        assertThat(order.status()).isEqualTo(OrderStatus.PARTIALLY_SHIPPED);
        assertThat(stock.available("BOOK-1")).isEqualTo(8);
        assertThatThrownBy(() -> orderService.cancel(id, "too late")).isInstanceOf(IllegalStateException.class);

        orderService.ship(id, "DHL", "TRACK-2", List.of(new ShipmentLine(MUG, new Quantity(1))));

        assertThat(orders.findById(id).orElseThrow().status()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(stock.available("MUG-7")).isEqualTo(4);
    }
}
