package com.codethatmakessense.shop.order;

import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orders;

    public OrderService(OrderRepository orders) {
        this.orders = orders;
    }

    @Transactional
    public Order place(String customerEmail, List<LineRequest> lines) {
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("An order needs at least one line");
        }
        Order order = new Order();
        order.setCustomerEmail(customerEmail);
        order.setStatus(OrderStatus.PLACED);
        order.setPlacedOn(LocalDate.now());
        long total = 0;
        for (LineRequest request : lines) {
            if (request.quantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be positive: " + request.sku());
            }
            OrderLine line = new OrderLine();
            line.setOrder(order);
            line.setSku(request.sku());
            line.setQuantity(request.quantity());
            line.setUnitPriceCents(request.unitPriceCents());
            order.getLines().add(line);
            total += request.quantity() * request.unitPriceCents();
        }
        order.setTotalCents(total);
        return orders.save(order);
    }

    @Transactional
    public Order pay(Long orderId, String paymentReference) {
        Order order = orders.findById(orderId).orElseThrow();
        if (order.getStatus() != OrderStatus.PLACED) {
            throw new IllegalStateException("Only a placed order can be paid");
        }
        order.setStatus(OrderStatus.PAID);
        order.setPaidOn(LocalDate.now());
        order.setPaymentReference(paymentReference);
        return order;
    }

    @Transactional
    public Order ship(Long orderId, String carrier, String trackingNumber) {
        Order order = orders.findById(orderId).orElseThrow();
        if (!isShippable(order)) {
            throw new IllegalStateException("Only a paid, unshipped order can be shipped");
        }
        order.setStatus(OrderStatus.SHIPPED);
        order.setShippedOn(LocalDate.now());
        order.setCarrier(carrier);
        order.setTrackingNumber(trackingNumber);
        return order;
    }

    @Transactional
    public Order cancel(Long orderId, String reason) {
        Order order = orders.findById(orderId).orElseThrow();
        if (order.getStatus() == OrderStatus.SHIPPED) {
            throw new IllegalStateException("Cannot cancel a shipped order");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("The order is already cancelled");
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledOn(LocalDate.now());
        order.setCancellationReason(reason);
        return order;
    }

    public boolean isShippable(Order order) {
        return order.getStatus() == OrderStatus.PAID && order.getShippedOn() == null;
    }
}
