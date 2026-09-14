package com.codethatmakessense.shop.order;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
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
    private final JdbcClient jdbc;

    public OrderController(OrderService orderService, JdbcClient jdbc) {
        this.orderService = orderService;
        this.jdbc = jdbc;
    }

    public record PlaceOrderBody(String customerEmail, List<LineRequest> lines, boolean giftWrap, String giftMessage) {
    }

    public record PaymentBody(String reference) {
    }

    public record ShipmentBody(String carrier, String trackingNumber, List<ShipmentRequest> lines) {
    }

    public record CancellationBody(String reason) {
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> place(@RequestBody PlaceOrderBody body) {
        var placed = orderService.place(body.customerEmail(), body.lines(), body.giftWrap(), body.giftMessage());
        return Map.of("id", placed.getId());
    }

    @PostMapping("/{id}/payment")
    public Map<String, Object> pay(@PathVariable Long id, @RequestBody PaymentBody body) {
        orderService.pay(id, body.reference());
        return get(id);
    }

    @PostMapping("/{id}/shipments")
    public Map<String, Object> ship(@PathVariable Long id, @RequestBody ShipmentBody body) {
        if (body.lines() == null || body.lines().isEmpty()) {
            orderService.ship(id, body.carrier(), body.trackingNumber());
        } else {
            orderService.ship(id, body.carrier(), body.trackingNumber(), body.lines());
        }
        return get(id);
    }

    @PostMapping("/{id}/cancellation")
    public Map<String, Object> cancel(@PathVariable Long id, @RequestBody CancellationBody body) {
        orderService.cancel(id, body.reason());
        return get(id);
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable Long id) {
        Map<String, Object> order = jdbc.sql("SELECT id, customer_email, status, total_cents FROM orders WHERE id = ?")
                .param(id).query().listOfRows().stream().findFirst().orElseThrow();
        List<Map<String, Object>> lines = jdbc.sql(
                "SELECT sku, quantity, unit_price_cents FROM order_lines WHERE order_id = ? ORDER BY id")
                .param(id).query().listOfRows();
        return Map.of(
                "id", order.get("ID"),
                "customerEmail", order.get("CUSTOMER_EMAIL"),
                "status", order.get("STATUS"),
                "totalCents", order.get("TOTAL_CENTS"),
                "lines", lines);
    }
}
