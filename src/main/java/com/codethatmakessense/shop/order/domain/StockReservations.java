package com.codethatmakessense.shop.order.domain;

import com.codethatmakessense.shop.shared.Quantity;
import com.codethatmakessense.shop.shared.Sku;

public interface StockReservations {

    void reserve(Sku sku, Quantity quantity);

    void release(Sku sku, Quantity quantity);

    void consume(Sku sku, Quantity quantity);
}
