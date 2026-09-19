package com.codethatmakessense.shop.order.domain;

import com.codethatmakessense.shop.shared.Quantity;
import com.codethatmakessense.shop.shared.Sku;

public record ShipmentLine(Sku sku, Quantity quantity) {
}
