package com.codethatmakessense.shop.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codethatmakessense.shop.order.adapter.jpa.OrderRow;
import com.codethatmakessense.shop.order.adapter.jpa.OrderRowRepository;
import com.codethatmakessense.shop.order.application.OrderService;
import com.codethatmakessense.shop.order.domain.CustomerEmail;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class OrderServiceTest {

    static final Sku BOOK = new Sku("BOOK-1");
    static final Sku MUG = new Sku("MUG-7");
    static final CustomerEmail VIKTOR = new CustomerEmail("viktor@example.com");

    @Autowired
    OrderService orderService;

    @Autowired
    OrderRowRepository orders;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    StockService stock;

    @BeforeEach
    void stockTheShelves() {
        stock.receive("BOOK-1", 10);
        stock.receive("MUG-7", 5);
    }

    @Test
    void placesAnOrderWithItsLinesAndTotal() {
        OrderId placed = place();

        OrderRow stored = orders.findById(placed.value()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(stored.getLines()).hasSize(2);
        assertThat(stored.getTotalCents()).isEqualTo(3_900);
    }

    @Test
    void paysAPlacedOrder() {
        OrderId placed = place();

        orderService.pay(placed, "PAY-42");

        OrderRow stored = orders.findById(placed.value()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(stored.getPaidOn()).isNotNull();
        assertThat(stored.getPaymentReference()).isEqualTo("PAY-42");
    }

    @Test
    void refusesToPayTwice() {
        OrderId placed = place();
        orderService.pay(placed, "PAY-42");

        assertThatThrownBy(() -> orderService.pay(placed, "PAY-43"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shipsAPaidOrder() {
        OrderId placed = place();
        orderService.pay(placed, "PAY-42");

        orderService.ship(placed, "DHL", "TRACK-1");

        OrderRow stored = orders.findById(placed.value()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(stored.getShippedOn()).isNotNull();
        assertThat(stored.getTrackingNumber()).isEqualTo("TRACK-1");
    }

    @Test
    void refusesToShipAnUnpaidOrder() {
        OrderId placed = place();

        assertThatThrownBy(() -> orderService.ship(placed, "DHL", "TRACK-1"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cancelsAnUnshippedOrder() {
        OrderId placed = place();
        orderService.pay(placed, "PAY-42");

        orderService.cancel(placed, "changed my mind");

        OrderRow stored = orders.findById(placed.value()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(stored.getCancelledOn()).isNotNull();
        assertThat(stored.getCancellationReason()).isEqualTo("changed my mind");
    }

    @Test
    void refusesToCancelAShippedOrder() {
        OrderId placed = place();
        orderService.pay(placed, "PAY-42");
        orderService.ship(placed, "DHL", "TRACK-1");

        assertThatThrownBy(() -> orderService.cancel(placed, "too late"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void reservesStockWhenPlacingAndReleasesItWhenCancelling() {
        OrderId placed = place();
        assertThat(stock.available("BOOK-1")).isEqualTo(8);

        orderService.cancel(placed, "changed my mind");

        assertThat(stock.available("BOOK-1")).isEqualTo(10);
    }

    @Test
    void consumesStockWhenShipping() {
        OrderId placed = place();
        orderService.pay(placed, "PAY-42");

        orderService.ship(placed, "DHL", "TRACK-1");

        assertThat(stock.available("BOOK-1")).isEqualTo(8);
    }

    @Test
    void refusesAnOrderTheStockCannotCover() {
        assertThatThrownBy(() -> orderService.place(VIKTOR,
                List.of(new OrderLine(MUG, new Quantity(6), new Money(900))), false))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void remembersGiftWrapping() {
        OrderId placed = orderService.place(VIKTOR,
                List.of(new OrderLine(BOOK, new Quantity(1), new Money(1_500))), true);

        OrderRow stored = orders.findById(placed.value()).orElseThrow();
        assertThat(stored.isGiftWrap()).isTrue();
    }

    @Test
    void shipsPartOfAnOrderAndKeepsTheRestOpen() {
        OrderId placed = place();
        orderService.pay(placed, "PAY-42");

        orderService.ship(placed, "DHL", "TRACK-1", List.of(new ShipmentLine(BOOK, new Quantity(2))));

        OrderRow stored = orders.findById(placed.value()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(OrderStatus.PARTIALLY_SHIPPED);
        assertThat(stored.getShipments()).hasSize(1);
        assertThat(orderRepository.findById(placed).orElseThrow().isShippable()).isTrue();

        orderService.ship(placed, "DHL", "TRACK-2", List.of(new ShipmentLine(MUG, new Quantity(1))));

        assertThat(stored.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(stored.getShipments()).hasSize(2);
        assertThat(stored.getTrackingNumber()).isEqualTo("TRACK-1");
    }

    @Test
    void refusesToShipMoreThanOrdered() {
        OrderId placed = place();
        orderService.pay(placed, "PAY-42");

        assertThatThrownBy(() -> orderService.ship(placed, "DHL", "TRACK-1",
                List.of(new ShipmentLine(BOOK, new Quantity(3)))))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsAnOrderWithoutLines() {
        assertThatThrownBy(() -> orderService.place(VIKTOR, List.of(), false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsANonPositiveQuantity() {
        assertThatThrownBy(() -> new OrderLine(BOOK, new Quantity(0), new Money(1_500)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private OrderId place() {
        return orderService.place(VIKTOR, List.of(
                new OrderLine(BOOK, new Quantity(2), new Money(1_500)),
                new OrderLine(MUG, new Quantity(1), new Money(900))), false);
    }
}
