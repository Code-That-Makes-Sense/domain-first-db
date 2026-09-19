package com.codethatmakessense.shop.returns.domain;

import com.codethatmakessense.shop.shared.OrderId;
import com.codethatmakessense.shop.shared.Sku;
import java.util.Optional;

public interface ShippedItems {

    Optional<ShippedItem> shippedItem(OrderId orderId, Sku sku);
}
