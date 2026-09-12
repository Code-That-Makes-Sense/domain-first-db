package com.codethatmakessense.shop.order;

public record LineRequest(String sku, int quantity, long unitPriceCents) {
}
