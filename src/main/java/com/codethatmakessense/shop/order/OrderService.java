package com.codethatmakessense.shop.order;

import com.codethatmakessense.shop.order.domain.OrderStatus;
import com.codethatmakessense.shop.order.adapter.jpa.OrderLineRow;
import com.codethatmakessense.shop.order.adapter.jpa.OrderRow;
import com.codethatmakessense.shop.order.adapter.jpa.OrderRowRepository;
import com.codethatmakessense.shop.order.adapter.jpa.ShipmentLineRow;
import com.codethatmakessense.shop.order.adapter.jpa.ShipmentRow;
import com.codethatmakessense.shop.order.domain.CustomerEmail;
import com.codethatmakessense.shop.order.domain.Order;
import com.codethatmakessense.shop.order.domain.OrderLine;
import com.codethatmakessense.shop.order.domain.OrderRepository;
import com.codethatmakessense.shop.order.domain.StockReservations;
import com.codethatmakessense.shop.shared.Money;
import com.codethatmakessense.shop.shared.OrderId;
import com.codethatmakessense.shop.shared.Quantity;
import com.codethatmakessense.shop.shared.Sku;
import com.codethatmakessense.shop.stock.StockService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRowRepository orders;

    private final StockService stock;

    private final OrderRepository orderRepository;

    private final StockReservations stockReservations;

    private final Clock clock;

    public OrderService(OrderRowRepository orders, StockService stock, OrderRepository orderRepository,
            StockReservations stockReservations, Clock clock) {
        this.orders = orders;
        this.stock = stock;
        this.orderRepository = orderRepository;
        this.stockReservations = stockReservations;
        this.clock = clock;
    }

    @Transactional
    public Long place(String customerEmail, List<LineRequest> lines) {
        return place(customerEmail, lines, false, null);
    }

    @Transactional
    public Long place(String customerEmail, List<LineRequest> requests, boolean giftWrap, String giftMessage) {
        List<OrderLine> lines = new ArrayList<>();
        for (LineRequest request : requests) {
            lines.add(new OrderLine(new Sku(request.sku()), new Quantity(request.quantity()),
                    new Money(request.unitPriceCents())));
        }
        Order order = Order.place(orderRepository.nextId(), new CustomerEmail(customerEmail), lines, giftWrap,
                LocalDate.now(clock));
        for (OrderLine line : order.lines()) {
            stockReservations.reserve(line.sku(), line.quantity());
        }
        orderRepository.save(order);
        return order.id().value();
    }

    @Transactional
    public void pay(Long orderId, String paymentReference) {
        Order order = load(orderId);
        order.pay(paymentReference, LocalDate.now(clock));
        orderRepository.save(order);
    }

    @Transactional
    public OrderRow ship(Long orderId, String carrier, String trackingNumber) {
        OrderRow order = orders.findById(orderId).orElseThrow();
        List<ShipmentRequest> everything = new ArrayList<>();
        for (OrderLineRow line : order.getLines()) {
            everything.add(new ShipmentRequest(line.getSku(), line.getQuantity()));
        }
        return ship(orderId, carrier, trackingNumber, everything);
    }

    @Transactional
    public OrderRow ship(Long orderId, String carrier, String trackingNumber, List<ShipmentRequest> requests) {
        OrderRow order = orders.findById(orderId).orElseThrow();
        if (!isShippable(order)) {
            throw new IllegalStateException("Only a paid order with something left to ship can be shipped");
        }
        ShipmentRow shipment = new ShipmentRow();
        shipment.setOrder(order);
        shipment.setShippedOn(LocalDate.now());
        shipment.setCarrier(carrier);
        shipment.setTrackingNumber(trackingNumber);
        for (ShipmentRequest request : requests) {
            int remaining = remainingToShip(order, request.sku());
            if (request.quantity() > remaining) {
                throw new IllegalStateException("Cannot ship more than ordered: " + request.sku());
            }
            ShipmentLineRow line = new ShipmentLineRow();
            line.setShipment(shipment);
            line.setSku(request.sku());
            line.setQuantity(request.quantity());
            shipment.getLines().add(line);
            stock.consume(request.sku(), request.quantity());
        }
        order.getShipments().add(shipment);
        if (order.getShippedOn() == null) {
            order.setShippedOn(shipment.getShippedOn());
            order.setCarrier(carrier);
            order.setTrackingNumber(trackingNumber);
        }
        if (isFullyShipped(order)) {
            order.setStatus(OrderStatus.SHIPPED);
        } else {
            order.setStatus(OrderStatus.PARTIALLY_SHIPPED);
        }
        return order;
    }

    @Transactional
    public OrderRow cancel(Long orderId, String reason) {
        OrderRow order = orders.findById(orderId).orElseThrow();
        if (order.getStatus() == OrderStatus.SHIPPED) {
            throw new IllegalStateException("Cannot cancel a shipped order");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("The order is already cancelled");
        }
        for (OrderLineRow line : order.getLines()) {
            stock.release(line.getSku(), line.getQuantity());
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledOn(LocalDate.now());
        order.setCancellationReason(reason);
        return order;
    }

    private Order load(Long orderId) {
        return orderRepository.findById(new OrderId(orderId)).orElseThrow();
    }

    public boolean isShippable(OrderRow order) {
        boolean paidOrPartial = order.getStatus() == OrderStatus.PAID
                || order.getStatus() == OrderStatus.PARTIALLY_SHIPPED;
        return paidOrPartial && !isFullyShipped(order);
    }

    public boolean isFullyShipped(OrderRow order) {
        for (OrderLineRow line : order.getLines()) {
            if (remainingToShip(order, line.getSku()) > 0) {
                return false;
            }
        }
        return true;
    }

    private int remainingToShip(OrderRow order, String sku) {
        int ordered = 0;
        for (OrderLineRow line : order.getLines()) {
            if (line.getSku().equals(sku)) {
                ordered += line.getQuantity();
            }
        }
        int shipped = 0;
        for (ShipmentRow shipment : order.getShipments()) {
            for (ShipmentLineRow line : shipment.getLines()) {
                if (line.getSku().equals(sku)) {
                    shipped += line.getQuantity();
                }
            }
        }
        return ordered - shipped;
    }
}
