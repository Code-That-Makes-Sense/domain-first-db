package com.codethatmakessense.shop.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codethatmakessense.shop.order.adapter.memory.InMemoryOrderRepository;
import com.codethatmakessense.shop.order.domain.CustomerEmail;
import com.codethatmakessense.shop.order.domain.Order;
import com.codethatmakessense.shop.order.domain.OrderLine;
import com.codethatmakessense.shop.order.domain.OrderStatus;
import com.codethatmakessense.shop.order.domain.ShipmentLine;
import com.codethatmakessense.shop.shared.Money;
import com.codethatmakessense.shop.shared.OrderId;
import com.codethatmakessense.shop.shared.Quantity;
import com.codethatmakessense.shop.shared.Sku;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrderServiceTest {

    static final Sku BOOK = new Sku("BOOK-1");
    static final Sku MUG = new Sku("MUG-7");
    static final CustomerEmail VIKTOR = new CustomerEmail("viktor@example.com");
    static final Clock TODAY = Clock.fixed(
            LocalDate.of(2026, 9, 22).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    InMemoryOrderRepository orders = new InMemoryOrderRepository();
    RecordingStockReservations stock = new RecordingStockReservations().shelve(BOOK, 10).shelve(MUG, 5);
    OrderService service = new OrderService(orders, stock, TODAY);

    @Test
    void placesAnOrderAndReservesItsStock() {
        OrderId id = place();

        Order stored = orders.findById(id).orElseThrow();
        assertThat(stored.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(stored.total()).isEqualTo(new Money(3_900));
        assertThat(stock.available(BOOK)).isEqualTo(8);
    }

    @Test
    void refusesAnOrderTheStockCannotCover() {
        assertThatThrownBy(() -> service.place(VIKTOR, List.of(new OrderLine(MUG, new Quantity(6), new Money(900))), false))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shipsInPartsAndConsumesStockAsItGoes() {
        OrderId id = place();
        service.pay(id, "PAY-42");

        service.ship(id, "DHL", "T-1", List.of(new ShipmentLine(BOOK, new Quantity(2))));
        assertThat(orders.findById(id).orElseThrow().status()).isEqualTo(OrderStatus.PARTIALLY_SHIPPED);
        assertThat(stock.available(BOOK)).isEqualTo(8);

        service.ship(id, "DHL", "T-2", List.of(new ShipmentLine(MUG, new Quantity(1))));
        assertThat(orders.findById(id).orElseThrow().status()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    void cancellingReleasesTheReservation() {
        OrderId id = place();

        service.cancel(id, "changed my mind");

        assertThat(orders.findById(id).orElseThrow().status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(stock.available(BOOK)).isEqualTo(10);
    }

    @Test
    void refusesToCancelAShippedOrder() {
        OrderId id = place();
        service.pay(id, "PAY-42");
        service.ship(id, "DHL", "T-1");

        assertThatThrownBy(() -> service.cancel(id, "too late")).isInstanceOf(IllegalStateException.class);
    }

    private OrderId place() {
        return service.place(VIKTOR, List.of(
                new OrderLine(BOOK, new Quantity(2), new Money(1_500)),
                new OrderLine(MUG, new Quantity(1), new Money(900))), false);
    }
}
