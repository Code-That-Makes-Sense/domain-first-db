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
import com.codethatmakessense.shop.order.domain.Shipment;
import com.codethatmakessense.shop.order.domain.ShipmentLine;
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
    public void ship(Long orderId, String carrier, String trackingNumber) {
        Order order = load(orderId);
        List<ShipmentRequest> everything = new ArrayList<>();
        for (OrderLine line : order.lines()) {
            everything.add(new ShipmentRequest(line.sku().value(), line.quantity().value()));
        }
        ship(orderId, carrier, trackingNumber, everything);
    }

    @Transactional
    public void ship(Long orderId, String carrier, String trackingNumber, List<ShipmentRequest> requests) {
        Order order = load(orderId);
        List<ShipmentLine> lines = new ArrayList<>();
        for (ShipmentRequest request : requests) {
            lines.add(new ShipmentLine(new Sku(request.sku()), new Quantity(request.quantity())));
        }
        Shipment shipment = order.ship(lines, carrier, trackingNumber, LocalDate.now(clock));
        for (ShipmentLine line : shipment.lines()) {
            stockReservations.consume(line.sku(), line.quantity());
        }
        orderRepository.save(order);
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
}
