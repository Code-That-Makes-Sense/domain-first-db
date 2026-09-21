package com.codethatmakessense.shop.order;

import com.codethatmakessense.shop.order.application.OrderService;
import com.codethatmakessense.shop.order.domain.CustomerEmail;
import com.codethatmakessense.shop.order.domain.Order;
import com.codethatmakessense.shop.order.domain.OrderLine;
import com.codethatmakessense.shop.order.domain.OrderRepository;
import com.codethatmakessense.shop.order.domain.ShipmentLine;
import com.codethatmakessense.shop.shared.Money;
import com.codethatmakessense.shop.shared.OrderId;
import com.codethatmakessense.shop.shared.Quantity;
import com.codethatmakessense.shop.shared.Sku;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orders;

    public OrderController(OrderService orderService, OrderRepository orders) {
        this.orderService = orderService;
        this.orders = orders;
    }

    public record LineBody(String sku, int quantity, long unitPriceCents) {
    }

    public record PlaceOrderBody(String customerEmail, List<LineBody> lines, boolean giftWrap, String giftMessage) {
    }

    public record PaymentBody(String reference) {
    }

    public record ShipmentLineBody(String sku, int quantity) {
    }

    public record ShipmentBody(String carrier, String trackingNumber, List<ShipmentLineBody> lines) {
    }

    public record CancellationBody(String reason) {
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> place(@RequestBody PlaceOrderBody body) {
        List<OrderLine> lines = body.lines().stream()
                .map(line -> new OrderLine(new Sku(line.sku()), new Quantity(line.quantity()), new Money(line.unitPriceCents())))
                .toList();
        OrderId id = orderService.place(new CustomerEmail(body.customerEmail()), lines, body.giftWrap());
        return Map.of("id", id.value());
    }

    @PostMapping("/{id}/payment")
    public Map<String, Object> pay(@PathVariable long id, @RequestBody PaymentBody body) {
        orderService.pay(new OrderId(id), body.reference());
        return get(id);
    }

    @PostMapping("/{id}/shipments")
    public Map<String, Object> ship(@PathVariable long id, @RequestBody ShipmentBody body) {
        if (body.lines() == null || body.lines().isEmpty()) {
            orderService.ship(new OrderId(id), body.carrier(), body.trackingNumber());
        } else {
            List<ShipmentLine> lines = body.lines().stream()
                    .map(line -> new ShipmentLine(new Sku(line.sku()), new Quantity(line.quantity())))
                    .toList();
            orderService.ship(new OrderId(id), body.carrier(), body.trackingNumber(), lines);
        }
        return get(id);
    }

    @PostMapping("/{id}/cancellation")
    public Map<String, Object> cancel(@PathVariable long id, @RequestBody CancellationBody body) {
        orderService.cancel(new OrderId(id), body.reason());
        return get(id);
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable long id) {
        Order order = orders.findById(new OrderId(id)).orElseThrow();
        return Map.of(
                "id", order.id().value(),
                "customerEmail", order.customer().value(),
                "status", order.status().name(),
                "totalCents", order.total().cents(),
                "lines", order.lines().stream().map(line -> Map.of(
                        "sku", line.sku().value(), "quantity", line.quantity().value(),
                        "unitPriceCents", line.unitPrice().cents())).toList(),
                "shipments", order.shipments().size());
    }
}
