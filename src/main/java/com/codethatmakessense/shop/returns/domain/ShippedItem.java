package com.codethatmakessense.shop.returns.domain;

import java.time.LocalDate;

public record ShippedItem(OrderId orderId, Sku sku, Quantity quantity, Money unitPrice, LocalDate shippedOn) {
}
