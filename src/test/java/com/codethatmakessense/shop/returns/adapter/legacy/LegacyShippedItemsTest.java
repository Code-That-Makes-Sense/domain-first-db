package com.codethatmakessense.shop.returns.adapter.legacy;

import static org.assertj.core.api.Assertions.assertThat;

import com.codethatmakessense.shop.BootsSpring;

import com.codethatmakessense.shop.order.application.OrderService;
import com.codethatmakessense.shop.order.domain.CustomerEmail;
import com.codethatmakessense.shop.order.domain.OrderLine;
import com.codethatmakessense.shop.shared.Money;
import com.codethatmakessense.shop.shared.Quantity;
import com.codethatmakessense.shop.order.domain.ShipmentLine;
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

@BootsSpring
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
        Sku book = new Sku("BOOK-1");
        OrderId orderId = orderService.place(new CustomerEmail("viktor@example.com"), List.of(
                new OrderLine(book, new Quantity(3), new Money(1_500)),
                new OrderLine(new Sku("MUG-7"), new Quantity(1), new Money(900))), false);
        orderService.pay(orderId, "PAY-42");
        orderService.ship(orderId, "DHL", "TRACK-1", List.of(new ShipmentLine(book, new Quantity(1))));
        orderService.ship(orderId, "DHL", "TRACK-2", List.of(new ShipmentLine(book, new Quantity(2))));

        Optional<ShippedItem> books = shippedItems.shippedItem(orderId, book);
        Optional<ShippedItem> mugs = shippedItems.shippedItem(orderId, new Sku("MUG-7"));

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
