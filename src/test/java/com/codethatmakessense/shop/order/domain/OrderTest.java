package com.codethatmakessense.shop.order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codethatmakessense.shop.shared.Money;
import com.codethatmakessense.shop.shared.OrderId;
import com.codethatmakessense.shop.shared.Quantity;
import com.codethatmakessense.shop.shared.Sku;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrderTest {

    static final LocalDate TODAY = LocalDate.of(2026, 9, 22);
    static final Sku BOOK = new Sku("BOOK-1");
    static final Sku MUG = new Sku("MUG-7");
    static final CustomerEmail VIKTOR = new CustomerEmail("viktor@example.com");

    @Test
    void placesAnOrderAndKnowsItsTotal() {
        Order order = placed();

        assertThat(order.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(order.total()).isEqualTo(new Money(3_900));
    }

    @Test
    void refusesAnOrderWithoutLines() {
        assertThatThrownBy(() -> Order.place(new OrderId(1), VIKTOR, List.of(), false, TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refusesALineWithNothingOnIt() {
        assertThatThrownBy(() -> new OrderLine(BOOK, Quantity.NONE, new Money(1_500)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void paysAPlacedOrderOnce() {
        Order order = placed();

        order.pay("PAY-42", TODAY);

        assertThat(order.status()).isEqualTo(OrderStatus.PAID);
        assertThat(order.payment()).contains(new Payment("PAY-42", TODAY));
        assertThatThrownBy(() -> order.pay("PAY-43", TODAY)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void refusesToShipBeforePayment() {
        Order order = placed();

        assertThatThrownBy(() -> order.ship(List.of(new ShipmentLine(BOOK, new Quantity(1))), "DHL", "T-1", TODAY))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shipsInPartsUntilNothingIsLeft() {
        Order order = paid();

        order.ship(List.of(new ShipmentLine(BOOK, new Quantity(2))), "DHL", "T-1", TODAY);
        assertThat(order.status()).isEqualTo(OrderStatus.PARTIALLY_SHIPPED);
        assertThat(order.remainingToShip(MUG)).isEqualTo(new Quantity(1));
        assertThat(order.isShippable()).isTrue();

        order.ship(List.of(new ShipmentLine(MUG, new Quantity(1))), "DHL", "T-2", TODAY);
        assertThat(order.status()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(order.shipments()).hasSize(2);
        assertThat(order.isShippable()).isFalse();
    }

    @Test
    void refusesToShipMoreThanOrdered() {
        Order order = paid();

        assertThatThrownBy(() -> order.ship(List.of(new ShipmentLine(BOOK, new Quantity(3))), "DHL", "T-1", TODAY))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cancelsAnOrderThatHasNotShipped() {
        Order order = paid();

        order.cancel("changed my mind", TODAY);

        assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.cancellation()).contains(new Cancellation("changed my mind", TODAY));
    }

    @Test
    void refusesToCancelAShippedOrder() {
        Order order = paid();
        order.ship(List.of(new ShipmentLine(BOOK, new Quantity(2)), new ShipmentLine(MUG, new Quantity(1))),
                "DHL", "T-1", TODAY);

        assertThatThrownBy(() -> order.cancel("too late", TODAY)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void refusesToCancelAPartiallyShippedOrder() {
        Order order = paid();
        order.ship(List.of(new ShipmentLine(BOOK, new Quantity(2))), "DHL", "T-1", TODAY);

        assertThatThrownBy(() -> order.cancel("half of it is gone already", TODAY))
                .isInstanceOf(IllegalStateException.class);
    }

    private static Order placed() {
        return Order.place(new OrderId(1), VIKTOR, List.of(
                new OrderLine(BOOK, new Quantity(2), new Money(1_500)),
                new OrderLine(MUG, new Quantity(1), new Money(900))), false, TODAY);
    }

    private static Order paid() {
        Order order = placed();
        order.pay("PAY-42", TODAY);
        return order;
    }
}
