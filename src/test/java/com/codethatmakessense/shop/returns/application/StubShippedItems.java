package com.codethatmakessense.shop.returns.application;

import com.codethatmakessense.shop.returns.domain.OrderId;
import com.codethatmakessense.shop.returns.domain.ShippedItem;
import com.codethatmakessense.shop.returns.domain.ShippedItems;
import com.codethatmakessense.shop.returns.domain.Sku;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class StubShippedItems implements ShippedItems {

    private final List<ShippedItem> items = new ArrayList<>();

    StubShippedItems shipped(ShippedItem item) {
        items.add(item);
        return this;
    }

    @Override
    public Optional<ShippedItem> shippedItem(OrderId orderId, Sku sku) {
        for (ShippedItem item : items) {
            if (item.orderId().equals(orderId) && item.sku().equals(sku)) {
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }
}
