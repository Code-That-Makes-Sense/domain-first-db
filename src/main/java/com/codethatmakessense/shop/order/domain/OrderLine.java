package com.codethatmakessense.shop.order.domain;

import com.codethatmakessense.shop.shared.Money;
import com.codethatmakessense.shop.shared.Quantity;
import com.codethatmakessense.shop.shared.Sku;

public record OrderLine(Sku sku, Quantity quantity, Money unitPrice) {

    public OrderLine {
        if (quantity.isNone()) {
            throw new IllegalArgumentException("Quantity must be positive: " + sku.value());
        }
    }

    public Money subtotal() {
        return unitPrice.times(quantity);
    }
}
