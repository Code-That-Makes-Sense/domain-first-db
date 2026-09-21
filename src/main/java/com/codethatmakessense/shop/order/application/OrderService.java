package com.codethatmakessense.shop.order.application;

import com.codethatmakessense.shop.order.domain.CustomerEmail;
import com.codethatmakessense.shop.order.domain.Order;
import com.codethatmakessense.shop.order.domain.OrderLine;
import com.codethatmakessense.shop.order.domain.OrderRepository;
import com.codethatmakessense.shop.order.domain.Shipment;
import com.codethatmakessense.shop.order.domain.ShipmentLine;
import com.codethatmakessense.shop.order.domain.StockReservations;
import com.codethatmakessense.shop.shared.OrderId;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class OrderService {

    private final OrderRepository orders;
    private final StockReservations stock;
    private final Clock clock;

    public OrderService(OrderRepository orders, StockReservations stock, Clock clock) {
        this.orders = orders;
        this.stock = stock;
        this.clock = clock;
    }

    @Transactional
    public OrderId place(CustomerEmail customer, List<OrderLine> lines, boolean giftWrap) {
        Order order = Order.place(orders.nextId(), customer, lines, giftWrap, LocalDate.now(clock));
        for (OrderLine line : order.lines()) {
            stock.reserve(line.sku(), line.quantity());
        }
        orders.save(order);
        return order.id();
    }

    @Transactional
    public void pay(OrderId id, String paymentReference) {
        Order order = load(id);
        order.pay(paymentReference, LocalDate.now(clock));
        orders.save(order);
    }

    @Transactional
    public void ship(OrderId id, String carrier, String trackingNumber) {
        Order order = load(id);
        List<ShipmentLine> everything = new ArrayList<>();
        for (OrderLine line : order.lines()) {
            everything.add(new ShipmentLine(line.sku(), line.quantity()));
        }
        ship(id, carrier, trackingNumber, everything);
    }

    @Transactional
    public void ship(OrderId id, String carrier, String trackingNumber, List<ShipmentLine> lines) {
        Order order = load(id);
        Shipment shipment = order.ship(lines, carrier, trackingNumber, LocalDate.now(clock));
        for (ShipmentLine line : shipment.lines()) {
            stock.consume(line.sku(), line.quantity());
        }
        orders.save(order);
    }

    @Transactional
    public void cancel(OrderId id, String reason) {
        Order order = load(id);
        order.cancel(reason, LocalDate.now(clock));
        for (OrderLine line : order.lines()) {
            stock.release(line.sku(), line.quantity());
        }
        orders.save(order);
    }

    private Order load(OrderId id) {
        return orders.findById(id).orElseThrow();
    }
}
