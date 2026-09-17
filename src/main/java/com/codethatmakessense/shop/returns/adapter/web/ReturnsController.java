package com.codethatmakessense.shop.returns.adapter.web;

import com.codethatmakessense.shop.returns.application.ReturnService;
import com.codethatmakessense.shop.returns.domain.OrderId;
import com.codethatmakessense.shop.returns.domain.Quantity;
import com.codethatmakessense.shop.returns.domain.ReturnId;
import com.codethatmakessense.shop.returns.domain.ReturnRequest;
import com.codethatmakessense.shop.returns.domain.ReturnRequestRepository;
import com.codethatmakessense.shop.returns.domain.Sku;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/returns")
public class ReturnsController {

    private final ReturnService returns;
    private final ReturnRequestRepository requests;

    public ReturnsController(ReturnService returns, ReturnRequestRepository requests) {
        this.returns = returns;
        this.requests = requests;
    }

    public record RequestBodyDto(long orderId, String sku, int quantity) {
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> request(@RequestBody RequestBodyDto body) {
        ReturnId id = returns.request(new OrderId(body.orderId()), new Sku(body.sku()), new Quantity(body.quantity()));
        return get(id.value());
    }

    @PostMapping("/{id}/approval")
    public Map<String, Object> approve(@PathVariable UUID id) {
        returns.approve(new ReturnId(id));
        return get(id);
    }

    @PostMapping("/{id}/rejection")
    public Map<String, Object> reject(@PathVariable UUID id) {
        returns.reject(new ReturnId(id));
        return get(id);
    }

    @PostMapping("/{id}/receipt")
    public Map<String, Object> receive(@PathVariable UUID id) {
        returns.receive(new ReturnId(id));
        return get(id);
    }

    @PostMapping("/{id}/refund")
    public Map<String, Object> refund(@PathVariable UUID id) {
        returns.refund(new ReturnId(id));
        return get(id);
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable UUID id) {
        ReturnRequest request = requests.findById(new ReturnId(id)).orElseThrow();
        return Map.of(
                "id", request.id().value(),
                "orderId", request.orderId().value(),
                "sku", request.sku().value(),
                "quantity", request.quantity().value(),
                "status", request.status().name(),
                "refundCents", request.refundAmount().cents());
    }
}
