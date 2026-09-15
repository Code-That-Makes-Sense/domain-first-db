package com.codethatmakessense.shop.returns.domain;

import java.util.Optional;

public interface ShippedItems {

    Optional<ShippedItem> shippedItem(OrderId orderId, Sku sku);
}
