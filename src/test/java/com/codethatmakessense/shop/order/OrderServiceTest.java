package com.codethatmakessense.shop.order;

import com.codethatmakessense.shop.order.domain.OrderStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codethatmakessense.shop.order.adapter.jpa.OrderRow;
import com.codethatmakessense.shop.order.adapter.jpa.OrderRowRepository;

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

    @Autowired
    OrderService orderService;

    @Autowired
    OrderRowRepository orders;

    @Autowired
    StockService stock;

    @BeforeEach
    void stockTheShelves() {
        stock.receive("BOOK-1", 10);
        stock.receive("MUG-7", 5);
    }

    @Test
    void placesAnOrderWithItsLinesAndTotal() {
        Long placed = orderService.place("viktor@example.com", List.of(
                new LineRequest("BOOK-1", 2, 1_500),
                new LineRequest("MUG-7", 1, 900)));

        OrderRow stored = orders.findById(placed).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(stored.getLines()).hasSize(2);
        assertThat(stored.getTotalCents()).isEqualTo(3_900);
    }

    @Test
    void paysAPlacedOrder() {
        Long placed = place();

        orderService.pay(placed, "PAY-42");

        OrderRow stored = orders.findById(placed).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(stored.getPaidOn()).isNotNull();
        assertThat(stored.getPaymentReference()).isEqualTo("PAY-42");
    }

    @Test
    void refusesToPayTwice() {
        Long placed = place();
        orderService.pay(placed, "PAY-42");

        assertThatThrownBy(() -> orderService.pay(placed, "PAY-43"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shipsAPaidOrder() {
        Long placed = place();
        orderService.pay(placed, "PAY-42");

        orderService.ship(placed, "DHL", "TRACK-1");

        OrderRow stored = orders.findById(placed).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(stored.getShippedOn()).isNotNull();
        assertThat(stored.getTrackingNumber()).isEqualTo("TRACK-1");
    }

    @Test
    void refusesToShipAnUnpaidOrder() {
        Long placed = place();

        assertThatThrownBy(() -> orderService.ship(placed, "DHL", "TRACK-1"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cancelsAnUnshippedOrder() {
        Long placed = place();
        orderService.pay(placed, "PAY-42");

        orderService.cancel(placed, "changed my mind");

        OrderRow stored = orders.findById(placed).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(stored.getCancelledOn()).isNotNull();
        assertThat(stored.getCancellationReason()).isEqualTo("changed my mind");
    }

    @Test
    void refusesToCancelAShippedOrder() {
        Long placed = place();
        orderService.pay(placed, "PAY-42");
        orderService.ship(placed, "DHL", "TRACK-1");

        assertThatThrownBy(() -> orderService.cancel(placed, "too late"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void reservesStockWhenPlacingAndReleasesItWhenCancelling() {
        Long placed = place();
        assertThat(stock.available("BOOK-1")).isEqualTo(8);

        orderService.cancel(placed, "changed my mind");

        assertThat(stock.available("BOOK-1")).isEqualTo(10);
    }

    @Test
    void consumesStockWhenShipping() {
        Long placed = place();
        orderService.pay(placed, "PAY-42");

        orderService.ship(placed, "DHL", "TRACK-1");

        assertThat(stock.available("BOOK-1")).isEqualTo(8);
    }

    @Test
    void refusesAnOrderTheStockCannotCover() {
        assertThatThrownBy(() -> orderService.place("viktor@example.com",
                List.of(new LineRequest("MUG-7", 6, 900))))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void remembersGiftWrapping() {
        Long placed = orderService.place("viktor@example.com",
                List.of(new LineRequest("BOOK-1", 1, 1_500)), true, "Happy reading");

        OrderRow stored = orders.findById(placed).orElseThrow();
        assertThat(stored.isGiftWrap()).isTrue();
    }

    @Test
    void shipsPartOfAnOrderAndKeepsTheRestOpen() {
        Long placed = place();
        orderService.pay(placed, "PAY-42");

        orderService.ship(placed, "DHL", "TRACK-1",
                List.of(new ShipmentRequest("BOOK-1", 2)));

        OrderRow stored = orders.findById(placed).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(OrderStatus.PARTIALLY_SHIPPED);
        assertThat(stored.getShipments()).hasSize(1);
        assertThat(orderService.isShippable(stored)).isTrue();

        orderService.ship(placed, "DHL", "TRACK-2",
                List.of(new ShipmentRequest("MUG-7", 1)));

        assertThat(stored.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(stored.getShipments()).hasSize(2);
        assertThat(stored.getTrackingNumber()).isEqualTo("TRACK-1");
    }

    @Test
    void refusesToShipMoreThanOrdered() {
        Long placed = place();
        orderService.pay(placed, "PAY-42");

        assertThatThrownBy(() -> orderService.ship(placed, "DHL", "TRACK-1",
                List.of(new ShipmentRequest("BOOK-1", 3))))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsAnOrderWithoutLines() {
        assertThatThrownBy(() -> orderService.place("viktor@example.com", List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsANonPositiveQuantity() {
        assertThatThrownBy(() -> orderService.place("viktor@example.com",
                List.of(new LineRequest("BOOK-1", 0, 1_500))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Long place() {
        return orderService.place("viktor@example.com", List.of(
                new LineRequest("BOOK-1", 2, 1_500),
                new LineRequest("MUG-7", 1, 900)));
    }
}
