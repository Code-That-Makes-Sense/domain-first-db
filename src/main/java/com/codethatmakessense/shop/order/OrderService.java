package com.codethatmakessense.shop.order;

import com.codethatmakessense.shop.order.adapter.jpa.OrderLineRow;
import com.codethatmakessense.shop.order.adapter.jpa.OrderRow;
import com.codethatmakessense.shop.order.adapter.jpa.OrderRowRepository;
import com.codethatmakessense.shop.order.adapter.jpa.ShipmentLineRow;
import com.codethatmakessense.shop.order.adapter.jpa.ShipmentRow;
import com.codethatmakessense.shop.stock.StockService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRowRepository orders;

    private final StockService stock;

    public OrderService(OrderRowRepository orders, StockService stock) {
        this.orders = orders;
        this.stock = stock;
    }

    @Transactional
    public OrderRow place(String customerEmail, List<LineRequest> lines) {
        return place(customerEmail, lines, false, null);
    }

    @Transactional
    public OrderRow place(String customerEmail, List<LineRequest> lines, boolean giftWrap, String giftMessage) {
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("An order needs at least one line");
        }
        OrderRow order = new OrderRow();
        order.setCustomerEmail(customerEmail);
        order.setStatus(OrderStatus.PLACED);
        order.setPlacedOn(LocalDate.now());
        order.setGiftWrap(giftWrap);
        order.setGiftMessage(giftMessage);
        long total = 0;
        for (LineRequest request : lines) {
            if (request.quantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be positive: " + request.sku());
            }
            stock.reserve(request.sku(), request.quantity());
            OrderLineRow line = new OrderLineRow();
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
    public OrderRow pay(Long orderId, String paymentReference) {
        OrderRow order = orders.findById(orderId).orElseThrow();
        if (order.getStatus() != OrderStatus.PLACED) {
            throw new IllegalStateException("Only a placed order can be paid");
        }
        order.setStatus(OrderStatus.PAID);
        order.setPaidOn(LocalDate.now());
        order.setPaymentReference(paymentReference);
        return order;
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
