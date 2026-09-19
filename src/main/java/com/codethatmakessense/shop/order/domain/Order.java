package com.codethatmakessense.shop.order.domain;

import com.codethatmakessense.shop.shared.Money;
import com.codethatmakessense.shop.shared.OrderId;
import com.codethatmakessense.shop.shared.Quantity;
import com.codethatmakessense.shop.shared.Sku;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Order {

    private final OrderId id;
    private final CustomerEmail customer;
    private final List<OrderLine> lines;
    private final LocalDate placedOn;
    private final boolean giftWrap;
    private final List<Shipment> shipments = new ArrayList<>();
    private OrderStatus status;
    private Payment payment;
    private Cancellation cancellation;

    private Order(OrderId id, CustomerEmail customer, List<OrderLine> lines, LocalDate placedOn,
            boolean giftWrap, OrderStatus status) {
        this.id = id;
        this.customer = customer;
        this.lines = List.copyOf(lines);
        this.placedOn = placedOn;
        this.giftWrap = giftWrap;
        this.status = status;
    }

    public static Order place(OrderId id, CustomerEmail customer, List<OrderLine> lines, boolean giftWrap,
            LocalDate placedOn) {
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("An order needs at least one line");
        }
        return new Order(id, customer, lines, placedOn, giftWrap, OrderStatus.PLACED);
    }

    public static Order reconstitute(OrderId id, CustomerEmail customer, List<OrderLine> lines, LocalDate placedOn,
            boolean giftWrap, OrderStatus status, Payment payment, List<Shipment> shipments,
            Cancellation cancellation) {
        Order order = new Order(id, customer, lines, placedOn, giftWrap, status);
        order.payment = payment;
        order.shipments.addAll(shipments);
        order.cancellation = cancellation;
        return order;
    }

    public void pay(String reference, LocalDate on) {
        if (status != OrderStatus.PLACED) {
            throw new IllegalStateException("Only a placed order can be paid");
        }
        status = OrderStatus.PAID;
        payment = new Payment(reference, on);
    }

    public Shipment ship(List<ShipmentLine> requested, String carrier, String trackingNumber, LocalDate on) {
        if (!isShippable()) {
            throw new IllegalStateException("Only a paid order with something left to ship can be shipped");
        }
        for (ShipmentLine line : requested) {
            if (line.quantity().exceeds(remainingToShip(line.sku()))) {
                throw new IllegalStateException("Cannot ship more than ordered: " + line.sku().value());
            }
        }
        Shipment shipment = new Shipment(on, carrier, trackingNumber, requested);
        shipments.add(shipment);
        if (isFullyShipped()) {
            status = OrderStatus.SHIPPED;
        } else {
            status = OrderStatus.PARTIALLY_SHIPPED;
        }
        return shipment;
    }

    public void cancel(String reason, LocalDate on) {
        if (status == OrderStatus.SHIPPED) {
            throw new IllegalStateException("Cannot cancel a shipped order");
        }
        if (status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("The order is already cancelled");
        }
        status = OrderStatus.CANCELLED;
        cancellation = new Cancellation(reason, on);
    }

    public boolean isShippable() {
        boolean paidOrPartial = status == OrderStatus.PAID || status == OrderStatus.PARTIALLY_SHIPPED;
        return paidOrPartial && !isFullyShipped();
    }

    public boolean isFullyShipped() {
        for (OrderLine line : lines) {
            if (!remainingToShip(line.sku()).isNone()) {
                return false;
            }
        }
        return true;
    }

    public Quantity remainingToShip(Sku sku) {
        Quantity ordered = Quantity.NONE;
        for (OrderLine line : lines) {
            if (line.sku().equals(sku)) {
                ordered = ordered.plus(line.quantity());
            }
        }
        Quantity shipped = Quantity.NONE;
        for (Shipment shipment : shipments) {
            shipped = shipped.plus(shipment.quantityOf(sku));
        }
        return ordered.minus(shipped);
    }

    public Money total() {
        Money total = new Money(0);
        for (OrderLine line : lines) {
            total = new Money(total.cents() + line.subtotal().cents());
        }
        return total;
    }

    public OrderId id() {
        return id;
    }

    public CustomerEmail customer() {
        return customer;
    }

    public List<OrderLine> lines() {
        return lines;
    }

    public LocalDate placedOn() {
        return placedOn;
    }

    public boolean giftWrap() {
        return giftWrap;
    }

    public OrderStatus status() {
        return status;
    }

    public Optional<Payment> payment() {
        return Optional.ofNullable(payment);
    }

    public List<Shipment> shipments() {
        return Collections.unmodifiableList(shipments);
    }

    public Optional<Cancellation> cancellation() {
        return Optional.ofNullable(cancellation);
    }
}
