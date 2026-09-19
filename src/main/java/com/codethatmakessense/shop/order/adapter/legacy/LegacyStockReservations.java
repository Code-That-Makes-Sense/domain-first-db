package com.codethatmakessense.shop.order.adapter.legacy;

import com.codethatmakessense.shop.order.domain.StockReservations;
import com.codethatmakessense.shop.shared.Quantity;
import com.codethatmakessense.shop.shared.Sku;
import com.codethatmakessense.shop.stock.StockService;
import org.springframework.stereotype.Component;

@Component
public class LegacyStockReservations implements StockReservations {

    private final StockService stock;

    public LegacyStockReservations(StockService stock) {
        this.stock = stock;
    }

    @Override
    public void reserve(Sku sku, Quantity quantity) {
        stock.reserve(sku.value(), quantity.value());
    }

    @Override
    public void release(Sku sku, Quantity quantity) {
        stock.release(sku.value(), quantity.value());
    }

    @Override
    public void consume(Sku sku, Quantity quantity) {
        stock.consume(sku.value(), quantity.value());
    }
}
