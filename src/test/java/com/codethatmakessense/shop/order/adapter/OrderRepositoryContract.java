package com.codethatmakessense.shop.order.adapter;

import static org.assertj.core.api.Assertions.assertThat;

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
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

public abstract class OrderRepositoryContract {

    static final LocalDate TODAY = LocalDate.of(2026, 9, 22);
    static final Sku BOOK = new Sku("BOOK-1");
    static final Sku MUG = new Sku("MUG-7");

    protected abstract OrderRepository repository();

    @Test
    void handsOutFreshIds() {
        assertThat(repository().nextId()).isNotEqualTo(repository().nextId());
    }

    @Test
    void findsWhatItSaved() {
        Order order = placed();

        repository().save(order);

        Order found = repository().findById(order.id()).orElseThrow();
        assertThat(found.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(found.customer()).isEqualTo(new CustomerEmail("viktor@example.com"));
        assertThat(found.lines()).hasSize(2);
        assertThat(found.total()).isEqualTo(new Money(3_900));
        assertThat(found.giftWrap()).isTrue();
    }

    @Test
    void keepsPaymentsShipmentsAndCancellations() {
        Order order = placed();
        repository().save(order);

        order.pay("PAY-42", TODAY);
        order.ship(List.of(new ShipmentLine(BOOK, new Quantity(1))), "DHL", "T-1", TODAY);
        repository().save(order);

        Order found = repository().findById(order.id()).orElseThrow();
        assertThat(found.status()).isEqualTo(OrderStatus.PARTIALLY_SHIPPED);
        assertThat(found.payment()).isPresent();
        assertThat(found.shipments()).hasSize(1);
        assertThat(found.remainingToShip(BOOK)).isEqualTo(new Quantity(1));

        found.ship(List.of(new ShipmentLine(BOOK, new Quantity(1)), new ShipmentLine(MUG, new Quantity(1))),
                "DHL", "T-2", TODAY);
        repository().save(found);

        Order shipped = repository().findById(order.id()).orElseThrow();
        assertThat(shipped.status()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(shipped.shipments()).hasSize(2);
    }

    @Test
    void keepsACancellation() {
        Order order = placed();
        repository().save(order);

        order.cancel("changed my mind", TODAY);
        repository().save(order);

        Order found = repository().findById(order.id()).orElseThrow();
        assertThat(found.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(found.cancellation()).isPresent();
    }

    @Test
    void findsNothingForAnUnknownId() {
        assertThat(repository().findById(new OrderId(999_999))).isEmpty();
    }

    private Order placed() {
        return Order.place(repository().nextId(), new CustomerEmail("viktor@example.com"), List.of(
                new OrderLine(BOOK, new Quantity(2), new Money(1_500)),
                new OrderLine(MUG, new Quantity(1), new Money(900))), true, TODAY);
    }
}
