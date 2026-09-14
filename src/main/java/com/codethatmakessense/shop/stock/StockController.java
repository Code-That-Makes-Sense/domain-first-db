package com.codethatmakessense.shop.stock;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stock")
public class StockController {

    private final StockService stock;

    public StockController(StockService stock) {
        this.stock = stock;
    }

    public record ReceiptBody(int quantity) {
    }

    @PostMapping("/{sku}/receipts")
    public Map<String, Object> receive(@PathVariable String sku, @RequestBody ReceiptBody body) {
        stock.receive(sku, body.quantity());
        return available(sku);
    }

    @GetMapping("/{sku}")
    public Map<String, Object> available(@PathVariable String sku) {
        return Map.of("sku", sku, "available", stock.available(sku));
    }
}
