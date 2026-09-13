package com.codethatmakessense.shop.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
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
    OrderRepository orders;

    @Test
    void placesAnOrderWithItsLinesAndTotal() {
        Order placed = orderService.place("viktor@example.com", List.of(
                new LineRequest("BOOK-1", 2, 1_500),
                new LineRequest("MUG-7", 1, 900)));

        Order stored = orders.findById(placed.getId()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(stored.getLines()).hasSize(2);
        assertThat(stored.getTotalCents()).isEqualTo(3_900);
    }

    @Test
    void paysAPlacedOrder() {
        Order placed = place();

        orderService.pay(placed.getId(), "PAY-42");

        Order stored = orders.findById(placed.getId()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(stored.getPaidOn()).isNotNull();
        assertThat(stored.getPaymentReference()).isEqualTo("PAY-42");
    }

    @Test
    void refusesToPayTwice() {
        Order placed = place();
        orderService.pay(placed.getId(), "PAY-42");

        assertThatThrownBy(() -> orderService.pay(placed.getId(), "PAY-43"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shipsAPaidOrder() {
        Order placed = place();
        orderService.pay(placed.getId(), "PAY-42");

        orderService.ship(placed.getId(), "DHL", "TRACK-1");

        Order stored = orders.findById(placed.getId()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(stored.getShippedOn()).isNotNull();
        assertThat(stored.getTrackingNumber()).isEqualTo("TRACK-1");
    }

    @Test
    void refusesToShipAnUnpaidOrder() {
        Order placed = place();

        assertThatThrownBy(() -> orderService.ship(placed.getId(), "DHL", "TRACK-1"))
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

    private Order place() {
        return orderService.place("viktor@example.com", List.of(
                new LineRequest("BOOK-1", 2, 1_500),
                new LineRequest("MUG-7", 1, 900)));
    }
}
