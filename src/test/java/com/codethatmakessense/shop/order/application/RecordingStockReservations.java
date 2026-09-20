package com.codethatmakessense.shop.order.application;

import com.codethatmakessense.shop.order.domain.StockReservations;
import com.codethatmakessense.shop.shared.Quantity;
import com.codethatmakessense.shop.shared.Sku;
import java.util.HashMap;
import java.util.Map;

class RecordingStockReservations implements StockReservations {

    private final Map<Sku, Integer> onHand = new HashMap<>();
    private final Map<Sku, Integer> reserved = new HashMap<>();

    RecordingStockReservations shelve(Sku sku, int quantity) {
        onHand.merge(sku, quantity, Integer::sum);
        return this;
    }

    int available(Sku sku) {
        return onHand.getOrDefault(sku, 0) - reserved.getOrDefault(sku, 0);
    }

    @Override
    public void reserve(Sku sku, Quantity quantity) {
        if (available(sku) < quantity.value()) {
            throw new IllegalStateException("Not enough stock for " + sku.value());
        }
        reserved.merge(sku, quantity.value(), Integer::sum);
    }

    @Override
    public void release(Sku sku, Quantity quantity) {
        reserved.merge(sku, -quantity.value(), Integer::sum);
    }

    @Override
    public void consume(Sku sku, Quantity quantity) {
        reserved.merge(sku, -quantity.value(), Integer::sum);
        onHand.merge(sku, -quantity.value(), Integer::sum);
    }
}
