package com.codethatmakessense.shop.returns.domain;

import com.codethatmakessense.shop.shared.Money;
import com.codethatmakessense.shop.shared.OrderId;
import com.codethatmakessense.shop.shared.Quantity;
import com.codethatmakessense.shop.shared.Sku;
import java.time.LocalDate;

public record ShippedItem(OrderId orderId, Sku sku, Quantity quantity, Money unitPrice, LocalDate shippedOn) {
}
