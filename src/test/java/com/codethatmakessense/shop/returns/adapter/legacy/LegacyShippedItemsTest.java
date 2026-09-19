package com.codethatmakessense.shop.returns.adapter.legacy;

import static org.assertj.core.api.Assertions.assertThat;

import com.codethatmakessense.shop.order.LineRequest;
import com.codethatmakessense.shop.order.Order;
import com.codethatmakessense.shop.order.OrderService;
import com.codethatmakessense.shop.order.ShipmentRequest;
import com.codethatmakessense.shop.shared.Money;
import com.codethatmakessense.shop.shared.OrderId;
import com.codethatmakessense.shop.shared.Quantity;
import com.codethatmakessense.shop.returns.domain.ShippedItem;
import com.codethatmakessense.shop.shared.Sku;
import com.codethatmakessense.shop.stock.StockService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class LegacyShippedItemsTest {

    @Autowired
    OrderService orderService;

    @Autowired
    StockService stock;

    @Autowired
    LegacyShippedItems shippedItems;

    @Test
    void reportsWhatShippedAcrossShipmentsAndNothingForTheRest() {
        stock.receive("BOOK-1", 10);
        stock.receive("MUG-7", 5);
        Order order = orderService.place("viktor@example.com", List.of(
                new LineRequest("BOOK-1", 3, 1_500),
                new LineRequest("MUG-7", 1, 900)));
        orderService.pay(order.getId(), "PAY-42");
        orderService.ship(order.getId(), "DHL", "TRACK-1", List.of(new ShipmentRequest("BOOK-1", 1)));
        orderService.ship(order.getId(), "DHL", "TRACK-2", List.of(new ShipmentRequest("BOOK-1", 2)));

        Optional<ShippedItem> books = shippedItems.shippedItem(new OrderId(order.getId()), new Sku("BOOK-1"));
        Optional<ShippedItem> mugs = shippedItems.shippedItem(new OrderId(order.getId()), new Sku("MUG-7"));

        assertThat(books).isPresent();
        assertThat(books.get().quantity()).isEqualTo(new Quantity(3));
        assertThat(books.get().unitPrice()).isEqualTo(new Money(1_500));
        assertThat(books.get().shippedOn()).isNotNull();
        assertThat(mugs).isEmpty();
    }

    @Test
    void knowsNothingAboutOrdersThatDoNotExist() {
        assertThat(shippedItems.shippedItem(new OrderId(999), new Sku("BOOK-1"))).isEmpty();
    }
}
